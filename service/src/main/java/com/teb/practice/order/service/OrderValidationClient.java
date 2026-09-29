package com.teb.practice.order.service;

import com.teb.practice.order.model.ValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OrderValidationClient {

    private final RestClient restClient;

    public OrderValidationClient(
            RestClient.Builder restClientBuilder,
            @Value("${validation-service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public boolean validate(String customerId, String productId) {
        try {
            ValidationResponse response = restClient.get()
                    .uri("/validation/customers/{customerId}/products/{productId}",
                            customerId, productId)
                    .retrieve()
                    .body(ValidationResponse.class);

            return response != null && response.valid();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            return false;
        }
    }
}