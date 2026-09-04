package com.guilhermeariza.ticketsystem.usersservice.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
