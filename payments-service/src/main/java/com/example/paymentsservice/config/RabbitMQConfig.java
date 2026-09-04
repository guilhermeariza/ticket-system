package com.example.paymentsservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_EXCHANGE = "orders.exchange";
    public static final String PAYMENTS_EXCHANGE = "payments.exchange";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue.payments";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE);
    }

    @Bean
    public Binding binding(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}