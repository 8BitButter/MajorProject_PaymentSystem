package com.dips.simulator.service;

import com.dips.simulator.domain.AccountEntity;
import com.dips.simulator.domain.LedgerEntryEntity;
import com.dips.simulator.domain.TransactionEntity;
import com.dips.simulator.domain.enums.LedgerEntryType;
import com.dips.simulator.repository.AccountRepository;
import com.dips.simulator.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class VirtualBankService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerRepository;

    public VirtualBankService(AccountRepository accountRepository, LedgerEntryRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public boolean debit(TransactionEntity tx, String payerVpa, BigDecimal amount) {
        AccountEntity payer = accountRepository.findByVpa(payerVpa)
                .orElseThrow(() -> new DomainException("Payer account not found: " + payerVpa));
        if (payer.getBalance().compareTo(amount) < 0) {
            return false;
        }
        payer.setBalance(payer.getBalance().subtract(amount));
        accountRepository.save(payer);
        writeLedger(tx, payerVpa, LedgerEntryType.DEBIT, amount);
        return true;
    }

    @Transactional
    public void credit(TransactionEntity tx, String payeeVpa, BigDecimal amount) {
        AccountEntity payee = accountRepository.findByVpa(payeeVpa)
                .orElseThrow(() -> new DomainException("Payee account not found: " + payeeVpa));
        payee.setBalance(payee.getBalance().add(amount));
        accountRepository.save(payee);
        writeLedger(tx, payeeVpa, LedgerEntryType.CREDIT, amount);
    }

    @Transactional
    public void reversalCreditToPayer(TransactionEntity tx, String payerVpa, BigDecimal amount) {
        AccountEntity payer = accountRepository.findByVpa(payerVpa)
                .orElseThrow(() -> new DomainException("Payer account not found for reversal: " + payerVpa));
        payer.setBalance(payer.getBalance().add(amount));
        accountRepository.save(payer);
        writeLedger(tx, payerVpa, LedgerEntryType.REVERSAL_CREDIT, amount);
    }

    private void writeLedger(TransactionEntity tx, String vpa, LedgerEntryType type, BigDecimal amount) {
        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setTransactionId(tx.getId());
        entry.setVpa(vpa);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setCreatedAt(OffsetDateTime.now());
        ledgerRepository.save(entry);
    }
}

