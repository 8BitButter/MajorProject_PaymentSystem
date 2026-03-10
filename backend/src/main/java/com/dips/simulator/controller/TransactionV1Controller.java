package com.dips.simulator.controller;

import com.dips.simulator.dto.TransactionResponse;
import com.dips.simulator.dto.TransactionStepResponse;
import com.dips.simulator.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionV1Controller {

    private final PaymentService paymentService;

    public TransactionV1Controller(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{txId}")
    public TransactionResponse get(@PathVariable UUID txId) {
        return paymentService.getTransaction(txId);
    }

    @GetMapping("/{txId}/timeline")
    public List<TransactionStepResponse> timeline(@PathVariable UUID txId) {
        return paymentService.getTimeline(txId);
    }
}

