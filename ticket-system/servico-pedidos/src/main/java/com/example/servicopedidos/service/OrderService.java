package com.example.servicopedidos.service;

import com.example.servicopedidos.client.EventServiceClient;
import com.example.servicopedidos.config.RabbitMQConfig;
import com.example.servicopedidos.dto.OrderRequest;
import com.example.servicopedidos.event.OrderCreatedEvent;
import com.example.servicopedidos.event.PaymentProcessedEvent;
import com.example.servicopedidos.model.Order;
import com.example.servicopedidos.model.OrderItem;
import com.example.servicopedidos.model.OrderStatus;
import com.example.servicopedidos.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventServiceClient eventServiceClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order createOrderFromRequest(String userId, OrderRequest orderRequest) {
        // Create Order from OrderRequest
        Order order = new Order();
        order.setUserId(userId);

        // Create OrderItem
        OrderItem item = new OrderItem();
        item.setTicketTypeId(orderRequest.getTicketTypeId());
        item.setQuantity(orderRequest.getQuantity());

        // Set items list
        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setItems(items);

        // Call existing createOrder logic
        return createOrder(order);
    }

    @Transactional
    @CircuitBreaker(name = "eventService", fallbackMethod = "createOrderFallback")
    @Retry(name = "eventService")
    @TimeLimiter(name = "eventService")
    public Order createOrder(Order order) {
        // Validate ticket availability and calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            Integer availableQuantity = eventServiceClient.getAvailableQuantity(item.getTicketTypeId());
            if (availableQuantity == null || availableQuantity < item.getQuantity()) {
                throw new RuntimeException("Not enough tickets available for ticket type " + item.getTicketTypeId());
            }
            BigDecimal price = eventServiceClient.getTicketPrice(item.getTicketTypeId());
            if (price == null) {
                throw new RuntimeException("Could not determine price for ticket type " + item.getTicketTypeId());
            }
            item.setUnitPrice(price);
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Decrement ticket quantity
        for (OrderItem item : order.getItems()) {
            eventServiceClient.decrementTicketQuantity(item.getTicketTypeId(), item.getQuantity());
        }

        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.getItems().forEach(item -> item.setOrder(order));
        Order savedOrder = orderRepository.save(order);

        // Publish OrderCreatedEvent to RabbitMQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY, new OrderCreatedEvent(savedOrder.getId(), savedOrder.getUserId(), savedOrder.getTotalAmount()));

        return savedOrder;
    }

    public Order updateOrder(Long id, Order orderDetails) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setUserId(orderDetails.getUserId());
        order.setTotalAmount(orderDetails.getTotalAmount());
        order.setStatus(orderDetails.getStatus());
        order.setItems(orderDetails.getItems());
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_PROCESSED_QUEUE)
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received payment processed event for order ID: {}", event.getOrderId());
        OrderStatus newStatus = event.isSuccess() ? OrderStatus.PAID : OrderStatus.CANCELLED;
        updateOrderStatus(event.getOrderId(), newStatus);
    }

    /**
     * Fallback method for createOrder when circuit breaker is open or service is down
     */
    public Order createOrderFallback(Order order, Throwable throwable) {
        log.error("Circuit breaker activated for createOrder. Event service is unavailable. Error: {}",
                  throwable.getMessage());
        throw new RuntimeException("Event service is currently unavailable. Cannot process order at this time. Please try again later.");
    }
}
