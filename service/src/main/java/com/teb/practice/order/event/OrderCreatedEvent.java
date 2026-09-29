package com.teb.practice.order.event;

public record OrderCreatedEvent(
        Long orderId,
        String customerId,
        String productId,
        Integer quantity
) {
}