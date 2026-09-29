#### ============================================================
#### KAFKA ARM64 IMAGE
#### ============================================================
#
#### PURPOSE
#### -------
#### Build, validate, publish, and verify an ARM64 Kafka image.
#
#### This procedure is independent of the application project
#### structure and can be reused whenever this image needs to be
#### created or revalidated.
#
#### USE THIS PROCEDURE WHEN
#### ----------------------
#### - Creating the ARM64 Kafka image for the first time.
#### - Rebuilding the image after a relevant image/configuration change.
#### - Revalidating the image on another ARM64 environment.
#
#### DO NOT USE THIS PROCEDURE WHEN
#### ------------------------------
#### - You only need to start an already-published Kafka image.
####   In that case, use the project's Docker Compose/runtime configuration.
#
#### - You are testing the application itself.
####   In that case, use the application/integration test procedure.
#
#### LOCAL IMAGE
#### -----------
#### multiarch-lab/kafka:3.9.2
#
#### DOCKER HUB IMAGE
#### ----------------
#### theencodedbong/kafka:3.9.2
#
#### ============================================================


#### 1. BUILD THE IMAGE LOCALLY
#### --------------------------
#### Purpose:
#### Create the ARM64 Kafka image locally.
#
#### --platform linux/arm64 explicitly ensures that this procedure
#### creates an ARM64 image rather than relying on the host default.
#
#### NOTE:
#### The final build context/Dockerfile location is intentionally
#### left to the environment/project using this procedure.

docker build \
--platform linux/arm64 \
-t multiarch-lab/kafka:3.9.2 \
<dockerfile-build-context>


#### 2. VERIFY THE IMAGE ARCHITECTURE
#### --------------------------------
#### Purpose:
#### Confirm that the locally created image is actually ARM64 before
#### running or publishing it.
#
#### Expected result:
#### linux/arm64

docker image inspect multiarch-lab/kafka:3.9.2 \
--format '{{.Os}}/{{.Architecture}}'


#### 3. RUN THE KAFKA CONTAINER
#### --------------------------
#### Purpose:
#### Start a temporary container from the locally built image so that
#### the Kafka runtime can be validated independently of the application.

docker run -d \
--name kafka-test \
-p 9092:9092 \
multiarch-lab/kafka:3.9.2


#### 4. VERIFY THE CONTAINER IS RUNNING
#### ----------------------------------
#### Purpose:
#### Confirm that Docker successfully started the Kafka container.

docker ps


#### 5. VERIFY KAFKA STARTUP
#### -----------------------
#### Purpose:
#### Confirm from the container logs that Kafka has successfully started
#### and reached its expected running/ready state.
#
#### The exact log messages may vary between Kafka versions.
#### The important result is that startup completes without fatal errors
#### and the broker reaches a running state.

docker logs kafka-test --tail 30


#### 6. REMOVE THE TEMPORARY TEST CONTAINER
#### --------------------------------------
#### Purpose:
#### Remove the temporary validation container.
#
#### The image itself is retained because it will be tagged and published.

docker rm -f kafka-test


#### 7. TAG THE VALIDATED IMAGE FOR DOCKER HUB
#### -----------------------------------------
#### Purpose:
#### Create the Docker Hub tag from the locally validated image.

docker tag \
multiarch-lab/kafka:3.9.2 \
theencodedbong/kafka:3.9.2


#### 8. VERIFY THE DOCKER HUB TAG LOCALLY
#### ------------------------------------
#### Purpose:
#### Confirm that the Docker Hub tag exists locally before pushing it.

docker images theencodedbong/kafka


#### 9. PUSH THE IMAGE TO DOCKER HUB
#### -------------------------------
#### Purpose:
#### Publish the validated ARM64 image to Docker Hub.

docker push theencodedbong/kafka:3.9.2


#### 10. VERIFY THE PUBLISHED REGISTRY MANIFEST
#### ------------------------------------------
#### Purpose:
#### Verify what architecture(s) are represented by the published tag.
#
#### Expected result for this ARM64 phase:
#### linux/arm64

docker buildx imagetools inspect theencodedbong/kafka:3.9.2