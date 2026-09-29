#### ============================================================
#### WIREMOCK ARM64 IMAGE
#### ============================================================
#
#### PURPOSE
#### -------
#### Build, validate, publish, and verify an ARM64 WireMock image.
#
#### The image is based on WireMock's official upstream image.
#### We create our own ARM64 image/tag so that it becomes part of
#### our controlled image set and can later be rebuilt for AMD64.
#
#### USE THIS PROCEDURE WHEN
#### ----------------------
#### - Creating the ARM64 WireMock image for the first time.
#### - Rebuilding the image after a relevant image/configuration change.
#### - Revalidating the image on another ARM64 environment.
#
#### DO NOT USE THIS PROCEDURE WHEN
#### ------------------------------
#### - You only need to start the already-published WireMock image.
####   In that case, use the project's Docker Compose/runtime configuration.
#
#### - You are testing the application itself.
####   In that case, use the application/integration test procedure.
#
#### LOCAL IMAGE
#### -----------
#### multiarch-lab/wiremock:3.13.2
#
#### DOCKER HUB IMAGE
#### ----------------
#### theencodedbong/wiremock:3.13.2
#
#### ============================================================


#### 1. CREATE THE WIREMOCK DOCKERFILE
#### ---------------------------------
#### Purpose:
#### Define our controlled WireMock image using the official
#### WireMock image as the upstream base.
#
#### No additional customization is required at this stage.

FROM wiremock/wiremock:3.13.2


#### 2. BUILD THE IMAGE LOCALLY
#### --------------------------
#### Purpose:
#### Create the ARM64 WireMock image locally.
#
#### --platform linux/arm64 explicitly ensures that this build
#### produces an ARM64 image.

docker build \
--platform linux/arm64 \
-t multiarch-lab/wiremock:3.13.2 \
<dockerfile-build-context>


#### 3. VERIFY THE IMAGE ARCHITECTURE
#### --------------------------------
#### Purpose:
#### Confirm that the locally created image is ARM64 before
#### running or publishing it.
#
#### Expected result:
#### linux/arm64

docker image inspect multiarch-lab/wiremock:3.13.2 \
--format '{{.Os}}/{{.Architecture}}'


#### 4. RUN THE WIREMOCK CONTAINER
#### -----------------------------
#### Purpose:
#### Start a temporary container so the WireMock runtime can be
#### validated independently of the application.

docker run -d \
--name wiremock-test \
-p 8080:8080 \
multiarch-lab/wiremock:3.13.2


#### 5. VERIFY THE CONTAINER IS RUNNING
#### ----------------------------------
#### Purpose:
#### Confirm that Docker successfully started the WireMock container.

docker ps


#### 6. VERIFY WIREMOCK IS RESPONDING
#### --------------------------------
#### Purpose:
#### Confirm that WireMock has started and is accepting HTTP requests.
#
#### WireMock's administrative API is available on port 8080.

curl -I http://localhost:8080/__admin/


#### 7. REMOVE THE TEMPORARY TEST CONTAINER
#### --------------------------------------
#### Purpose:
#### Remove the temporary validation container.
#
#### The image itself is retained because it will be tagged and
#### published.

docker rm -f wiremock-test


#### 8. TAG THE VALIDATED IMAGE FOR DOCKER HUB
#### -----------------------------------------
#### Purpose:
#### Create the Docker Hub tag from the locally validated image.

docker tag \
multiarch-lab/wiremock:3.13.2 \
theencodedbong/wiremock:3.13.2


#### 9. VERIFY THE DOCKER HUB TAG LOCALLY
#### ------------------------------------
#### Purpose:
#### Confirm that the Docker Hub tag exists locally before pushing it.

docker images theencodedbong/wiremock


#### 10. PUSH THE IMAGE TO DOCKER HUB
#### --------------------------------
#### Purpose:
#### Publish the validated ARM64 WireMock image.

docker push theencodedbong/wiremock:3.13.2


#### 11. VERIFY THE PUBLISHED REGISTRY MANIFEST
#### ------------------------------------------
#### Purpose:
#### Verify what architecture(s) are represented by the published
#### Docker Hub tag.
#
#### Expected result for this ARM64 phase:
#### linux/arm64

docker buildx imagetools inspect theencodedbong/wiremock:3.13.2