package com.example.servicoeventos.controller;

import com.example.servicoeventos.model.Event;
import com.example.servicoeventos.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @GetMapping
    public Page<Event> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return eventService.getAllEvents(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Event createEvent(@Valid @RequestBody Event event) {
        return eventService.createEvent(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @Valid @RequestBody Event eventDetails) {
        Event updatedEvent = eventService.updateEvent(id, eventDetails);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoints for Feign Client
    @GetMapping("/ticket-types/{ticketTypeId}/available-quantity")
    public ResponseEntity<Integer> getAvailableQuantity(@PathVariable Long ticketTypeId) {
        Integer quantity = eventService.getAvailableQuantity(ticketTypeId);
        return quantity != null ? ResponseEntity.ok(quantity) : ResponseEntity.notFound().build();
    }

    @GetMapping("/ticket-types/{ticketTypeId}/price")
    public ResponseEntity<BigDecimal> getTicketPrice(@PathVariable Long ticketTypeId) {
        BigDecimal price = eventService.getTicketPrice(ticketTypeId);
        return price != null ? ResponseEntity.ok(price) : ResponseEntity.notFound().build();
    }

    @PutMapping("/ticket-types/{ticketTypeId}/decrement-quantity/{quantity}")
    public ResponseEntity<Void> decrementTicketQuantity(@PathVariable Long ticketTypeId, @PathVariable Integer quantity) {
        eventService.decrementTicketQuantity(ticketTypeId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ticket-types/{ticketTypeId}/release/{quantity}")
    public ResponseEntity<Void> releaseTicketQuantity(@PathVariable Long ticketTypeId, @PathVariable Integer quantity) {
        eventService.releaseTicketQuantity(ticketTypeId, quantity);
        return ResponseEntity.ok().build();
    }
}
