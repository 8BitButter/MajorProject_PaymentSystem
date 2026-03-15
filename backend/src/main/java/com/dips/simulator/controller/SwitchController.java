package com.dips.simulator.controller;

import com.dips.simulator.dto.BankOperationStatusResponse;
import com.dips.simulator.dto.OperationRequest;
import com.dips.simulator.service.PaymentSwitchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/switch", "/switch"})
public class SwitchController {

    private final PaymentSwitchService paymentSwitchService;

    public SwitchController(PaymentSwitchService paymentSwitchService) {
        this.paymentSwitchService = paymentSwitchService;
    }

    @PostMapping("/debit")
    public BankOperationStatusResponse debit(@RequestBody OperationRequest request) {
        return paymentSwitchService.routeDebit(request);
    }

    @PostMapping("/credit")
    public BankOperationStatusResponse credit(@RequestBody OperationRequest request) {
        return paymentSwitchService.routeCredit(request);
    }

    @PostMapping("/reverse")
    public BankOperationStatusResponse reverse(@RequestBody OperationRequest request) {
        return paymentSwitchService.routeReverse(request);
    }
}
