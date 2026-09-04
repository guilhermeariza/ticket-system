package com.guilhermeariza.ticketsystem.paymentsservice.service;

import com.guilhermeariza.ticketsystem.paymentsservice.config.RabbitMQConfig;
import com.guilhermeariza.ticketsystem.paymentsservice.event.OrderCreatedEvent;
import com.guilhermeariza.ticketsystem.paymentsservice.event.PaymentProcessedEvent;
import com.guilhermeariza.ticketsystem.paymentsservice.model.Payment;
import com.guilhermeariza.ticketsystem.paymentsservice.model.PaymentStatus;
import com.guilhermeariza.ticketsystem.paymentsservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService
 * Tests business logic in isolation using Mockito
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private OrderCreatedEvent testOrderEvent;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setAmount(new BigDecimal("150.00"));
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setTransactionId("TXN-123456");

        testOrderEvent = new OrderCreatedEvent();
        testOrderEvent.setOrderId(100L);
        testOrderEvent.setUserId("user123");
        testOrderEvent.setTotalAmount(new BigDecimal("150.00"));
    }

    @Test
    void getPaymentById_WithValidId_ShouldReturnPayment() {
        // Arrange
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        // Act
        Optional<Payment> result = paymentService.getPaymentById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(new BigDecimal("150.00"), result.get().getAmount());
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void getPaymentById_WithInvalidId_ShouldReturnEmpty() {
        // Arrange
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Payment> result = paymentService.getPaymentById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(paymentRepository, times(1)).findById(999L);
    }

    @Test
    void processPayment_WithValidPayment_ShouldSaveAndPublishEvent() {
        // Arrange
        Payment paymentToProcess = new Payment();
        paymentToProcess.setOrderId(100L);
        paymentToProcess.setAmount(new BigDecimal("150.00"));

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);

        // Act
        Payment result = paymentService.processPayment(paymentToProcess);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTransactionId());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.PAYMENTS_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY),
                eventCaptor.capture()
        );

        PaymentProcessedEvent event = eventCaptor.getValue();
        assertEquals(100L, event.getOrderId());
        assertTrue(event.isSuccess());
    }

    @Test
    void processPayment_WithNullAmount_ShouldThrowException() {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(invalidPayment);
        });

        assertEquals("Invalid payment amount", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void processPayment_WithZeroAmount_ShouldThrowException() {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(BigDecimal.ZERO);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(invalidPayment);
        });

        assertEquals("Invalid payment amount", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_WithNegativeAmount_ShouldThrowException() {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(new BigDecimal("-50.00"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(invalidPayment);
        });

        assertEquals("Invalid payment amount", exception.getMessage());
    }

    @Test
    void processPayment_WithNullOrderId_ShouldThrowException() {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(null);
        invalidPayment.setAmount(new BigDecimal("150.00"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(invalidPayment);
        });

        assertEquals("Order ID is required", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_ShouldGenerateTransactionId() {
        // Arrange
        Payment paymentToProcess = new Payment();
        paymentToProcess.setOrderId(100L);
        paymentToProcess.setAmount(new BigDecimal("150.00"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        Payment result = paymentService.processPayment(paymentToProcess);

        // Assert
        assertNotNull(result.getTransactionId());
        assertNotNull(result.getProcessedAt());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
    }

    @Test
    void processOrder_ShouldCreatePaymentAndPublishEvent() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);

        // Act
        paymentService.processOrder(testOrderEvent);

        // Assert
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.PAYMENTS_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY),
                eventCaptor.capture()
        );

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(100L, savedPayment.getOrderId());
        assertEquals(new BigDecimal("150.00"), savedPayment.getAmount());
        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
        assertNotNull(savedPayment.getTransactionId());

        PaymentProcessedEvent event = eventCaptor.getValue();
        assertEquals(100L, event.getOrderId());
        assertTrue(event.isSuccess());
    }

    @Test
    void processOrder_ShouldHandleInterruptedException() {
        // Arrange
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // Interrupt the current thread before processing
        Thread.currentThread().interrupt();

        // Act
        paymentService.processOrder(testOrderEvent);

        // Assert
        assertTrue(Thread.interrupted()); // Clear the interrupt flag
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_WithLargeAmount_ShouldProcessSuccessfully() {
        // Arrange
        Payment largePayment = new Payment();
        largePayment.setOrderId(200L);
        largePayment.setAmount(new BigDecimal("999999.99"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payment result = paymentService.processPayment(largePayment);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("999999.99"), result.getAmount());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
    }

    @Test
    void processPayment_ShouldPublishSuccessEventOnSuccess() {
        // Arrange
        Payment payment = new Payment();
        payment.setOrderId(100L);
        payment.setAmount(new BigDecimal("50.00"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ArgumentCaptor<PaymentProcessedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentProcessedEvent.class);

        // Act
        paymentService.processPayment(payment);

        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.PAYMENTS_EXCHANGE),
                eq(RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY),
                eventCaptor.capture()
        );

        PaymentProcessedEvent event = eventCaptor.getValue();
        assertTrue(event.isSuccess());
    }

    @Test
    void processOrder_WithDifferentAmounts_ShouldCreateCorrectPayments() {
        // Arrange
        OrderCreatedEvent event1 = new OrderCreatedEvent();
        event1.setOrderId(1L);
        event1.setTotalAmount(new BigDecimal("25.00"));

        OrderCreatedEvent event2 = new OrderCreatedEvent();
        event2.setOrderId(2L);
        event2.setTotalAmount(new BigDecimal("75.50"));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        // Act
        paymentService.processOrder(event1);
        paymentService.processOrder(event2);

        // Assert
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());

        assertEquals(new BigDecimal("25.00"), paymentCaptor.getAllValues().get(0).getAmount());
        assertEquals(new BigDecimal("75.50"), paymentCaptor.getAllValues().get(1).getAmount());
    }
}
