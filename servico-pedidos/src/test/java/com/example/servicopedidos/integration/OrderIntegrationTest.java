package com.example.servicopedidos.integration;

import com.example.servicopedidos.dto.OrderRequest;
import com.example.servicopedidos.model.Order;
import com.example.servicopedidos.model.OrderStatus;
import com.example.servicopedidos.repository.OrderRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Order functionality
 * Tests the full stack from controller to repository
 *
 * Note: These tests use @SpringBootTest which loads the full application context
 * For true integration tests, you would need a running database and other services
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        orderRequest = new OrderRequest();
        orderRequest.setTicketTypeId(1L);
        orderRequest.setQuantity(2);
    }

    @Test
    void testGetAllOrders_WithPagination() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateOrder_WithoutUserId_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCreateOrder_WithInvalidData_BadRequest() throws Exception {
        orderRequest.setQuantity(null);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_WithZeroQuantity_BadRequest() throws Exception {
        orderRequest.setQuantity(0);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testPaginationParameters() throws Exception {
        // Test with different page sizes
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5));

        mockMvc.perform(get("/api/orders")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void testSortingParameters() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .param("sortBy", "createdAt")
                        .param("direction", "ASC"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders")
                        .param("sortBy", "totalAmount")
                        .param("direction", "DESC"))
                .andExpect(status().isOk());
    }

    @Test
    void testValidationConstraints() throws Exception {
        // Test null ticket type
        orderRequest.setTicketTypeId(null);
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());

        // Test negative quantity
        orderRequest.setTicketTypeId(1L);
        orderRequest.setQuantity(-1);
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }
}
