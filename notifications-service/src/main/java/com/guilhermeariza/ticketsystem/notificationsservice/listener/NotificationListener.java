package com.guilhermeariza.ticketsystem.notificationsservice.listener;

import com.guilhermeariza.ticketsystem.notificationsservice.config.RabbitMQConfig;
import com.guilhermeariza.ticketsystem.notificationsservice.event.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_PROCESSED_QUEUE)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment processed event for order ID: {}. Preparing to send notification.", event.getOrderId());

        if (event.isSuccess()) {
            // Simulate sending a success email
            log.info("Simulating sending SUCCESS notification for order ID: {}", event.getOrderId());
        } else {
            // Simulate sending a failure email
            log.info("Simulating sending FAILURE notification for order ID: {}", event.getOrderId());
        }
    }
}