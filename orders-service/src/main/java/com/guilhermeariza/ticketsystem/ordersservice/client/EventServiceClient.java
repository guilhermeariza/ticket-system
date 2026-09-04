package com.guilhermeariza.ticketsystem.ordersservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.math.BigDecimal;

@FeignClient(name = "events-service", fallbackFactory = EventServiceFallbackFactory.class)
public interface EventServiceClient {

    @GetMapping("/api/events/ticket-types/{ticketTypeId}/price")
    BigDecimal getTicketPrice(@PathVariable("ticketTypeId") Long ticketTypeId);

    @PutMapping("/api/events/ticket-types/{ticketTypeId}/decrement-quantity/{quantity}")
    void decrementTicketQuantity(@PathVariable("ticketTypeId") Long ticketTypeId, @PathVariable("quantity") Integer quantity);

    @PostMapping("/api/events/ticket-types/{ticketTypeId}/release/{quantity}")
    void releaseTickets(@PathVariable("ticketTypeId") Long ticketTypeId, @PathVariable("quantity") Integer quantity);
}
