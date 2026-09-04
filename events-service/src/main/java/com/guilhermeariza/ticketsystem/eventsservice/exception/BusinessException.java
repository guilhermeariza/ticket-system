package com.guilhermeariza.ticketsystem.eventsservice.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
