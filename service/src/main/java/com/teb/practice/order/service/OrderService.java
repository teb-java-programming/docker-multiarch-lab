package com.teb.practice.order.service;

import com.teb.practice.order.event.OrderCreatedEvent;
import com.teb.practice.order.event.OrderEventPublisher;
import com.teb.practice.order.messaging.OrderMessagePublisher;
import com.teb.practice.order.messaging.OrderProcessingMessage;
import com.teb.practice.order.model.Order;
import com.teb.practice.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderValidationClient validationClient;
    private final OrderEventPublisher eventPublisher;
    private final OrderMessagePublisher messagePublisher;

    public OrderService(
            OrderRepository orderRepository,
            OrderValidationClient validationClient,
            OrderEventPublisher eventPublisher,
            OrderMessagePublisher messagePublisher) {
        this.orderRepository = orderRepository;
        this.validationClient = validationClient;
        this.eventPublisher = eventPublisher;
        this.messagePublisher = messagePublisher;
    }

    public Order create(Order order) {
        validateRequest(order);

        if (!validationClient.validate(order.getCustomerId(), order.getProductId())) {
            throw new IllegalArgumentException("Order validation failed");
        }

        order.setStatus("CREATED");

        Order savedOrder = orderRepository.save(order);

        eventPublisher.publish(new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity()
        ));

        messagePublisher.publish(new OrderProcessingMessage(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity()
        ));

        return savedOrder;
    }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private void validateRequest(Order order) {
        if (order == null
                || order.getCustomerId() == null
                || order.getCustomerId().isBlank()
                || order.getProductId() == null
                || order.getProductId().isBlank()
                || order.getQuantity() == null
                || order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Invalid order request");
        }
    }
}