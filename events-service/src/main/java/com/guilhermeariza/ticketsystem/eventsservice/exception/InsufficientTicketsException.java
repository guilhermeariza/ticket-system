package com.guilhermeariza.ticketsystem.eventsservice.exception;

public class InsufficientTicketsException extends RuntimeException {
    public InsufficientTicketsException(String message) {
        super(message);
    }
}
