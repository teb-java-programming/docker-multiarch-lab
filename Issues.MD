PHASE 4 — ISSUES ENCOUNTERED AND FIXES APPLIED
================================================

1. TOMCAT COULD NOT CONNECT TO ORACLE
-------------------------------------

Issue:
The Spring Boot application was initially using:

jdbc:oracle:thin:@localhost:1521/orclpdb1

When running inside the Tomcat container, "localhost" referred to the
Tomcat container itself, not the Oracle container.

Symptom:
The application failed to start because it could not connect to Oracle.

Fix:
Kept application.yml unchanged and supplied the Docker-specific datasource
URL through docker-compose.yml:

SPRING_DATASOURCE_URL: jdbc:oracle:thin:@oracle:1521/orclpdb1

"oracle" is the Docker Compose service name and is resolved through the
Compose network.


2. TOMCAT COULD NOT CONNECT TO KAFKA
------------------------------------

Issue:
The application was configured to bootstrap against:

kafka:29092

However, Kafka was advertising itself as:

PLAINTEXT://localhost:9092

Kafka clients use the broker's advertised address after the initial
bootstrap connection.

Symptom:
The Tomcat logs showed:

Connection to node 1 (localhost/127.0.0.1:9092) could not be established.

This was followed by:

TimeoutException: Expiring 1 record(s) for orders-0:120000 ms has
passed since batch creation

Cause:
"localhost" advertised by Kafka referred to the Tomcat container from the
application's point of view.


3. FIRST KAFKA ADVERTISED-LISTENER FIX BROKE KAFKA STARTUP
-----------------------------------------------------------

Issue:
The first attempt was to override only:

KAFKA_ADVERTISED_LISTENERS

Symptom:
Kafka then failed to start with:

Missing required configuration `zookeeper.connect`

Cause:
The Apache Kafka image uses KRaft configuration and its configuration
generation logic. Overriding only the advertised listeners resulted in
the required KRaft configuration not being preserved correctly.

Investigation:
The Kafka image was inspected and confirmed to be using KRaft:

process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093

Fix:
Configured the complete KRaft listener configuration instead of changing
only the advertised listener.

Final configuration:

KAFKA_PROCESS_ROLES: broker,controller
KAFKA_NODE_ID: 1
KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
KAFKA_LISTENERS: INTERNAL://:29092,EXTERNAL://:9092,CONTROLLER://:9093
KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:29092,EXTERNAL://localhost:9092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER

Result:

Container-to-container:
kafka:29092

Host-to-Kafka:
localhost:9092

Kafka controller:
localhost:9093

Kafka was subsequently verified to advertise:

advertised.listeners=INTERNAL://kafka:29092,EXTERNAL://localhost:9092


4. WIREMOCK HAD NO STUB MAPPING
-------------------------------

Issue:
The deployed application correctly called WireMock for:

GET /validation/customers/CUST-001/products/PROD-001

but WireMock had no matching stub.

Symptom:
The POST request returned HTTP 500.

WireMock returned:

404 Not Found:
No response could be served as there are no stub mappings in this
WireMock instance.

Fix:
Added a WireMock mapping for:

GET /validation/customers/CUST-001/products/PROD-001

The mapping returned HTTP 200 with:

{
"valid": true
}

Result:
The subsequent order request successfully passed external validation
and returned HTTP 201.


5. IBM MQ AUTHENTICATION FAILED WITH 2035
------------------------------------------

Issue:
The application was configured with:

Queue manager: QM1
Channel: DEV.APP.SVRCONN
User: app
Password: passw0rd

The MQ connection initially failed.

Symptom:

MQ reason code 2035

Cause:
The password for the MQ application user had not been configured in the
IBM MQ container.

Important:
"app" is the MQ user/principal. It is not the environment variable that
sets the password.

Fix:
Added to the IBM MQ Compose service:

LICENSE: accept
MQ_QMGR_NAME: QM1
MQ_APP_PASSWORD: passw0rd

Result:
The application was then able to connect to QM1 and publish messages to:

DEV.QUEUE.1


6. COMBINED TEST SUITE INTERMITTENTLY FAILED KAFKA EVENT ASSERTION
-----------------------------------------------------------------

Issue:
OrderIntegrationTest passed individually.

RunCucumberTest passed individually.

However, when the complete test suite was run together, the Kafka
OrderCreated event assertion intermittently failed.

Initial suspicion:
The failure appeared to suggest that Kafka publishing was unreliable.

Investigation:
Kafka was checked directly and the expected events were found in the
orders topic.

