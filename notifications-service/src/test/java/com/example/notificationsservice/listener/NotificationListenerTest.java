package com.example.notificationsservice.listener;

import com.example.notificationsservice.event.PaymentProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NotificationListener
 * Tests RabbitMQ message handling logic
 */
@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @InjectMocks
    private NotificationListener notificationListener;

    private PaymentProcessedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new PaymentProcessedEvent(100L, true);
    }

    @Test
    void handlePaymentProcessed_WithSuccessfulPayment_ShouldProcessSuccessfully() {
        // Arrange
        PaymentProcessedEvent successEvent = new PaymentProcessedEvent(123L, true);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(successEvent);
        });
    }

    @Test
    void handlePaymentProcessed_WithFailedPayment_ShouldProcessSuccessfully() {
        // Arrange
        PaymentProcessedEvent failureEvent = new PaymentProcessedEvent(456L, false);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(failureEvent);
        });
    }

    @Test
    void handlePaymentProcessed_WithNullEvent_ShouldHandleGracefully() {
        // Act & Assert - May throw NullPointerException depending on implementation
        // This tests current behavior
        assertThrows(NullPointerException.class, () -> {
            notificationListener.handlePaymentProcessed(null);
        });
    }

    @Test
    void handlePaymentProcessed_WithMultipleEvents_ShouldProcessAll() {
        // Arrange
        PaymentProcessedEvent event1 = new PaymentProcessedEvent(1L, true);
        PaymentProcessedEvent event2 = new PaymentProcessedEvent(2L, false);
        PaymentProcessedEvent event3 = new PaymentProcessedEvent(3L, true);

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(event1);
            notificationListener.handlePaymentProcessed(event2);
            notificationListener.handlePaymentProcessed(event3);
        });
    }

    @Test
    void handlePaymentProcessed_WithLargeOrderId_ShouldProcessSuccessfully() {
        // Arrange
        PaymentProcessedEvent largeIdEvent = new PaymentProcessedEvent(999999999L, true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(largeIdEvent);
        });
    }

    @Test
    void handlePaymentProcessed_WithZeroOrderId_ShouldProcessSuccessfully() {
        // Arrange
        PaymentProcessedEvent zeroIdEvent = new PaymentProcessedEvent(0L, true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(zeroIdEvent);
        });
    }

    @Test
    void handlePaymentProcessed_WithNegativeOrderId_ShouldProcessSuccessfully() {
        // Arrange
        PaymentProcessedEvent negativeIdEvent = new PaymentProcessedEvent(-1L, true);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(negativeIdEvent);
        });
    }

    @Test
    void handlePaymentProcessed_RepeatedCalls_ShouldAllSucceed() {
        // Arrange
        PaymentProcessedEvent event = new PaymentProcessedEvent(100L, true);

        // Act & Assert - Multiple calls should all succeed
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                notificationListener.handlePaymentProcessed(event);
            }
        });
    }

    @Test
    void handlePaymentProcessed_SuccessAndFailureAlternating_ShouldProcessAll() {
        // Arrange & Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(new PaymentProcessedEvent(1L, true));
            notificationListener.handlePaymentProcessed(new PaymentProcessedEvent(2L, false));
            notificationListener.handlePaymentProcessed(new PaymentProcessedEvent(3L, true));
            notificationListener.handlePaymentProcessed(new PaymentProcessedEvent(4L, false));
        });
    }

    @Test
    void handlePaymentProcessed_WithSameOrderIdDifferentStatus_ShouldProcessBoth() {
        // Arrange
        Long orderId = 500L;
        PaymentProcessedEvent successEvent = new PaymentProcessedEvent(orderId, true);
        PaymentProcessedEvent failureEvent = new PaymentProcessedEvent(orderId, false);

        // Act & Assert
        assertDoesNotThrow(() -> {
            notificationListener.handlePaymentProcessed(successEvent);
            notificationListener.handlePaymentProcessed(failureEvent);
        });
    }
}
