package com.example.paymentsservice.service;

import com.example.paymentsservice.config.RabbitMQConfig;
import com.example.paymentsservice.event.OrderCreatedEvent;
import com.example.paymentsservice.event.PaymentProcessedEvent;
import com.example.paymentsservice.model.Payment;
import com.example.paymentsservice.model.PaymentStatus;
import com.example.paymentsservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public Payment processPayment(Payment payment) {
        log.info("Processing payment for Order ID: {}", payment.getOrderId());

        // Validate payment
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            throw new RuntimeException("Invalid payment amount");
        }
        if (payment.getOrderId() == null) {
            throw new RuntimeException("Order ID is required");
        }

        // Generate transaction ID
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.SUCCESS);

        // Save payment
        Payment savedPayment = paymentRepository.save(payment);

        // Publish payment processed event
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                savedPayment.getOrderId(),
                savedPayment.getStatus() == PaymentStatus.SUCCESS
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENTS_EXCHANGE,
                RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY,
                paymentEvent
        );

        log.info("Payment processed successfully with transaction ID: {}", savedPayment.getTransactionId());
        return savedPayment;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void processOrder(OrderCreatedEvent orderEvent) {
        log.info("Received order to process payment: Order ID {}", orderEvent.getOrderId());

        // Simulate payment processing
        try {
            Thread.sleep(2000); // Simulate a 2-second processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Create and save payment record
        Payment payment = new Payment();
        payment.setOrderId(orderEvent.getOrderId());
        payment.setAmount(orderEvent.getTotalAmount());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setProcessedAt(LocalDateTime.now());
        payment.setStatus(PaymentStatus.SUCCESS);

        paymentRepository.save(payment);

        // Publish payment processed event
        PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(orderEvent.getOrderId(), true);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENTS_EXCHANGE, RabbitMQConfig.PAYMENT_PROCESSED_ROUTING_KEY, paymentEvent);

        log.info("Payment for Order ID {} processed with status: SUCCESS", orderEvent.getOrderId());
    }
}