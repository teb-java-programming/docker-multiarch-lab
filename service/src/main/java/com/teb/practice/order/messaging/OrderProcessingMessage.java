package com.teb.practice.order.messaging;

public record OrderProcessingMessage(
        Long orderId,
        String customerId,
        String productId,
        Integer quantity
) {
}