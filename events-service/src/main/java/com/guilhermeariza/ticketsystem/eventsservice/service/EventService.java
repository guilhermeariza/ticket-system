package com.guilhermeariza.ticketsystem.eventsservice.service;

import com.guilhermeariza.ticketsystem.eventsservice.exception.InsufficientTicketsException;
import com.guilhermeariza.ticketsystem.eventsservice.exception.ResourceNotFoundException;
import com.guilhermeariza.ticketsystem.eventsservice.model.Event;
import com.guilhermeariza.ticketsystem.eventsservice.model.TicketType;
import com.guilhermeariza.ticketsystem.eventsservice.repository.EventRepository;
import com.guilhermeariza.ticketsystem.eventsservice.repository.TicketTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    public Page<Event> getAllEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    public Event updateEvent(Long id, Event eventDetails) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        event.setName(eventDetails.getName());
        event.setDescription(eventDetails.getDescription());
        event.setDate(eventDetails.getDate());
        event.setLocation(eventDetails.getLocation());
        event.setTicketTypes(eventDetails.getTicketTypes());
        return eventRepository.save(event);
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Integer getAvailableQuantity(Long ticketTypeId) {
        return ticketTypeRepository.findById(ticketTypeId)
                .map(TicketType::getAvailableQuantity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTicketPrice(Long ticketTypeId) {
        return ticketTypeRepository.findById(ticketTypeId)
                .map(TicketType::getPrice)
                .orElse(null);
    }

    @Transactional
    public void decrementTicketQuantity(Long ticketTypeId, Integer quantity) {
        int rowsAffected = ticketTypeRepository.decrementIfAvailable(ticketTypeId, quantity);
        if (rowsAffected == 0) {
            if (!ticketTypeRepository.existsById(ticketTypeId)) {
                throw new ResourceNotFoundException("TicketType", ticketTypeId);
            }
            throw new InsufficientTicketsException("Not enough tickets available for ticket type " + ticketTypeId);
        }
    }

    @Transactional
    public void releaseTicketQuantity(Long ticketTypeId, Integer quantity) {
        if (!ticketTypeRepository.existsById(ticketTypeId)) {
            throw new ResourceNotFoundException("TicketType", ticketTypeId);
        }
        ticketTypeRepository.incrementAvailable(ticketTypeId, quantity);
    }
}
