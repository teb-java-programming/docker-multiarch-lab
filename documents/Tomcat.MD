#### ============================================================
#### TOMCAT ARM64 IMAGE
#### ============================================================
####
#### PURPOSE
#### -------
#### Build, validate, publish, and verify an ARM64 Tomcat image.
####
#### This procedure is independent of the application project
#### structure and can be reused whenever this image needs to be
#### created or revalidated.
####
#### USE THIS PROCEDURE WHEN
#### ----------------------
#### - Creating the ARM64 Tomcat image for the first time.
#### - Rebuilding the image after a relevant image/configuration change.
#### - Revalidating the image on another ARM64 environment.
####
#### DO NOT USE THIS PROCEDURE WHEN
#### ------------------------------
#### - You only need to start an already-published Tomcat image.
#### - You are testing the application itself.
####
#### LOCAL IMAGE
#### -----------
#### multiarch-lab/tomcat:10.1-jdk21
####
#### DOCKER HUB IMAGE
#### ----------------
#### theencodedbong/tomcat:10.1-jdk21
####
#### ============================================================


#### 1. BUILD THE IMAGE LOCALLY
#### --------------------------
#### Purpose:
#### Create the ARM64 Tomcat image locally.
####
#### --platform linux/arm64 explicitly ensures that this procedure
#### creates an ARM64 image rather than relying on the host default.
####
#### NOTE:
#### The final build context/Dockerfile location is intentionally
#### left to the environment/project using this procedure.

docker build \
--platform linux/arm64 \
-t multiarch-lab/tomcat:10.1-jdk21 \
<dockerfile-build-context>


#### 2. VERIFY THE IMAGE ARCHITECTURE
#### --------------------------------
#### Purpose:
#### Confirm that the locally created image is actually ARM64 before
#### spending time testing or publishing it.
####
#### Expected result:
#### linux/arm64

docker image inspect multiarch-lab/tomcat:10.1-jdk21 \
--format '{{.Os}}/{{.Architecture}}'


#### 3. RUN THE TOMCAT CONTAINER
#### ---------------------------
#### Purpose:
#### Start a temporary container from the locally built image so that
#### the Tomcat runtime can be validated independently of the application.

docker run -d \
--name tomcat-test \
-p 8080:8080 \
multiarch-lab/tomcat:10.1-jdk21


#### 4. VERIFY THE CONTAINER IS RUNNING
#### ----------------------------------
#### Purpose:
#### Confirm that Docker successfully started the Tomcat container.

docker ps


#### 5. VERIFY TOMCAT IS RESPONDING
#### ------------------------------
#### Purpose:
#### Confirm that Tomcat has started and is accepting HTTP requests.
####
#### Expected result:
#### HTTP/1.1 404
####
#### A 404 is expected because no application has been deployed yet.
#### The purpose of this check is to prove that the Tomcat server itself
#### is running and reachable.

curl -I http://localhost:8080


#### 6. REMOVE THE TEMPORARY TEST CONTAINER
#### --------------------------------------
#### Purpose:
#### Remove the temporary validation container.
####
#### The image itself is retained because it will be tagged and published.

docker rm -f tomcat-test


#### 7. TAG THE VALIDATED IMAGE FOR DOCKER HUB
#### -----------------------------------------
#### Purpose:
#### Create the Docker Hub tag from the locally validated image.
####
#### Local image:
####   multiarch-lab/tomcat:10.1-jdk21
####
#### Docker Hub image:
####   theencodedbong/tomcat:10.1-jdk21

docker tag \
multiarch-lab/tomcat:10.1-jdk21 \
theencodedbong/tomcat:10.1-jdk21


#### 8. VERIFY THE DOCKER HUB TAG LOCALLY
#### ------------------------------------
#### Purpose:
#### Confirm that the Docker Hub tag exists locally before pushing it.

docker images theencodedbong/tomcat


#### 9. PUSH THE IMAGE TO DOCKER HUB
#### -------------------------------
#### Purpose:
#### Publish the validated ARM64 image to Docker Hub.

docker push theencodedbong/tomcat:10.1-jdk21


#### 10. VERIFY THE PUBLISHED REGISTRY MANIFEST
#### ------------------------------------------
#### Purpose:
#### Verify what architecture(s) are actually represented by the
#### published Docker Hub tag.
####
#### Expected result for this ARM64 phase:
#### linux/arm64

docker buildx imagetools inspect theencodedbong/tomcat:10.1-jdk21