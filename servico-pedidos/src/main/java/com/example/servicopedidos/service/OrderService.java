package com.example.servicopedidos.service;

import com.example.servicopedidos.client.EventServiceClient;
import com.example.servicopedidos.config.RabbitMQConfig;
import com.example.servicopedidos.dto.OrderRequest;
import com.example.servicopedidos.event.OrderCreatedEvent;
import com.example.servicopedidos.event.PaymentProcessedEvent;
import com.example.servicopedidos.exception.BusinessException;
import com.example.servicopedidos.exception.InsufficientTicketsException;
import com.example.servicopedidos.exception.ResourceNotFoundException;
import feign.FeignException;
import com.example.servicopedidos.model.Order;
import com.example.servicopedidos.model.OrderItem;
import com.example.servicopedidos.model.OrderStatus;
import com.example.servicopedidos.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
    @CircuitBreaker(name = "eventService", fallbackMethod = "createOrderFallback")
    @Retry(name = "eventService")
    public Order createOrder(String userId, OrderRequest orderRequest) {
        Order order = new Order();
        order.setUserId(userId);

        OrderItem item = new OrderItem();
        item.setTicketTypeId(orderRequest.getTicketTypeId());
        item.setQuantity(orderRequest.getQuantity());

        List<OrderItem> items = new ArrayList<>();
        items.add(item);
        order.setItems(items);

        // Price is looked up up front; availability is no longer checked separately here -
        // the decrement call below is itself the atomic availability check (see events-service).
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem orderItem : order.getItems()) {
            BigDecimal price = eventServiceClient.getTicketPrice(orderItem.getTicketTypeId());
            if (price == null) {
                throw new BusinessException("Could not determine price for ticket type " + orderItem.getTicketTypeId());
            }
            orderItem.setUnitPrice(price);
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        List<OrderItem> reservedItems = new ArrayList<>();
        try {
            for (OrderItem orderItem : order.getItems()) {
                eventServiceClient.decrementTicketQuantity(orderItem.getTicketTypeId(), orderItem.getQuantity());
                reservedItems.add(orderItem);
            }

            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.getItems().forEach(orderItem -> orderItem.setOrder(order));
            Order savedOrder = orderRepository.save(order);

            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDERS_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    new OrderCreatedEvent(savedOrder.getId(), savedOrder.getUserId(), savedOrder.getTotalAmount()));

            return savedOrder;
        } catch (Exception ex) {
            // Best-effort compensation: this is not a full saga (no outbox, no guaranteed
            // retry of the release itself) - see README "Controle de concorrência" section.
            for (OrderItem reservedItem : reservedItems) {
                try {
                    eventServiceClient.releaseTickets(reservedItem.getTicketTypeId(), reservedItem.getQuantity());
                } catch (Exception releaseEx) {
                    log.error("Failed to release reserved tickets for ticket type {} after order creation failure",
                              reservedItem.getTicketTypeId(), releaseEx);
                }
            }
            throw ex;
        }
    }

    public Order updateOrder(Long id, Order orderDetails) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
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
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
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
    public Order createOrderFallback(String userId, OrderRequest orderRequest, Throwable throwable) {
        if (throwable instanceof FeignException.Conflict) {
            throw new InsufficientTicketsException(
                    "Not enough tickets available for ticket type " + orderRequest.getTicketTypeId());
        }
        if (throwable instanceof FeignException.NotFound) {
            throw new ResourceNotFoundException("TicketType", orderRequest.getTicketTypeId());
        }
        if (throwable instanceof BusinessException businessException) {
            throw businessException;
        }
        if (throwable instanceof ResourceNotFoundException resourceNotFoundException) {
            throw resourceNotFoundException;
        }
        log.error("Circuit breaker activated for createOrder. Event service is unavailable. Error: {}",
                  throwable.getMessage());
        throw new BusinessException("Event service is currently unavailable. Cannot process order at this time. Please try again later.");
    }
}
