package com.example.servicoeventos.controller;

import com.example.servicoeventos.exception.ResourceNotFoundException;
import com.example.servicoeventos.model.Event;
import com.example.servicoeventos.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setDate(LocalDateTime.now().plusDays(7));
        testEvent.setLocation("Test Location");
    }

    @Test
    void getAllEvents_ShouldReturnPageOfEvents() throws Exception {
        // Arrange
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(testEvent), PageRequest.of(0, 10), 1);
        when(eventService.getAllEvents(any())).thenReturn(eventPage);

        // Act & Assert
        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test Event"))
                .andExpect(jsonPath("$.content[0].location").value("Test Location"));
    }

    @Test
    void getEventById_WhenExists_ShouldReturnEvent() throws Exception {
        // Arrange
        when(eventService.getEventById(1L)).thenReturn(Optional.of(testEvent));

        // Act & Assert
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Event"));
    }

    @Test
    void getEventById_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(eventService.getEventById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEvent_WithValidData_ShouldReturn200() throws Exception {
        // Arrange
        when(eventService.createEvent(any(Event.class))).thenReturn(testEvent);

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Event"));
    }

    @Test
    void createEvent_WithInvalidName_ShouldReturn400() throws Exception {
        // Arrange
        testEvent.setName("ab"); // Too short (min 3)

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_WithEmptyLocation_ShouldReturn400() throws Exception {
        // Arrange
        testEvent.setLocation("");

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_WhenExists_ShouldReturnUpdated() throws Exception {
        // Arrange
        when(eventService.updateEvent(anyLong(), any(Event.class))).thenReturn(testEvent);

        // Act & Assert
        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Event"));
    }

    @Test
    void updateEvent_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(eventService.updateEvent(anyLong(), any(Event.class)))
                .thenThrow(new ResourceNotFoundException("Event", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/events/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEvent_ShouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAvailableQuantity_WhenExists_ShouldReturnQuantity() throws Exception {
        // Arrange
        when(eventService.getAvailableQuantity(1L)).thenReturn(50);

        // Act & Assert
        mockMvc.perform(get("/api/events/ticket-types/1/available-quantity"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));
    }

    @Test
    void getAvailableQuantity_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(eventService.getAvailableQuantity(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/events/ticket-types/999/available-quantity"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTicketPrice_WhenExists_ShouldReturnPrice() throws Exception {
        // Arrange
        when(eventService.getTicketPrice(1L)).thenReturn(new BigDecimal("100.00"));

        // Act & Assert
        mockMvc.perform(get("/api/events/ticket-types/1/price"))
                .andExpect(status().isOk())
                .andExpect(content().string("100.00"));
    }

    @Test
    void getTicketPrice_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(eventService.getTicketPrice(999L)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/events/ticket-types/999/price"))
                .andExpect(status().isNotFound());
    }

    @Test
    void decrementTicketQuantity_WithValidData_ShouldReturn200() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/events/ticket-types/1/decrement-quantity/10"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllEvents_WithSorting_ShouldReturnSorted() throws Exception {
        // Arrange
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(testEvent), PageRequest.of(0, 10), 1);
        when(eventService.getAllEvents(any())).thenReturn(eventPage);

        // Act & Assert
        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "date")
                        .param("direction", "ASC"))
                .andExpect(status().isOk());
    }
}
