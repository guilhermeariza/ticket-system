package com.example.servicopedidos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Fallback implementation for EventServiceClient
 * Provides graceful degradation when servico-eventos is unavailable
 */
@Component
public class EventServiceFallback implements EventServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EventServiceFallback.class);

    @Override
    public Integer getAvailableQuantity(Long ticketTypeId) {
        log.warn("EventService is unavailable. Returning null for available quantity of ticket type: {}", ticketTypeId);
        return null; // Will trigger validation error in OrderService
    }

    @Override
    public BigDecimal getTicketPrice(Long ticketTypeId) {
        log.warn("EventService is unavailable. Returning null for price of ticket type: {}", ticketTypeId);
        return null; // Will trigger validation error in OrderService
    }

    @Override
    public void decrementTicketQuantity(Long ticketTypeId, Integer quantity) {
        log.error("EventService is unavailable. Cannot decrement quantity for ticket type: {}. Quantity: {}",
                  ticketTypeId, quantity);
        // This is critical - we cannot complete the order if we can't decrement inventory
        throw new RuntimeException("Event service is currently unavailable. Please try again later.");
    }
}
