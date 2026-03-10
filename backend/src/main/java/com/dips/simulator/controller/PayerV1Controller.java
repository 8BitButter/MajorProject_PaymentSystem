package com.dips.simulator.controller;

import com.dips.simulator.dto.PushPaymentRequest;
import com.dips.simulator.dto.PushPaymentResponse;
import com.dips.simulator.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payer")
public class PayerV1Controller {

    private final PaymentService paymentService;

    public PayerV1Controller(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/push")
    public PushPaymentResponse push(@Valid @RequestBody PushPaymentRequest request) {
        return paymentService.initiateOnlinePush(request);
    }
}

