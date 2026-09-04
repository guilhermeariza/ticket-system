package com.example.servicoeventos.exception;

public class InsufficientTicketsException extends RuntimeException {
    public InsufficientTicketsException(String message) {
        super(message);
    }
}
