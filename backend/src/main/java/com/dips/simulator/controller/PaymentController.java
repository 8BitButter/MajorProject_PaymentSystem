package com.dips.simulator.controller;

import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.dto.PushPaymentResponse;
import com.dips.simulator.dto.TransactionEventResponse;
import com.dips.simulator.dto.TransactionResponse;
import com.dips.simulator.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/push")
    public PushPaymentResponse push(@Valid @RequestBody PushPaymentRequest request) {
        return paymentService.initiateOnlinePush(request);
    }

    @GetMapping("/{txId}")
    public TransactionResponse get(@PathVariable UUID txId) {
        return paymentService.getTransaction(txId);
    }

    @GetMapping("/{txId}/events")
    public List<TransactionEventResponse> events(@PathVariable UUID txId) {
        return paymentService.getEvents(txId);
    }
}

