package com.guilhermeariza.ticketsystem.ordersservice.exception;

public class InsufficientTicketsException extends RuntimeException {
    public InsufficientTicketsException(String message) {
        super(message);
    }
}
