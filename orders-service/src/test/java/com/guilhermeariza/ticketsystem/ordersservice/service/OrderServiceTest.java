package com.guilhermeariza.ticketsystem.ordersservice.service;

import com.guilhermeariza.ticketsystem.ordersservice.client.EventServiceClient;
import com.guilhermeariza.ticketsystem.ordersservice.config.RabbitMQConfig;
import com.guilhermeariza.ticketsystem.ordersservice.dto.OrderRequest;
import com.guilhermeariza.ticketsystem.ordersservice.event.OrderCreatedEvent;
import com.guilhermeariza.ticketsystem.ordersservice.event.PaymentProcessedEvent;
import com.guilhermeariza.ticketsystem.ordersservice.exception.BusinessException;
import com.guilhermeariza.ticketsystem.ordersservice.exception.InsufficientTicketsException;
import com.guilhermeariza.ticketsystem.ordersservice.exception.ResourceNotFoundException;
import com.guilhermeariza.ticketsystem.ordersservice.model.Order;
import com.guilhermeariza.ticketsystem.ordersservice.model.OrderItem;
import com.guilhermeariza.ticketsystem.ordersservice.model.OrderStatus;
import com.guilhermeariza.ticketsystem.ordersservice.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderRequest testOrderRequest;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderItem = new OrderItem();
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

    private FeignException.Conflict conflictException() {
        Request request = Request.create(Request.HttpMethod.PUT, "/api/events/ticket-types/1/decrement-quantity/2",
                java.util.Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return new FeignException.Conflict("Conflict", request, null, null);
    }

    @Test
    void getAllOrders_ShouldReturnPageOfOrders() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        // Act
        Page<Order> result = orderService.getAllOrders(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user123", result.getContent().get(0).getUserId());
        verify(orderRepository).findAll(pageable);
    }

    @Test
    void getOrderById_WhenExists_ShouldReturnOrder() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        Optional<Order> result = orderService.getOrderById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("user123", result.get().getUserId());
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_WhenNotExists_ShouldReturnEmpty() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Order> result = orderService.getOrderById(999L);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void createOrder_WithSufficientTickets_ShouldCreateOrder() {
        // Arrange
        String userId = "user123";
        when(eventServiceClient.getTicketPrice(1L)).thenReturn(new BigDecimal("50.00"));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        Order result = orderService.createOrder(userId, testOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.getStatus());
        verify(eventServiceClient).getTicketPrice(1L);
        verify(eventServiceClient).decrementTicketQuantity(1L, 2);
        verify(orderRepository).save(any(Order.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDERS_EXCHANGE),
                eq(RabbitMQConfig.ORDER_CREATED_ROUTING_KEY),
                any(OrderCreatedEvent.class)
        );
    }

    @Test
    void createOrder_WhenPriceIsNull_ShouldThrowBusinessException() {
        // Arrange
        when(eventServiceClient.getTicketPrice(anyLong())).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> orderService.createOrder("user123", testOrderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_WhenDecrementFails_ShouldNotPersistOrder() {
        // Arrange
        when(eventServiceClient.getTicketPrice(anyLong())).thenReturn(new BigDecimal("50.00"));
        doThrow(new InsufficientTicketsException("Not enough tickets"))
                .when(eventServiceClient).decrementTicketQuantity(1L, 2);

        // Act & Assert
        assertThrows(InsufficientTicketsException.class, () -> orderService.createOrder("user123", testOrderRequest));
        verify(orderRepository, never()).save(any());
        verify(eventServiceClient, never()).releaseTickets(anyLong(), any());
    }

    @Test
    void createOrder_WhenSaveFailsAfterDecrement_ShouldReleaseReservedTickets() {
        // Arrange
        when(eventServiceClient.getTicketPrice(anyLong())).thenReturn(new BigDecimal("50.00"));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB unavailable"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.createOrder("user123", testOrderRequest));
        verify(eventServiceClient).decrementTicketQuantity(1L, 2);
        verify(eventServiceClient).releaseTickets(1L, 2);
    }

    @Test
    void createOrder_ShouldPublishOrderCreatedEvent() {
        // Arrange
        when(eventServiceClient.getTicketPrice(anyLong())).thenReturn(new BigDecimal("50.00"));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);

        // Act
        orderService.createOrder("user123", testOrderRequest);

        // Assert
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDERS_EXCHANGE),
                eq(RabbitMQConfig.ORDER_CREATED_ROUTING_KEY),
                eventCaptor.capture()
        );

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(testOrder.getId(), capturedEvent.getOrderId());
        assertEquals(testOrder.getUserId(), capturedEvent.getUserId());
    }

    @Test
    void createOrder_ShouldCalculateTotalAmountCorrectly() {
        // Arrange
        when(eventServiceClient.getTicketPrice(anyLong())).thenReturn(new BigDecimal("50.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            assertEquals(new BigDecimal("100.00"), savedOrder.getTotalAmount()); // 2 tickets * 50.00
            return savedOrder;
        });

        // Act
        orderService.createOrder("user123", testOrderRequest);

        // Assert
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderFallback_WhenConflict_ShouldThrowInsufficientTickets() {
        assertThrows(InsufficientTicketsException.class,
                () -> orderService.createOrderFallback("user123", testOrderRequest, conflictException()));
    }

    @Test
    void createOrderFallback_WhenServiceDown_ShouldThrowBusinessException() {
        assertThrows(BusinessException.class,
                () -> orderService.createOrderFallback("user123", testOrderRequest, new java.io.IOException("timeout")));
    }

    @Test
    void updateOrder_WhenExists_ShouldUpdate() {
        // Arrange
        Order updatedDetails = new Order();
        updatedDetails.setUserId("newUser");
        updatedDetails.setTotalAmount(new BigDecimal("200.00"));
        updatedDetails.setStatus(OrderStatus.PAID);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        Order result = orderService.updateOrder(1L, updatedDetails);

        // Assert
        assertNotNull(result);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrder_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.updateOrder(999L, testOrder);
        });
    }

    @Test
    void deleteOrder_ShouldCallRepository() {
        // Act
        orderService.deleteOrder(1L);

        // Assert
        verify(orderRepository).deleteById(1L);
    }

    @Test
    void updateOrderStatus_WhenExists_ShouldUpdateStatus() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.updateOrderStatus(1L, OrderStatus.PAID);

        // Assert
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void updateOrderStatus_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.updateOrderStatus(999L, OrderStatus.PAID);
        });
    }

    @Test
    void handlePaymentProcessed_WithSuccessfulPayment_ShouldSetStatusToPaid() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(1L, true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.handlePaymentProcessed(event);

        // Assert
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    void handlePaymentProcessed_WithFailedPayment_ShouldSetStatusToCancelled() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(1L, false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.handlePaymentProcessed(event);

        // Assert
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
    }
}
