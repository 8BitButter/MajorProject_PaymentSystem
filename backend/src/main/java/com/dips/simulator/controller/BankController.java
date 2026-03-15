package com.dips.simulator.controller;

import com.dips.simulator.domain.enums.BankOperationStatus;
import com.dips.simulator.domain.enums.BankOperationType;
import com.dips.simulator.dto.BankOperationStatusResponse;
import com.dips.simulator.dto.OperationRequest;
import com.dips.simulator.service.BankApiService;
import com.dips.simulator.service.DomainException;
import com.dips.simulator.service.VirtualBankService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping({"/api/bank", "/bank"})
public class BankController {

    private final VirtualBankService virtualBankService;
    private final BankApiService bankApiService;

    public BankController(VirtualBankService virtualBankService, BankApiService bankApiService) {
        this.virtualBankService = virtualBankService;
        this.bankApiService = bankApiService;
    }

    @PostMapping("/debit")
    public BankOperationStatusResponse debit(@RequestBody OperationRequest request) {
        return bankApiService.debit(request);
    }

    @PostMapping("/credit")
    public BankOperationStatusResponse credit(@RequestBody OperationRequest request) {
        return bankApiService.credit(request);
    }

    @PostMapping("/reverse")
    public BankOperationStatusResponse reverse(@RequestBody OperationRequest request) {
        return bankApiService.reverse(request);
    }

    @GetMapping("/transaction/{transactionId}")
    public BankOperationStatusResponse transactionStatus(
            @PathVariable UUID transactionId,
            @RequestParam String operation
    ) {
        BankOperationType operationType = parseOperation(operation);
        BankOperationStatus status = virtualBankService.getOperationStatus(transactionId, operationType);
        OffsetDateTime processedAt = virtualBankService.getOperationProcessedAt(transactionId, operationType).orElse(null);

        String reasonCode = switch (status) {
            case SUCCESS -> "NONE";
            case FAILED -> "OPERATION_FAILED";
            case NOT_FOUND -> "NOT_FOUND";
        };
        String message = switch (status) {
            case SUCCESS -> "Operation already applied";
            case FAILED -> "Operation was processed and failed";
            case NOT_FOUND -> "Operation not found";
        };

        return new BankOperationStatusResponse(
                transactionId,
                operationType.name(),
                status.name(),
                reasonCode,
                message,
                processedAt
        );
    }

    private BankOperationType parseOperation(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException("operation query param is required");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("REVERSE".equals(normalized)) {
            return BankOperationType.REVERSAL;
        }
        try {
            return BankOperationType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Unsupported operation: " + raw);
        }
    }
}
