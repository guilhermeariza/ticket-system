package com.guilhermeariza.ticketsystem.paymentsservice.integration;

import com.guilhermeariza.ticketsystem.paymentsservice.model.Payment;
import com.guilhermeariza.ticketsystem.paymentsservice.model.PaymentStatus;
import com.guilhermeariza.ticketsystem.paymentsservice.repository.PaymentRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Payment functionality
 * Tests the full stack from controller to repository
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void testFullPaymentLifecycle_CreateAndRead() throws Exception {
        // 1. Create payment
        Payment paymentRequest = new Payment();
        paymentRequest.setOrderId(100L);
        paymentRequest.setAmount(new BigDecimal("150.00"));

        String createResponse = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.processedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Payment createdPayment = objectMapper.readValue(createResponse, Payment.class);
        Long paymentId = createdPayment.getId();

        // 2. Read payment
        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testProcessPayment_PersistsToDatabase() throws Exception {
        // Arrange
        Payment payment = new Payment();
        payment.setOrderId(200L);
        payment.setAmount(new BigDecimal("250.00"));

        // Act
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk());

        // Assert
        long count = paymentRepository.count();
        assert count == 1;
    }

    @Test
    void testProcessPayment_WithInvalidAmount_DoesNotPersist() throws Exception {
        // Arrange
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(BigDecimal.ZERO);

        // Act
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayment)))
                .andExpect(status().isBadRequest());

        // Assert
        long count = paymentRepository.count();
        assert count == 0;
    }

    @Test
    void testGetPaymentById_NotFound() throws Exception {
        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testProcessMultiplePayments_AllPersist() throws Exception {
        // Create payment 1
        Payment payment1 = new Payment();
        payment1.setOrderId(100L);
        payment1.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment1)))
                .andExpect(status().isOk());

        // Create payment 2
        Payment payment2 = new Payment();
        payment2.setOrderId(200L);
        payment2.setAmount(new BigDecimal("200.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment2)))
                .andExpect(status().isOk());

        // Verify both persisted
        long count = paymentRepository.count();
        assert count == 2;
    }

    @Test
    void testProcessPayment_GeneratesUniqueTransactionIds() throws Exception {
        // Create payment 1
        Payment payment1 = new Payment();
        payment1.setOrderId(100L);
        payment1.setAmount(new BigDecimal("50.00"));

        String response1 = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Create payment 2
        Payment payment2 = new Payment();
        payment2.setOrderId(200L);
        payment2.setAmount(new BigDecimal("75.00"));

        String response2 = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify transaction IDs are different
        Payment p1 = objectMapper.readValue(response1, Payment.class);
        Payment p2 = objectMapper.readValue(response2, Payment.class);

        assert !p1.getTransactionId().equals(p2.getTransactionId());
    }

    @Test
    void testProcessPayment_WithNullOrderId_ReturnsBadRequest() throws Exception {
        Payment invalidPayment = new Payment();
        invalidPayment.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayment)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessPayment_WithNegativeAmount_ReturnsBadRequest() throws Exception {
        Payment invalidPayment = new Payment();
        invalidPayment.setOrderId(100L);
        invalidPayment.setAmount(new BigDecimal("-50.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayment)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testProcessPayment_SetsStatusToSuccess() throws Exception {
        Payment payment = new Payment();
        payment.setOrderId(100L);
        payment.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testProcessPayment_SetsProcessedAtTimestamp() throws Exception {
        Payment payment = new Payment();
        payment.setOrderId(100L);
        payment.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedAt").exists())
                .andExpect(jsonPath("$.processedAt").isNotEmpty());
    }
}
