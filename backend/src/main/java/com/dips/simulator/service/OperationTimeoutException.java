package com.dips.simulator.service;

public class OperationTimeoutException extends RuntimeException {

    public OperationTimeoutException(String message) {
        super(message);
    }
}
