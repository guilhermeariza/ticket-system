package com.example.servicoeventos.controller;

import com.example.servicoeventos.exception.ErrorResponse;
import com.example.servicoeventos.model.Event;
import com.example.servicoeventos.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Events", description = "Event and ticket type management")
public class EventController {

    @Autowired
    private EventService eventService;

    @Operation(summary = "List events", description = "Returns a paginated, sortable list of events")
    @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
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

    @Operation(summary = "Get an event by id")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create an event")
    @ApiResponse(responseCode = "200", description = "Event created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid event data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    public Event createEvent(@Valid @RequestBody Event event) {
        return eventService.createEvent(event);
    }

    @Operation(summary = "Update an event")
    @ApiResponse(responseCode = "200", description = "Event updated successfully")
    @ApiResponse(responseCode = "404", description = "Event not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @Valid @RequestBody Event eventDetails) {
        Event updatedEvent = eventService.updateEvent(id, eventDetails);
        return ResponseEntity.ok(updatedEvent);
    }

    @Operation(summary = "Delete an event")
    @ApiResponse(responseCode = "204", description = "Event deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoints for Feign Client
    @Operation(summary = "Get available ticket quantity for a ticket type")
    @ApiResponse(responseCode = "200", description = "Quantity retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Ticket type not found")
    @GetMapping("/ticket-types/{ticketTypeId}/available-quantity")
    public ResponseEntity<Integer> getAvailableQuantity(@PathVariable Long ticketTypeId) {
        Integer quantity = eventService.getAvailableQuantity(ticketTypeId);
        return quantity != null ? ResponseEntity.ok(quantity) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get the price of a ticket type")
    @ApiResponse(responseCode = "200", description = "Price retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Ticket type not found")
    @GetMapping("/ticket-types/{ticketTypeId}/price")
    public ResponseEntity<BigDecimal> getTicketPrice(@PathVariable Long ticketTypeId) {
        BigDecimal price = eventService.getTicketPrice(ticketTypeId);
        return price != null ? ResponseEntity.ok(price) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Reserve tickets",
            description = "Atomically decrements available quantity; this is the only availability check performed.")
    @ApiResponse(responseCode = "200", description = "Tickets reserved successfully")
    @ApiResponse(responseCode = "404", description = "Ticket type not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Not enough tickets available",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/ticket-types/{ticketTypeId}/decrement-quantity/{quantity}")
    public ResponseEntity<Void> decrementTicketQuantity(@PathVariable Long ticketTypeId, @PathVariable Integer quantity) {
        eventService.decrementTicketQuantity(ticketTypeId, quantity);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Release previously reserved tickets",
            description = "Compensating action used when an order fails after tickets were reserved.")
    @ApiResponse(responseCode = "200", description = "Tickets released successfully")
    @ApiResponse(responseCode = "404", description = "Ticket type not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/ticket-types/{ticketTypeId}/release/{quantity}")
    public ResponseEntity<Void> releaseTicketQuantity(@PathVariable Long ticketTypeId, @PathVariable Integer quantity) {
        eventService.releaseTicketQuantity(ticketTypeId, quantity);
        return ResponseEntity.ok().build();
    }
}
