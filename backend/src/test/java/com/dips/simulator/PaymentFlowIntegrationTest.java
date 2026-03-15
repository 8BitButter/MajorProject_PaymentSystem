package com.dips.simulator;

import com.dips.simulator.controller.BankController;
import com.dips.simulator.controller.SwitchController;
import com.dips.simulator.domain.AccountEntity;
import com.dips.simulator.domain.enums.ExecutionQueueStatus;
import com.dips.simulator.domain.enums.BankType;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.BankOperationStatusResponse;
import com.dips.simulator.dto.OfflineSmsDecryptedPayload;
import com.dips.simulator.dto.OfflineSmsRequest;
import com.dips.simulator.dto.OperationRequest;
import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.repository.AccountRepository;
import com.dips.simulator.repository.ExecutionQueueRepository;
import com.dips.simulator.service.FailureInjectionService;
import com.dips.simulator.service.OfflineSmsService;
import com.dips.simulator.service.AdminDashboardService;
import com.dips.simulator.service.PaymentExecutionService;
import com.dips.simulator.service.PaymentService;
import com.dips.simulator.service.scheduler.ExecutionQueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentFlowIntegrationTest {

    private static final String KEY_B64 = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private PaymentExecutionService paymentExecutionService;

    @Autowired
    private FailureInjectionService failureInjectionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OfflineSmsService offlineSmsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankController bankController;

    @Autowired
    private SwitchController switchController;

    @Autowired
    private ExecutionQueueService executionQueueService;

    @Autowired
    private ExecutionQueueRepository executionQueueRepository;

    @BeforeEach
    void setup() {
        executionQueueRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(account("payer@issuer", BankType.ISSUER, new BigDecimal("5000.00")));
        accountRepository.save(account("payee@acquirer", BankType.ACQUIRER, new BigDecimal("1000.00")));
        for (FailureScenario scenario : FailureScenario.values()) {
            failureInjectionService.set(scenario, false);
        }
    }

    @Test
    void happyPathCompletesAndBalancesMatch() {
        PushPaymentRequest req = request("req-happy", new BigDecimal("100.00"));
        var created = paymentService.initiateOnlinePush(req);
        paymentExecutionService.execute(created.getTransactionId());

        var tx = paymentService.getTransaction(created.getTransactionId());
        assertThat(tx.getState()).isEqualTo(TransactionState.COMPLETED);
        assertThat(balance("payer@issuer")).isEqualByComparingTo("4900.00");
        assertThat(balance("payee@acquirer")).isEqualByComparingTo("1100.00");
    }

    @Test
    void debitFailureEndsInFailedPreDebitWithoutBalanceChange() {
        failureInjectionService.set(FailureScenario.DEBIT_FAIL, true);
        PushPaymentRequest req = request("req-debit-fail", new BigDecimal("100.00"));
        var created = paymentService.initiateOnlinePush(req);
        paymentExecutionService.execute(created.getTransactionId());

        var tx = paymentService.getTransaction(created.getTransactionId());
        assertThat(tx.getState()).isEqualTo(TransactionState.FAILED_PRE_DEBIT);
        assertThat(balance("payer@issuer")).isEqualByComparingTo("5000.00");
        assertThat(balance("payee@acquirer")).isEqualByComparingTo("1000.00");
    }

    @Test
    void creditFailureTriggersReversalAndConservesMoney() {
        failureInjectionService.set(FailureScenario.CREDIT_FAIL, true);
        PushPaymentRequest req = request("req-credit-fail", new BigDecimal("220.00"));
        var created = paymentService.initiateOnlinePush(req);
        paymentExecutionService.execute(created.getTransactionId());

        var tx = paymentService.getTransaction(created.getTransactionId());
        assertThat(tx.getState()).isEqualTo(TransactionState.REVERSED);
        assertThat(balance("payer@issuer")).isEqualByComparingTo("5000.00");
        assertThat(balance("payee@acquirer")).isEqualByComparingTo("1000.00");
    }

    @Test
    void idempotentRetryReturnsSameTransaction() {
        PushPaymentRequest req = request("req-idem", new BigDecimal("50.00"));
        var first = paymentService.initiateOnlinePush(req);
        var second = paymentService.initiateOnlinePush(req);
        assertThat(second.isIdempotentReplay()).isTrue();
        assertThat(second.getTransactionId()).isEqualTo(first.getTransactionId());
    }

    @Test
    void bankOperationStatusQueryReportsSuccessNotFoundAndReverseAlias() {
        PushPaymentRequest req = request("req-bank-status", new BigDecimal("80.00"));
        var created = paymentService.initiateOnlinePush(req);
        paymentExecutionService.execute(created.getTransactionId());

        BankOperationStatusResponse debit = bankController.transactionStatus(created.getTransactionId(), "DEBIT");
        assertThat(debit.getStatus()).isEqualTo("SUCCESS");
        assertThat(debit.getOperation()).isEqualTo("DEBIT");

        BankOperationStatusResponse reverseAlias = bankController.transactionStatus(created.getTransactionId(), "REVERSE");
        assertThat(reverseAlias.getStatus()).isEqualTo("NOT_FOUND");
        assertThat(reverseAlias.getOperation()).isEqualTo("REVERSAL");

        BankOperationStatusResponse unknown = bankController.transactionStatus(UUID.randomUUID(), "DEBIT");
        assertThat(unknown.getStatus()).isEqualTo("NOT_FOUND");
    }

    @Test
    void switchEndpointsSupportIdempotentDebitAndReverse() {
        UUID txId = UUID.randomUUID();
        OperationRequest debitRequest = operationRequest(txId, "payer@issuer", new BigDecimal("40.00"));

        BankOperationStatusResponse firstDebit = switchController.debit(debitRequest);
        BankOperationStatusResponse replayDebit = switchController.debit(debitRequest);
        assertThat(firstDebit.getStatus()).isEqualTo("SUCCESS");
        assertThat(replayDebit.getStatus()).isEqualTo("SUCCESS");

        OperationRequest reverseRequest = operationRequest(txId, "payer@issuer", new BigDecimal("40.00"));
        BankOperationStatusResponse firstReverse = switchController.reverse(reverseRequest);
        BankOperationStatusResponse replayReverse = switchController.reverse(reverseRequest);
        assertThat(firstReverse.getStatus()).isEqualTo("SUCCESS");
        assertThat(replayReverse.getStatus()).isEqualTo("SUCCESS");

        assertThat(balance("payer@issuer")).isEqualByComparingTo("5000.00");
    }

    @Test
    void switchCreditEndpointReturnsFailureForUnknownAccount() {
        OperationRequest creditRequest = operationRequest(UUID.randomUUID(), "missing@acquirer", new BigDecimal("10.00"));
        BankOperationStatusResponse response = switchController.credit(creditRequest);
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getReasonCode()).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    void executionQueueClaimReleaseAndCompleteLifecycle() {
        PushPaymentRequest req = request("req-queue-lifecycle", new BigDecimal("20.00"));
        var created = paymentService.initiateOnlinePush(req);
        executionQueueService.enqueue(created.getTransactionId(), req.getAmount(), new BigDecimal("100.00"));

        var claimed = executionQueueService.claimNext("worker-lifecycle");
        assertThat(claimed).contains(created.getTransactionId());
        var processingEntry = executionQueueRepository.findById(created.getTransactionId()).orElseThrow();
        assertThat(processingEntry.getQueueStatus()).isEqualTo(ExecutionQueueStatus.PROCESSING);
        assertThat(processingEntry.getLockedBy()).isEqualTo("worker-lifecycle");

        executionQueueService.releasePending(created.getTransactionId());
        var pendingEntry = executionQueueRepository.findById(created.getTransactionId()).orElseThrow();
        assertThat(pendingEntry.getQueueStatus()).isEqualTo(ExecutionQueueStatus.PENDING);
        assertThat(pendingEntry.getLockedBy()).isNull();

        executionQueueService.complete(created.getTransactionId());
        assertThat(executionQueueRepository.findById(created.getTransactionId())).isEmpty();
    }

    @Test
    void executionQueueMovesToDeadLetterAfterMaxRetries() {
        PushPaymentRequest req = request("req-queue-dead-letter", new BigDecimal("30.00"));
        var created = paymentService.initiateOnlinePush(req);
        executionQueueService.enqueue(created.getTransactionId(), req.getAmount(), new BigDecimal("100.00"));

        executionQueueService.requeueAfterFailure(created.getTransactionId(), "FIRST_FAILURE", 2);
        var afterFirstFailure = executionQueueRepository.findById(created.getTransactionId()).orElseThrow();
        assertThat(afterFirstFailure.getAttemptCount()).isEqualTo(1);
        assertThat(afterFirstFailure.getQueueStatus()).isEqualTo(ExecutionQueueStatus.PENDING);
        assertThat(afterFirstFailure.getLastErrorCode()).isEqualTo("FIRST_FAILURE");

        executionQueueService.requeueAfterFailure(created.getTransactionId(), "SECOND_FAILURE", 2);
        var afterSecondFailure = executionQueueRepository.findById(created.getTransactionId()).orElseThrow();
        assertThat(afterSecondFailure.getAttemptCount()).isEqualTo(2);
        assertThat(afterSecondFailure.getQueueStatus()).isEqualTo(ExecutionQueueStatus.DEAD_LETTER);
        assertThat(afterSecondFailure.getLastErrorCode()).isEqualTo("SECOND_FAILURE");
    }

    @Test
    void offlineSmsValidAcceptedAndTamperedRejected() throws Exception {
        OfflineSmsDecryptedPayload payload = new OfflineSmsDecryptedPayload();
        payload.setClientRequestId("offline-req-1");
        payload.setPayerVpa("payer@issuer");
        payload.setPayeeVpa("payee@acquirer");
        payload.setAmount(new BigDecimal("70.00"));
        payload.setMpin("1111");

        String messageId = "msg-" + UUID.randomUUID();
        OfflineSmsRequest valid = encryptPayload(messageId, payload);
        var response = offlineSmsService.handle(valid);
        assertThat(response.isAccepted()).isTrue();
        paymentExecutionService.execute(response.getTransactionId());
        assertThat(paymentService.getTransaction(response.getTransactionId()).getState()).isEqualTo(TransactionState.COMPLETED);

        OfflineSmsRequest tampered = encryptPayload("msg-" + UUID.randomUUID(), payload);
        tampered.setCipherTextBase64(tampered.getCipherTextBase64().substring(0, tampered.getCipherTextBase64().length() - 2) + "AA");
        var bad = offlineSmsService.handle(tampered);
        assertThat(bad.isAccepted()).isFalse();
    }

    @Test
    void adminDashboardReturnsKpisAndUserFilteredRecentTransactions() {
        var baseline = adminDashboardService.dashboard(100, "payer@issuer");

        PushPaymentRequest ok = request("req-dashboard-ok", new BigDecimal("25.00"));
        var tx1 = paymentService.initiateOnlinePush(ok);
        paymentExecutionService.execute(tx1.getTransactionId());

        failureInjectionService.set(FailureScenario.DEBIT_FAIL, true);
        PushPaymentRequest failed = request("req-dashboard-fail", new BigDecimal("10.00"));
        var tx2 = paymentService.initiateOnlinePush(failed);
        paymentExecutionService.execute(tx2.getTransactionId());
        failureInjectionService.set(FailureScenario.DEBIT_FAIL, false);

        var dashboard = adminDashboardService.dashboard(10, "payer@issuer");
        assertThat(dashboard.getTotalTransactions()).isEqualTo(baseline.getTotalTransactions() + 2);
        assertThat(dashboard.getCompletedTransactions()).isEqualTo(baseline.getCompletedTransactions() + 1);
        assertThat(dashboard.getFailedPreDebitTransactions()).isEqualTo(baseline.getFailedPreDebitTransactions() + 1);
        assertThat(dashboard.getRecentTransactions()).isNotEmpty();
        assertThat(dashboard.getRecentTransactions().stream().map(tx -> tx.getClientRequestId()))
                .contains("req-dashboard-ok", "req-dashboard-fail");
    }

    private OfflineSmsRequest encryptPayload(String messageId, OfflineSmsDecryptedPayload payload) throws Exception {
        byte[] key = Base64.getDecoder().decode(KEY_B64);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        cipher.updateAAD(messageId.getBytes(StandardCharsets.UTF_8));
        byte[] plain = objectMapper.writeValueAsBytes(payload);
        byte[] encrypted = cipher.doFinal(plain);

        OfflineSmsRequest req = new OfflineSmsRequest();
        req.setMessageId(messageId);
        req.setIvBase64(Base64.getEncoder().encodeToString(iv));
        req.setCipherTextBase64(Base64.getEncoder().encodeToString(encrypted));
        req.setTimestampEpochSeconds(Instant.now().getEpochSecond());
        return req;
    }

    private AccountEntity account(String vpa, BankType bankType, BigDecimal balance) {
        AccountEntity account = new AccountEntity();
        account.setVpa(vpa);
        account.setBankType(bankType);
        account.setBalance(balance);
        return account;
    }

    private BigDecimal balance(String vpa) {
        return accountRepository.findByVpa(vpa).orElseThrow().getBalance();
    }

    private PushPaymentRequest request(String clientRequestId, BigDecimal amount) {
        PushPaymentRequest request = new PushPaymentRequest();
        request.setClientRequestId(clientRequestId);
        request.setPayerVpa("payer@issuer");
        request.setPayeeVpa("payee@acquirer");
        request.setAmount(amount);
        request.setMpin("1111");
        return request;
    }

    private OperationRequest operationRequest(UUID transactionId, String accountId, BigDecimal amount) {
        return new OperationRequest(transactionId, "corr-" + transactionId, accountId, amount);
    }
}

