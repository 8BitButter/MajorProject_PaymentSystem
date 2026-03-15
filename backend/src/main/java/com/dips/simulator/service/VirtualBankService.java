package com.dips.simulator.service;

import com.dips.simulator.domain.AccountEntity;
import com.dips.simulator.domain.LedgerEntryEntity;
import com.dips.simulator.domain.ProcessedOperationEntity;
import com.dips.simulator.domain.enums.BankOperationType;
import com.dips.simulator.domain.enums.BankOperationStatus;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.LedgerEntryType;
import com.dips.simulator.domain.enums.ProcessedOperationResult;
import com.dips.simulator.repository.AccountRepository;
import com.dips.simulator.repository.LedgerEntryRepository;
import com.dips.simulator.repository.ProcessedOperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class VirtualBankService {

    private static final Logger log = LoggerFactory.getLogger(VirtualBankService.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final ProcessedOperationRepository processedOperationRepository;
    private final FailureInjectionService failureInjectionService;

    public VirtualBankService(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerRepository,
            ProcessedOperationRepository processedOperationRepository,
            FailureInjectionService failureInjectionService
    ) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.processedOperationRepository = processedOperationRepository;
        this.failureInjectionService = failureInjectionService;
    }

    @Transactional
    public boolean debit(UUID transactionId, String payerVpa, BigDecimal amount) {
        log.info("event=bank_debit_attempt account={} amount={}", payerVpa, amount);
        Boolean replayResult = processedOutcome(transactionId, BankOperationType.DEBIT).orElse(null);
        if (replayResult != null) {
            log.info("event=bank_debit_idempotent_replay result={}", replayResult);
            return replayResult;
        }
        if (failureInjectionService.isEnabled(FailureScenario.DEBIT_TIMEOUT)) {
            throw new OperationTimeoutException("Debit timeout injected");
        }
        AccountEntity payer = accountRepository.findByVpa(payerVpa)
                .orElseThrow(() -> new DomainException("Payer account not found: " + payerVpa));
        if (payer.getBalance().compareTo(amount) < 0) {
            log.info("event=bank_debit_failed reason=insufficient_funds account={} balance={} amount={}",
                    payerVpa, payer.getBalance(), amount);
            markProcessed(transactionId, BankOperationType.DEBIT, ProcessedOperationResult.FAILED);
            return false;
        }
        payer.setBalance(payer.getBalance().subtract(amount));
        accountRepository.save(payer);
        writeLedger(transactionId, payerVpa, LedgerEntryType.DEBIT, amount);
        markProcessed(transactionId, BankOperationType.DEBIT, ProcessedOperationResult.SUCCESS);
        if (failureInjectionService.isEnabled(FailureScenario.NETWORK_TIMEOUT)) {
            throw new OperationTimeoutException("Network timeout after debit apply");
        }
        log.info("event=bank_debit_success account={} newBalance={}", payerVpa, payer.getBalance());
        return true;
    }

    @Transactional
    public void credit(UUID transactionId, String payeeVpa, BigDecimal amount) {
        log.info("event=bank_credit_attempt account={} amount={}", payeeVpa, amount);
        Boolean replayResult = processedOutcome(transactionId, BankOperationType.CREDIT).orElse(null);
        if (replayResult != null) {
            if (replayResult) {
                log.info("event=bank_credit_idempotent_replay result=SUCCESS");
                return;
            }
            throw new DomainException("Credit already processed as FAILED");
        }
        if (failureInjectionService.isEnabled(FailureScenario.CREDIT_TIMEOUT)) {
            throw new OperationTimeoutException("Credit timeout injected");
        }
        try {
            AccountEntity payee = accountRepository.findByVpa(payeeVpa)
                    .orElseThrow(() -> new DomainException("Payee account not found: " + payeeVpa));
            payee.setBalance(payee.getBalance().add(amount));
            accountRepository.save(payee);
            writeLedger(transactionId, payeeVpa, LedgerEntryType.CREDIT, amount);
            markProcessed(transactionId, BankOperationType.CREDIT, ProcessedOperationResult.SUCCESS);
            if (failureInjectionService.isEnabled(FailureScenario.NETWORK_TIMEOUT)) {
                throw new OperationTimeoutException("Network timeout after credit apply");
            }
            log.info("event=bank_credit_success account={} newBalance={}", payeeVpa, payee.getBalance());
        } catch (DomainException ex) {
            markProcessed(transactionId, BankOperationType.CREDIT, ProcessedOperationResult.FAILED);
            throw ex;
        }
    }

    @Transactional
    public void reversalCreditToPayer(UUID transactionId, String payerVpa, BigDecimal amount) {
        log.info("event=bank_reversal_attempt account={} amount={}", payerVpa, amount);
        Boolean replayResult = processedOutcome(transactionId, BankOperationType.REVERSAL).orElse(null);
        if (replayResult != null) {
            if (replayResult) {
                log.info("event=bank_reversal_idempotent_replay result=SUCCESS");
                return;
            }
            throw new DomainException("Reversal already processed as FAILED");
        }
        if (failureInjectionService.isEnabled(FailureScenario.REVERSAL_TIMEOUT)) {
            throw new OperationTimeoutException("Reversal timeout injected");
        }
        AccountEntity payer = applyReversal(transactionId, payerVpa, amount);
        markProcessed(transactionId, BankOperationType.REVERSAL, ProcessedOperationResult.SUCCESS);
        if (failureInjectionService.isEnabled(FailureScenario.NETWORK_TIMEOUT)) {
            throw new OperationTimeoutException("Network timeout after reversal apply");
        }
        log.info("event=bank_reversal_success account={} newBalance={}", payerVpa, payer.getBalance());
    }

    @Transactional
    public void reversalCreditToPayerForce(UUID transactionId, String payerVpa, BigDecimal amount) {
        Boolean replayResult = processedOutcome(transactionId, BankOperationType.REVERSAL).orElse(null);
        if (Boolean.TRUE.equals(replayResult)) {
            return;
        }
        log.warn("event=bank_reversal_force account={} amount={}", payerVpa, amount);
        applyReversal(transactionId, payerVpa, amount);
        markProcessed(transactionId, BankOperationType.REVERSAL, ProcessedOperationResult.SUCCESS);
    }

    public boolean isDebitApplied(UUID transactionId) {
        return getOperationStatus(transactionId, BankOperationType.DEBIT) == BankOperationStatus.SUCCESS;
    }

    public boolean isCreditApplied(UUID transactionId) {
        return getOperationStatus(transactionId, BankOperationType.CREDIT) == BankOperationStatus.SUCCESS;
    }

    public boolean isReversalApplied(UUID transactionId) {
        return getOperationStatus(transactionId, BankOperationType.REVERSAL) == BankOperationStatus.SUCCESS;
    }

    public BankOperationStatus getOperationStatus(UUID transactionId, BankOperationType operationType) {
        Optional<ProcessedOperationEntity> existing = processedOperationRepository
                .findByTransactionIdAndOperationType(transactionId, operationType);
        if (existing.isPresent()) {
            return existing.get().getResult() == ProcessedOperationResult.SUCCESS
                    ? BankOperationStatus.SUCCESS
                    : BankOperationStatus.FAILED;
        }
        if (ledgerRepository.existsByTransactionIdAndEntryType(transactionId, ledgerEntryTypeFor(operationType))) {
            return BankOperationStatus.SUCCESS;
        }
        return BankOperationStatus.NOT_FOUND;
    }

    public Optional<OffsetDateTime> getOperationProcessedAt(UUID transactionId, BankOperationType operationType) {
        return processedOperationRepository.findByTransactionIdAndOperationType(transactionId, operationType)
                .map(ProcessedOperationEntity::getProcessedAt);
    }

    private AccountEntity applyReversal(UUID transactionId, String payerVpa, BigDecimal amount) {
        AccountEntity payer = accountRepository.findByVpa(payerVpa)
                .orElseThrow(() -> new DomainException("Payer account not found for reversal: " + payerVpa));
        payer.setBalance(payer.getBalance().add(amount));
        accountRepository.save(payer);
        writeLedger(transactionId, payerVpa, LedgerEntryType.REVERSAL_CREDIT, amount);
        return payer;
    }

    private void writeLedger(UUID transactionId, String vpa, LedgerEntryType type, BigDecimal amount) {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setTransactionId(transactionId);
        entry.setVpa(vpa);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setCreatedAt(OffsetDateTime.now());
        ledgerRepository.save(entry);
    }

    private Optional<Boolean> processedOutcome(UUID transactionId, BankOperationType operationType) {
        return processedOperationRepository.findByTransactionIdAndOperationType(transactionId, operationType)
                .map(record -> record.getResult() == ProcessedOperationResult.SUCCESS);
    }

    private LedgerEntryType ledgerEntryTypeFor(BankOperationType operationType) {
        return switch (operationType) {
            case DEBIT -> LedgerEntryType.DEBIT;
            case CREDIT -> LedgerEntryType.CREDIT;
            case REVERSAL -> LedgerEntryType.REVERSAL_CREDIT;
        };
    }

    private void markProcessed(UUID transactionId, BankOperationType operationType, ProcessedOperationResult result) {
        if (processedOperationRepository.findByTransactionIdAndOperationType(transactionId, operationType).isPresent()) {
            return;
        }
        ProcessedOperationEntity record = new ProcessedOperationEntity();
        record.setTransactionId(transactionId);
        record.setOperationType(operationType);
        record.setResult(result);
        record.setProcessedAt(OffsetDateTime.now());
        try {
            processedOperationRepository.save(record);
        } catch (DataIntegrityViolationException ignored) {
            // Another concurrent worker already persisted the same operation idempotency record.
        }
    }
}