The actual problem was the test consumer's starting offset.

Fix:
The Kafka consumer used by the integration test was changed to:

properties.put(
ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
"earliest"
);

This allowed the test consumer to read records that already existed in
the topic.

Result:
The following runs all completed successfully:

mvn -pl service test
mvn -pl service test
mvn test
mvn test

The Kafka producer itself was not changed.


7. @DIRTIESCONTEXT DID NOT SOLVE THE TEST PROBLEM
-------------------------------------------------

Issue:
Because the combined test failure appeared to involve Spring test context
state, @DirtiesContext(AFTER_CLASS) was tried.

Result:
It did not solve the Kafka assertion problem and made the full-suite
behaviour worse.

Fix:
Removed @DirtiesContext.

Final state:
CucumberSpringConfiguration.java does NOT use @DirtiesContext.

The actual fix was the Kafka test consumer configuration:

AUTO_OFFSET_RESET_CONFIG = earliest


8. KAFKA VERIFICATION INITIALLY APPEARED TO SHOW NO EVENT
----------------------------------------------------------

Issue:
An initial attempt to grep/search the Kafka output produced no visible
result.

Rather than assuming that the producer had failed, Kafka itself was
inspected.

Verification performed:

Topic:
orders

Partition count:
1

Replication factor:
1

Leader:
1

Offset:

orders:0:1

The record was then read directly from partition 0 at offset 0.

The resulting event was:

{
"orderId": 391,
"customerId": "CUST-001",
"productId": "PROD-001",
"quantity": 2
}

Conclusion:
Kafka publishing was working.

The earlier verification command/output was the problem, not the Kafka
producer.


9. IBM MQ QUEUE CONTAINED MESSAGES FROM PREVIOUS TESTS
-------------------------------------------------------

Issue:
The MQ queue showed:

CURDEPTH(3)

This was because successful order tests had accumulated messages.

Messages were found for:

order 389
order 390
order 391

The current order 391 message was:

{
"orderId": 391,
"customerId": "CUST-001",
"productId": "PROD-001",
"quantity": 2
}

The message also showed:

UserIdentifier: 'app'
PutApplName: 'catalina.startup.Bootstrap'

This confirmed that the deployed Tomcat application had successfully
published the message.

Verification:
The IBM MQ sample utility:

/opt/mqm/samp/bin/amqsbcg

was used to inspect the queue.

Side effect:
The sample utility consumed the messages while performing the
verification, so the queue was subsequently emptied.

This was a verification side effect, not an application failure.


10. LOCALHOST ADDRESSES WERE NOT VALID INSIDE CONTAINERS
---------------------------------------------------------

Issue:
The same application.yml is used for local/integration testing and the
Docker deployment.

Local configuration uses localhost addresses such as:

Oracle:
localhost:1521

Kafka:
localhost:9092

IBM MQ:
localhost:1414

WireMock:
localhost:8081

Inside the Tomcat container, these addresses refer to Tomcat itself.

Fix:
application.yml was deliberately NOT changed.

Docker-specific values were supplied through docker-compose.yml:

SPRING_DATASOURCE_URL:
jdbc:oracle:thin:@oracle:1521/orclpdb1

SPRING_KAFKA_BOOTSTRAP_SERVERS:
kafka:29092

IBM_MQ_CONN_NAME:
ibm-mq(1414)

VALIDATION_SERVICE_BASE_URL:
http://wiremock:8080

This kept the local/test configuration separate from the Docker runtime
configuration.


11. WIREMOCK MAPPING IS RUNTIME STATE
-------------------------------------

Issue:
The WireMock validation mapping was added manually to the running
WireMock container during deployment testing.

Current consequence:
The mapping exists in the current running WireMock instance, but it is
not currently defined as part of the image itself or automatically
loaded from Compose.

Therefore, restarting/recreating the WireMock container will remove that
runtime mapping.

This was accepted for the Phase 4 deployment verification and was not
changed further during this phase.


FINAL PHASE 4 RESULT
====================

The following deployment path was successfully verified:

POST /service/api/orders
|
+-- WireMock validation
|
+-- Oracle persistence
|
+-- Kafka OrderCreated event
|
+-- IBM MQ OrderProcessing message
|
+-- HTTP 201

Successful deployed order:
orderId = 391

Kafka event for order 391 was verified.

IBM MQ message for order 391 was verified.

Oracle persistence was verified.

WireMock validation was verified.

A deployed GET had previously been verified successfully for order 390.

The deployed GET for order 391 was not separately executed during the
final verification sequence.