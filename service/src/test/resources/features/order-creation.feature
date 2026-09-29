Feature: Order creation

  Scenario: Create a valid order
    Given a valid customer and product
    When I submit an order for the product
    Then the order should be created
    And the order should be persisted
    And an OrderCreated event should be published
    And an order-processing message should be sent

  Scenario: Reject an invalid order
    Given an invalid order request
    When I submit the order
    Then the order should be rejected
    And the order should not be persisted
    And an OrderCreated event should not be published
    And an order-processing message should not be sent

  Scenario: Reject an order when the external validation fails
    Given the customer or product cannot be validated
    When I submit the order
    Then the order should be rejected
    And the order should not be persisted
    And an OrderCreated event should not be published
    And an order-processing message should not be sent

  Scenario: Retrieve an existing order
    Given an existing order
    When I request the order
    Then the order should be returned

  Scenario: Retrieve a non-existent order
    Given an order does not exist
    When I request the order
    Then the order should not be found