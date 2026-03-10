package com.dips.simulator;

import com.dips.simulator.domain.AccountEntity;
import com.dips.simulator.domain.UserEntity;
import com.dips.simulator.domain.enums.BankType;
import com.dips.simulator.domain.enums.FailureScenario;
import com.dips.simulator.domain.enums.TransactionState;
import com.dips.simulator.dto.OfflineSmsDecryptedPayload;
import com.dips.simulator.dto.OfflineSmsRequest;
import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.repository.AccountRepository;
import com.dips.simulator.repository.UserRepository;
import com.dips.simulator.service.FailureInjectionService;
import com.dips.simulator.service.OfflineSmsService;
import com.dips.simulator.service.PaymentExecutionService;
import com.dips.simulator.service.PaymentService;
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
    private PaymentExecutionService paymentExecutionService;

    @Autowired
    private FailureInjectionService failureInjectionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OfflineSmsService offlineSmsService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.save(account("payer@issuer", BankType.ISSUER, new BigDecimal("5000.00")));
        accountRepository.save(account("payee@acquirer", BankType.ACQUIRER, new BigDecimal("1000.00")));
        userRepository.save(user("Payer One", "payer@issuer", "1111", "ISSUER_BANK"));
        userRepository.save(user("Payee One", "payee@acquirer", "1111", "ACQUIRER_BANK"));
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

    private UserEntity user(String name, String vpa, String mpin, String bankNodeCode) {
        UserEntity user = new UserEntity();
        user.setDisplayName(name);
        user.setVpa(vpa);
        user.setMpin(mpin);
        user.setBankNodeCode(bankNodeCode);
        return user;
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
}
