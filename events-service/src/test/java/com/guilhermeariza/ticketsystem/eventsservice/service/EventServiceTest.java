package com.guilhermeariza.ticketsystem.eventsservice.service;

import com.guilhermeariza.ticketsystem.eventsservice.exception.InsufficientTicketsException;
import com.guilhermeariza.ticketsystem.eventsservice.exception.ResourceNotFoundException;
import com.guilhermeariza.ticketsystem.eventsservice.model.Event;
import com.guilhermeariza.ticketsystem.eventsservice.model.TicketType;
import com.guilhermeariza.ticketsystem.eventsservice.repository.EventRepository;
import com.guilhermeariza.ticketsystem.eventsservice.repository.TicketTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private TicketType testTicketType;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setDate(LocalDateTime.now().plusDays(7));
        testEvent.setLocation("Test Location");

        testTicketType = new TicketType();
        testTicketType.setId(1L);
        testTicketType.setName("VIP");
        testTicketType.setPrice(new BigDecimal("100.00"));
        testTicketType.setTotalQuantity(100);
        testTicketType.setAvailableQuantity(50);
        testTicketType.setEvent(testEvent);
    }

    @Test
    void getAllEvents_ShouldReturnPageOfEvents() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Event> events = Arrays.asList(testEvent);
        Page<Event> eventPage = new PageImpl<>(events, pageable, events.size());
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<Event> result = eventService.getAllEvents(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Event", result.getContent().get(0).getName());
        verify(eventRepository).findAll(pageable);
    }

    @Test
    void getEventById_WhenExists_ShouldReturnEvent() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        // Act
        Optional<Event> result = eventService.getEventById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Event", result.get().getName());
        verify(eventRepository).findById(1L);
    }

    @Test
    void getEventById_WhenNotExists_ShouldReturnEmpty() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Event> result = eventService.getEventById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(eventRepository).findById(999L);
    }

    @Test
    void createEvent_ShouldSaveAndReturnEvent() {
        // Arrange
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        Event result = eventService.createEvent(testEvent);

        // Assert
        assertNotNull(result);
        assertEquals("Test Event", result.getName());
        verify(eventRepository).save(testEvent);
    }

    @Test
    void updateEvent_WhenExists_ShouldUpdateAndReturn() {
        // Arrange
        Event updatedDetails = new Event();
        updatedDetails.setName("Updated Event");
        updatedDetails.setDescription("Updated Description");
        updatedDetails.setDate(LocalDateTime.now().plusDays(10));
        updatedDetails.setLocation("Updated Location");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        Event result = eventService.updateEvent(1L, updatedDetails);

        // Assert
        assertNotNull(result);
        verify(eventRepository).findById(1L);
        verify(eventRepository).save(testEvent);
    }

    @Test
    void updateEvent_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            eventService.updateEvent(999L, testEvent);
        });
    }

    @Test
    void deleteEvent_ShouldCallRepository() {
        // Act
        eventService.deleteEvent(1L);

        // Assert
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void getAvailableQuantity_WhenTicketExists_ShouldReturnQuantity() {
        // Arrange
        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));

        // Act
        Integer quantity = eventService.getAvailableQuantity(1L);

        // Assert
        assertNotNull(quantity);
        assertEquals(50, quantity);
        verify(ticketTypeRepository).findById(1L);
    }

    @Test
    void getAvailableQuantity_WhenTicketNotExists_ShouldReturnNull() {
        // Arrange
        when(ticketTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Integer quantity = eventService.getAvailableQuantity(999L);

        // Assert
        assertNull(quantity);
    }

    @Test
    void getTicketPrice_WhenTicketExists_ShouldReturnPrice() {
        // Arrange
        when(ticketTypeRepository.findById(1L)).thenReturn(Optional.of(testTicketType));

        // Act
        BigDecimal price = eventService.getTicketPrice(1L);

        // Assert
        assertNotNull(price);
        assertEquals(new BigDecimal("100.00"), price);
        verify(ticketTypeRepository).findById(1L);
    }

    @Test
    void getTicketPrice_WhenTicketNotExists_ShouldReturnNull() {
        // Arrange
        when(ticketTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        BigDecimal price = eventService.getTicketPrice(999L);

        // Assert
        assertNull(price);
    }

    @Test
    void decrementTicketQuantity_WithSufficientTickets_ShouldDecrement() {
        // Arrange
        when(ticketTypeRepository.decrementIfAvailable(1L, 10)).thenReturn(1);

        // Act
        eventService.decrementTicketQuantity(1L, 10);

        // Assert
        verify(ticketTypeRepository).decrementIfAvailable(1L, 10);
    }

    @Test
    void decrementTicketQuantity_WithInsufficientTickets_ShouldThrowException() {
        // Arrange
        when(ticketTypeRepository.decrementIfAvailable(1L, 100)).thenReturn(0);
        when(ticketTypeRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(InsufficientTicketsException.class, () -> {
            eventService.decrementTicketQuantity(1L, 100); // Trying to buy more than available (50)
        });
    }

    @Test
    void decrementTicketQuantity_WhenTicketNotExists_ShouldThrowException() {
        // Arrange
        when(ticketTypeRepository.decrementIfAvailable(999L, 10)).thenReturn(0);
        when(ticketTypeRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            eventService.decrementTicketQuantity(999L, 10);
        });
    }

    @Test
    void decrementTicketQuantity_ToZero_ShouldWork() {
        // Arrange
        when(ticketTypeRepository.decrementIfAvailable(1L, 50)).thenReturn(1);

        // Act
        eventService.decrementTicketQuantity(1L, 50); // Exactly the available quantity

        // Assert
        verify(ticketTypeRepository).decrementIfAvailable(1L, 50);
    }

    @Test
    void decrementTicketQuantity_BelowZero_ShouldThrowException() {
        // Arrange
        when(ticketTypeRepository.decrementIfAvailable(1L, 51)).thenReturn(0);
        when(ticketTypeRepository.existsById(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(InsufficientTicketsException.class, () -> {
            eventService.decrementTicketQuantity(1L, 51); // One more than available
        });
    }

    @Test
    void releaseTicketQuantity_WhenExists_ShouldIncrement() {
        // Arrange
        when(ticketTypeRepository.existsById(1L)).thenReturn(true);

        // Act
        eventService.releaseTicketQuantity(1L, 10);

        // Assert
        verify(ticketTypeRepository).incrementAvailable(1L, 10);
    }

    @Test
    void releaseTicketQuantity_WhenTicketNotExists_ShouldThrowException() {
        // Arrange
        when(ticketTypeRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            eventService.releaseTicketQuantity(999L, 10);
        });
    }
}
