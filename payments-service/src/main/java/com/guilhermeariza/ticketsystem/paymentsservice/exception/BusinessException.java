package com.guilhermeariza.ticketsystem.paymentsservice.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
