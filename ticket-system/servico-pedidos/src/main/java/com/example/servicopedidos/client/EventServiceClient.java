package com.example.servicopedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.math.BigDecimal;

@FeignClient(name = "servico-eventos", fallback = EventServiceFallback.class)
public interface EventServiceClient {

    @GetMapping("/api/events/ticket-types/{ticketTypeId}/available-quantity")
    Integer getAvailableQuantity(@PathVariable("ticketTypeId") Long ticketTypeId);

    @GetMapping("/api/events/ticket-types/{ticketTypeId}/price")
    BigDecimal getTicketPrice(@PathVariable("ticketTypeId") Long ticketTypeId);

    @PutMapping("/api/events/ticket-types/{ticketTypeId}/decrement-quantity/{quantity}")
    void decrementTicketQuantity(@PathVariable("ticketTypeId") Long ticketTypeId, @PathVariable("quantity") Integer quantity);
}
