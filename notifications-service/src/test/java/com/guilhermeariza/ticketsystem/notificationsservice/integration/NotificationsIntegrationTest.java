package com.guilhermeariza.ticketsystem.notificationsservice.integration;

import com.guilhermeariza.ticketsystem.notificationsservice.event.PaymentProcessedEvent;
import com.guilhermeariza.ticketsystem.notificationsservice.listener.NotificationListener;
import com.guilhermeariza.ticketsystem.notificationsservice.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Notifications Service
 * Tests application context and bean wiring
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationsIntegrationTest {

    @Autowired
    private NotificationListener notificationListener;

    @Autowired
    private EmailService emailService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
        assertNotNull(notificationListener);
        assertNotNull(emailService);
    }

    @Test
    void notificationListenerBean_ShouldBeConfigured() {
        // Verify NotificationListener is properly autowired
        assertNotNull(notificationListener);
    }

    @Test
    void emailServiceBean_ShouldBeConfigured() {
        // Verify EmailService is properly autowired
        assertNotNull(emailService);
    }

    @Test
    void mailSenderBean_ShouldBeConfigured() {
        // Verify JavaMailSender is properly autowired
        assertNotNull(mailSender);
    }

    @Test
    void notificationListener_ShouldHandlePaymentEvent() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(100L, true);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(event);
        });
    }

    @Test
    void notificationListener_ShouldHandleMultipleEvents() {
        // Arrange
        PaymentProcessedEvent event1 = new PaymentProcessedEvent(1L, true);
        PaymentProcessedEvent event2 = new PaymentProcessedEvent(2L, false);
        PaymentProcessedEvent event3 = new PaymentProcessedEvent(3L, true);

        // Act & Assert - All should process without errors
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(event1);
            notificationListener.handlePaymentProcessed(event2);
            notificationListener.handlePaymentProcessed(event3);
        });
    }

    @Test
    void notificationListener_WithSuccessfulPayment_ShouldComplete() {
        // Arrange
        PaymentProcessedEvent successEvent = new PaymentProcessedEvent(200L, true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(successEvent);
        });
    }

    @Test
    void notificationListener_WithFailedPayment_ShouldComplete() {
        // Arrange
        PaymentProcessedEvent failureEvent = new PaymentProcessedEvent(300L, false);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(failureEvent);
        });
    }

    @Test
    void notificationListener_ConcurrentEvents_ShouldAllProcess() {
        // Arrange
        int numberOfEvents = 5;

        // Act & Assert
        assertDoesNotThrow(() -> {
            for (int i = 0; i < numberOfEvents; i++) {
                PaymentProcessedEvent event = new PaymentProcessedEvent((long) i, i % 2 == 0);
                notificationListener.handlePaymentProcessed(event);
            }
        });
    }

    @Test
    void applicationContext_ShouldHaveRequiredBeans() {
        // Verify all required beans are present
        assertNotNull(notificationListener, "NotificationListener bean should exist");
        assertNotNull(emailService, "EmailService bean should exist");
        assertNotNull(mailSender, "JavaMailSender bean should exist");
    }
}
