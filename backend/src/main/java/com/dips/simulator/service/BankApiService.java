package com.dips.simulator.service;

import com.dips.simulator.domain.enums.BankOperationType;
import com.dips.simulator.dto.BankOperationStatusResponse;
import com.dips.simulator.dto.OperationRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class BankApiService {

    private final VirtualBankService virtualBankService;

    public BankApiService(VirtualBankService virtualBankService) {
        this.virtualBankService = virtualBankService;
    }

    public BankOperationStatusResponse debit(OperationRequest request) {
        try {
            boolean success = virtualBankService.debit(request.getTransactionId(), request.getAccountId(), request.getAmount());
            return response(
                    request,
                    BankOperationType.DEBIT,
                    success ? "SUCCESS" : "FAILED",
                    success ? "NONE" : "INSUFFICIENT_FUNDS",
                    success ? "Debit applied" : "Debit rejected"
            );
        } catch (OperationTimeoutException ex) {
            throw ex;
        } catch (DomainException ex) {
            return response(request, BankOperationType.DEBIT, "FAILED", "ACCOUNT_NOT_FOUND", ex.getMessage());
        }
    }

    public BankOperationStatusResponse credit(OperationRequest request) {
        try {
            virtualBankService.credit(request.getTransactionId(), request.getAccountId(), request.getAmount());
            return response(request, BankOperationType.CREDIT, "SUCCESS", "NONE", "Credit applied");
        } catch (OperationTimeoutException ex) {
            throw ex;
        } catch (DomainException ex) {
            return response(request, BankOperationType.CREDIT, "FAILED", "ACCOUNT_NOT_FOUND", ex.getMessage());
        }
    }

    public BankOperationStatusResponse reverse(OperationRequest request) {
        try {
            virtualBankService.reversalCreditToPayer(request.getTransactionId(), request.getAccountId(), request.getAmount());
            return response(request, BankOperationType.REVERSAL, "SUCCESS", "NONE", "Reversal applied");
        } catch (OperationTimeoutException ex) {
            throw ex;
        } catch (DomainException ex) {
            return response(request, BankOperationType.REVERSAL, "FAILED", "ACCOUNT_NOT_FOUND", ex.getMessage());
        }
    }

    private BankOperationStatusResponse response(
            OperationRequest request,
            BankOperationType operationType,
            String status,
            String reasonCode,
            String message
    ) {
        OffsetDateTime processedAt = virtualBankService
                .getOperationProcessedAt(request.getTransactionId(), operationType)
                .orElse(OffsetDateTime.now());
        return new BankOperationStatusResponse(
                request.getTransactionId(),
                operationType.name(),
                status,
                reasonCode,
                message,
                processedAt
        );
    }
}
