package com.example.servicopedidos.controller;

import com.example.servicopedidos.dto.OrderRequest;
import com.example.servicopedidos.exception.ResourceNotFoundException;
import com.example.servicopedidos.model.Order;
import com.example.servicopedidos.model.OrderItem;
import com.example.servicopedidos.model.OrderStatus;
import com.example.servicopedidos.service.OrderService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private Order testOrder;
    private OrderRequest testOrderRequest;

    @BeforeEach
    void setUp() {
        OrderItem testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setTicketTypeId(1L);
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(new BigDecimal("50.00"));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId("user123");
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        items.add(testOrderItem);
        testOrder.setItems(items);

        testOrderRequest = new OrderRequest();
        testOrderRequest.setTicketTypeId(1L);
        testOrderRequest.setQuantity(2);
    }

    @Test
    void getAllOrders_ShouldReturnPageOfOrders() throws Exception {
        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), PageRequest.of(0, 10), 1);
        when(orderService.getAllOrders(any())).thenReturn(orderPage);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user123"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrder() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user123"));
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(orderService.getOrderById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_WithValidDataAndUserId_ShouldReturn200() throws Exception {
        // Arrange
        when(orderService.createOrder(anyString(), any(OrderRequest.class)))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"));
    }

    @Test
    void createOrder_WithoutUserId_ShouldReturn401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_WithEmptyUserId_ShouldReturn401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createOrder_WithInvalidTicketTypeId_ShouldReturn400() throws Exception {
        // Arrange
        testOrderRequest.setTicketTypeId(null);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_WithZeroQuantity_ShouldReturn400() throws Exception {
        // Arrange
        testOrderRequest.setQuantity(0);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_WithNegativeQuantity_ShouldReturn400() throws Exception {
        // Arrange
        testOrderRequest.setQuantity(-1);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrder_WhenExists_ShouldReturnUpdated() throws Exception {
        // Arrange
        when(orderService.updateOrder(any(), any(Order.class))).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(put("/api/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user123"));
    }

    @Test
    void updateOrder_WhenNotExists_ShouldReturn404() throws Exception {
        // Arrange
        when(orderService.updateOrder(any(), any(Order.class)))
                .thenThrow(new ResourceNotFoundException("Order", 999L));

        // Act & Assert
        mockMvc.perform(put("/api/orders/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrder)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOrder_ShouldReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllOrders_WithPaginationAndSorting_ShouldReturnSorted() throws Exception {
        // Arrange
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), PageRequest.of(0, 5), 1);
        when(orderService.getAllOrders(any())).thenReturn(orderPage);

        // Act & Assert
        mockMvc.perform(get("/api/orders")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "createdAt")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").exists());
    }
}
