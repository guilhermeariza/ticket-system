package com.guilhermeariza.ticketsystem.eventsservice.integration;

import com.guilhermeariza.ticketsystem.eventsservice.model.Event;
import com.guilhermeariza.ticketsystem.eventsservice.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Event functionality
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();

        testEvent = new Event();
        testEvent.setName("Integration Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setDate(LocalDateTime.now().plusDays(7));
        testEvent.setLocation("Test Location");
    }

    @Test
    void testGetAllEvents_WithPagination() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    void testGetEventById_NotFound() throws Exception {
        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateEvent_WithInvalidName_BadRequest() throws Exception {
        testEvent.setName("ab"); // Too short

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateEvent_WithEmptyLocation_BadRequest() throws Exception {
        testEvent.setLocation("");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testEvent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteEvent() throws Exception {
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testPaginationWithDifferentSizes() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void testSortingByDate() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("sortBy", "date")
                        .param("direction", "ASC"))
                .andExpect(status().isOk());
    }

    @Test
    void testSortingByLocation() throws Exception {
        mockMvc.perform(get("/api/events")
                        .param("sortBy", "location")
                        .param("direction", "DESC"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAvailableQuantity_NotFound() throws Exception {
        mockMvc.perform(get("/api/events/ticket-types/999/available-quantity"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetTicketPrice_NotFound() throws Exception {
        mockMvc.perform(get("/api/events/ticket-types/999/price"))
                .andExpect(status().isNotFound());
    }
}
