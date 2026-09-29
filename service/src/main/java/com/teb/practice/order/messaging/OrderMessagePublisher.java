package com.teb.practice.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderMessagePublisher {

    private static final String QUEUE_NAME = "DEV.QUEUE.1";

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    public OrderMessagePublisher(
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(OrderProcessingMessage message) {
        try {
            jmsTemplate.convertAndSend(
                    QUEUE_NAME,
                    objectMapper.writeValueAsString(message)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish order message", e);
        }
    }
}