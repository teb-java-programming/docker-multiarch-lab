package com.teb.practice.order.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class OrderCreationSteps {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    private String customerId;
    private String productId;
    private Integer quantity;
    private JsonNode responseBody;
    private int responseStatus;
    private long orderId;

    @Before
    public void resetTestState() throws Exception {
        jdbcTemplate.update("DELETE FROM ORDERS");

        HttpRequest resetRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8081/__admin/reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resetResponse =
                httpClient.send(resetRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resetResponse.statusCode());
    }

    @Given("a valid customer and product")
    public void aValidCustomerAndProduct() throws Exception {
        customerId = "CUST-001";
        productId = "PROD-001";
        quantity = 2;

        stubValidation(true);
    }

    @When("I submit an order for the product")
    public void iSubmitAnOrderForTheProduct() throws Exception {
        submitOrder();
    }

    @Then("the order should be created")
    public void theOrderShouldBeCreated() {
        assertEquals(201, responseStatus);
        assertNotNull(responseBody);
        assertEquals(orderId, responseBody.get("id").asLong());
        assertEquals(customerId, responseBody.get("customerId").asText());
        assertEquals(productId, responseBody.get("productId").asText());
        assertEquals(quantity, responseBody.get("quantity").asInt());
        assertEquals("CREATED", responseBody.get("status").asText());
    }

    @Then("the order should be persisted")
    public void theOrderShouldBePersisted() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM ORDERS
                        WHERE ID = ?
                          AND CUSTOMER_ID = ?
                          AND PRODUCT_ID = ?
                          AND QUANTITY = ?
                          AND STATUS = ?
                        """,
                Integer.class,
                orderId,
                customerId,
                productId,
                quantity,
                "CREATED"
        );

        assertEquals(1, count);
    }

    @Then("an OrderCreated event should be published")
    public void anOrderCreatedEventShouldBePublished() throws Exception {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-acceptance-verification-" + UUID.randomUUID()
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(Collections.singletonList("orders"));

            long deadline = System.currentTimeMillis() + 10_000;
            JsonNode matchingEvent = null;

            while (System.currentTimeMillis() < deadline
                    && matchingEvent == null) {

                for (ConsumerRecord<String, String> record :
                        consumer.poll(Duration.ofMillis(500))) {

                    JsonNode event = objectMapper.readTree(record.value());

                    if (event.has("orderId")
                            && event.get("orderId").asLong() == orderId) {
                        matchingEvent = event;
                        break;
                    }
                }
            }

            assertNotNull(matchingEvent);

            assertEquals(
                    orderId,
                    matchingEvent.get("orderId").asLong()
            );
            assertEquals(
                    customerId,
                    matchingEvent.get("customerId").asText()
            );
            assertEquals(
                    productId,
                    matchingEvent.get("productId").asText()
            );
            assertEquals(
                    quantity,
                    matchingEvent.get("quantity").asInt()
            );
        }
    }

    @Then("an order-processing message should be sent")
    public void anOrderProcessingMessageShouldBeSent() throws Exception {
        JsonNode message = receiveOrderProcessingMessage(orderId);

        assertNotNull(message);
        assertEquals(orderId, message.get("orderId").asLong());
        assertEquals(customerId, message.get("customerId").asText());
        assertEquals(productId, message.get("productId").asText());
        assertEquals(quantity, message.get("quantity").asInt());
    }

    @Given("an invalid order request")
    public void anInvalidOrderRequest() {
        customerId = "";
        productId = "";
        quantity = 0;
    }

    @When("I submit the order")
    public void iSubmitTheOrder() throws Exception {
        submitOrder();
    }

    @Then("the order should be rejected")
    public void theOrderShouldBeRejected() {
        assertEquals(400, responseStatus);
    }

    @Then("the order should not be persisted")
    public void theOrderShouldNotBePersisted() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ORDERS",
                Integer.class
        );

        assertEquals(0, count);
    }

    @Then("an OrderCreated event should not be published")
    public void anOrderCreatedEventShouldNotBePublished() throws Exception {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-acceptance-negative-" + UUID.randomUUID()
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(Collections.singletonList("orders"));

            long deadline = System.currentTimeMillis() + 2_000;
            boolean eventFound = false;

            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record :
                        consumer.poll(Duration.ofMillis(250))) {

                    JsonNode event = objectMapper.readTree(record.value());

                    if (event.has("orderId")
                            && event.get("orderId").asLong() == orderId) {
                        eventFound = true;
                        break;
                    }
                }

                if (eventFound) {
                    break;
                }
            }

            assertTrue(
                    !eventFound,
                    "OrderCreated event should not have been published"
            );
        }
    }

    @Then("an order-processing message should not be sent")
    public void anOrderProcessingMessageShouldNotBeSent() throws Exception {
        jmsTemplate.setReceiveTimeout(500);

        long deadline = System.currentTimeMillis() + 2_000;
        boolean messageFound = false;

        while (System.currentTimeMillis() < deadline) {
            Object message =
                    jmsTemplate.receiveAndConvert("DEV.QUEUE.1");

            if (message == null) {
                continue;
            }

            JsonNode json = objectMapper.readTree(message.toString());

            if (json.has("orderId")
                    && json.get("orderId").asLong() == orderId) {
                messageFound = true;
                break;
            }
        }

        assertTrue(
                !messageFound,
                "OrderProcessing message should not have been sent"
        );
    }

    @Given("the customer or product cannot be validated")
    public void theCustomerOrProductCannotBeValidated() throws Exception {
        customerId = "CUST-001";
        productId = "PROD-001";
        quantity = 2;

        stubValidation(false);
    }

    @Given("an existing order")
    public void anExistingOrder() throws Exception {
        customerId = "CUST-001";
        productId = "PROD-001";
        quantity = 2;

        stubValidation(true);
        submitOrder();

        assertEquals(201, responseStatus);
        assertTrue(orderId > 0);
    }

    @When("I request the order")
    public void iRequestTheOrder() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/api/orders/{id}", orderId)
                )
                .andReturn();

        responseStatus = result.getResponse().getStatus();

        String body = result.getResponse().getContentAsString();

        if (!body.isBlank()) {
            responseBody = objectMapper.readTree(body);
        }
    }

    @Then("the order should be returned")
    public void theOrderShouldBeReturned() {
        assertEquals(200, responseStatus);
        assertNotNull(responseBody);
        assertEquals(orderId, responseBody.get("id").asLong());
        assertEquals(customerId, responseBody.get("customerId").asText());
        assertEquals(productId, responseBody.get("productId").asText());
        assertEquals(quantity, responseBody.get("quantity").asInt());
        assertEquals("CREATED", responseBody.get("status").asText());
    }

    @Given("an order does not exist")
    public void anOrderDoesNotExist() {
        orderId = 999999L;
    }

    @Then("the order should not be found")
    public void theOrderShouldNotBeFound() {
        assertEquals(404, responseStatus);
    }

    private void submitOrder() throws Exception {
        orderId = 0;
        responseBody = null;

        String request = """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "quantity": %d
                }
                """.formatted(customerId, productId, quantity);

        MvcResult result = mockMvc.perform(
                        post("/api/orders")
                                .contentType("application/json")
                                .content(request)
                )
                .andReturn();

        responseStatus = result.getResponse().getStatus();

        String body = result.getResponse().getContentAsString();

        if (!body.isBlank()) {
            responseBody = objectMapper.readTree(body);

            if (responseBody.has("id")) {
                orderId = responseBody.get("id").asLong();
            }
        }
    }

    private void stubValidation(boolean valid) throws Exception {
        String mapping = """
                {
                  "request": {
                    "method": "GET",
                    "urlPath": "/validation/customers/%s/products/%s"
                  },
                  "response": {
                    "status": 200,
                    "headers": {
                      "Content-Type": "application/json"
                    },
                    "jsonBody": {
                      "valid": %s
                    }
                  }
                }
                """.formatted(customerId, productId, valid);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8081/__admin/mappings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapping))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
    }

    private JsonNode consumeOrderCreatedEvent(long expectedOrderId)
            throws Exception {

        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-acceptance-test-" + UUID.randomUUID()
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(Collections.singletonList("orders"));

            while (consumer.assignment().isEmpty()) {
                consumer.poll(Duration.ofMillis(100));
            }

            long deadline = System.currentTimeMillis() + 10_000;

            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record :
                        consumer.poll(Duration.ofMillis(500))) {

                    JsonNode event = objectMapper.readTree(record.value());

                    if (event.get("orderId").asLong() == expectedOrderId) {
                        return event;
                    }
                }
            }
        }

        return null;
    }

    private JsonNode receiveOrderProcessingMessage(long expectedOrderId)
            throws Exception {

        jmsTemplate.setReceiveTimeout(1000);

        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {
            Object message = jmsTemplate.receiveAndConvert("DEV.QUEUE.1");

            if (message == null) {
                continue;
            }

            JsonNode json = objectMapper.readTree(message.toString());

            if (json.get("orderId").asLong() == expectedOrderId) {
                return json;
            }
        }

        return null;
    }
}