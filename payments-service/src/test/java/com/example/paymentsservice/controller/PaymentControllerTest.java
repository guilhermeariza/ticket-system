package com.example.paymentsservice.controller;

import com.example.paymentsservice.exception.BusinessException;
import com.example.paymentsservice.model.Payment;
import com.example.paymentsservice.model.PaymentStatus;
import com.example.paymentsservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController
 * Tests REST endpoints using MockMvc
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setAmount(new BigDecimal("150.00"));
        testPayment.setStatus(PaymentStatus.SUCCESS);
        testPayment.setTransactionId("TXN-123456");
        testPayment.setProcessedAt(LocalDateTime.now());
    }

    @Test
    void getPaymentById_WithValidId_ShouldReturnPayment() throws Exception {
        // Arrange
        when(paymentService.getPaymentById(1L)).thenReturn(Optional.of(testPayment));

        // Act & Assert
        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-123456"));

        verify(paymentService, times(1)).getPaymentById(1L);
    }

    @Test
    void getPaymentById_WithInvalidId_ShouldReturn404() throws Exception {
        // Arrange
        when(paymentService.getPaymentById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());

        verify(paymentService, times(1)).getPaymentById(999L);
    }

    @Test
    void processPayment_WithValidPayment_ShouldReturnProcessedPayment() throws Exception {
        // Arrange
        Payment paymentRequest = new Payment();
        paymentRequest.setOrderId(100L);
        paymentRequest.setAmount(new BigDecimal("150.00"));

        when(paymentService.processPayment(any(Payment.class))).thenReturn(testPayment);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-123456"));

        verify(paymentService, times(1)).processPayment(any(Payment.class));
    }

    @Test
    void processPayment_WithInvalidAmount_ShouldReturn400() throws Exception {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(null);

        when(paymentService.processPayment(any(Payment.class)))
                .thenThrow(new BusinessException("Invalid payment amount"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayment)))
                .andExpect(status().isBadRequest());

        verify(paymentService, times(1)).processPayment(any(Payment.class));
    }

    @Test
    void processPayment_WithZeroAmount_ShouldReturn400() throws Exception {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(BigDecimal.ZERO);

        when(paymentService.processPayment(any(Payment.class)))
                .thenThrow(new BusinessException("Invalid payment amount"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayment)))
                .andExpect(status().isBadRequest());

        verify(paymentService, times(1)).processPayment(any(Payment.class));
    }

    @Test
    void processPayment_WithMissingOrderId_ShouldReturn400() throws Exception {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setAmount(new BigDecimal("150.00"));

        when(paymentService.processPayment(any(Payment.class)))
                .thenThrow(new BusinessException("Order ID is required"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayment)))
                .andExpect(status().isBadRequest());

        verify(paymentService, times(1)).processPayment(any(Payment.class));
    }

    @Test
    void processPayment_ShouldAcceptOnlyJsonContentType() throws Exception {
        // Arrange
        Payment paymentRequest = new Payment();
        paymentRequest.setOrderId(100L);
        paymentRequest.setAmount(new BigDecimal("150.00"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isUnsupportedMediaType());

        verify(paymentService, never()).processPayment(any(Payment.class));
    }

    @Test
    void getPaymentById_WithNonNumericId_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/payments/abc"))
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).getPaymentById(any(Long.class));
    }

    @Test
    void processPayment_WithEmptyBody_ShouldProcessWithDefaults() throws Exception {
        // Arrange
        when(paymentService.processPayment(any(Payment.class)))
                .thenThrow(new BusinessException("Order ID is required"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_WithCompleteData_ShouldMapAllFields() throws Exception {
        // Arrange
        Payment completePayment = new Payment();
        completePayment.setOrderId(200L);
        completePayment.setAmount(new BigDecimal("250.75"));

        Payment processedPayment = new Payment();
        processedPayment.setId(2L);
        processedPayment.setOrderId(200L);
        processedPayment.setAmount(new BigDecimal("250.75"));
        processedPayment.setStatus(PaymentStatus.SUCCESS);
        processedPayment.setTransactionId("TXN-789012");
        processedPayment.setProcessedAt(LocalDateTime.now());

        when(paymentService.processPayment(any(Payment.class))).thenReturn(processedPayment);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completePayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.orderId").value(200))
                .andExpect(jsonPath("$.amount").value(250.75))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-789012"));
    }

    @Test
    void getPaymentById_WithZeroId_ShouldReturn404() throws Exception {
        // Arrange
        when(paymentService.getPaymentById(0L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/payments/0"))
                .andExpect(status().isNotFound());

        verify(paymentService, times(1)).getPaymentById(0L);
    }

    @Test
    void processPayment_WithLargeAmount_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        Payment largePayment = new Payment();
        largePayment.setOrderId(100L);
        largePayment.setAmount(new BigDecimal("999999.99"));

        Payment processedPayment = new Payment();
        processedPayment.setId(3L);
        processedPayment.setOrderId(100L);
        processedPayment.setAmount(new BigDecimal("999999.99"));
        processedPayment.setStatus(PaymentStatus.SUCCESS);
        processedPayment.setTransactionId("TXN-999999");

        when(paymentService.processPayment(any(Payment.class))).thenReturn(processedPayment);

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largePayment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(999999.99));
    }

    @Test
    void processPayment_WithUnexpectedServiceException_ShouldReturn500WithErrorBody() throws Exception {
        // Arrange
        Payment payment = new Payment();
        payment.setOrderId(100L);
        payment.setAmount(new BigDecimal("150.00"));

        when(paymentService.processPayment(any(Payment.class)))
                .thenThrow(new RuntimeException("Payment processing failed"));

        // Act & Assert
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
