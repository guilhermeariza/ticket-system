package com.guilhermeariza.ticketsystem.ordersservice.client;

import com.guilhermeariza.ticketsystem.ordersservice.exception.BusinessException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Fallback for EventServiceClient. A business error from events-service (e.g. 409 for
 * insufficient stock, 404 for an unknown ticket type) is rethrown as-is so it reaches the
 * client with the correct status code; only genuine unavailability (timeouts, connection
 * failures, 5xx) is turned into a generic "service unavailable" error.
 */
@Component
public class EventServiceFallbackFactory implements FallbackFactory<EventServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(EventServiceFallbackFactory.class);

    @Override
    public EventServiceClient create(Throwable cause) {
        return new EventServiceClient() {

            @Override
            public BigDecimal getTicketPrice(Long ticketTypeId) {
                if (cause instanceof FeignException.NotFound) {
                    throw (FeignException.NotFound) cause;
                }
                log.warn("EventService is unavailable. Returning null for price of ticket type: {}", ticketTypeId, cause);
                return null;
            }

            @Override
            public void decrementTicketQuantity(Long ticketTypeId, Integer quantity) {
                if (cause instanceof FeignException.Conflict || cause instanceof FeignException.NotFound) {
                    throw (FeignException) cause;
                }
                log.error("EventService is unavailable. Cannot decrement quantity for ticket type: {}. Quantity: {}",
                          ticketTypeId, quantity, cause);
                throw new BusinessException("Event service is currently unavailable. Please try again later.");
            }

            @Override
            public void releaseTickets(Long ticketTypeId, Integer quantity) {
                log.error("EventService is unavailable. Could not release reserved tickets for ticket type: {}. Quantity: {}. Manual reconciliation may be needed.",
                          ticketTypeId, quantity, cause);
            }
        };
    }
}
