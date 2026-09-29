#### ============================================================
#### IBM MQ 9.4.5.1 ARM64 IMAGE
#### ============================================================
####
#### PURPOSE
#### -------
#### Build, validate, publish, and verify an ARM64 IBM MQ image.
####
#### This procedure uses IBM's official mq-container repository
#### and is independent of the application project structure.
####
#### USE THIS PROCEDURE WHEN
#### ----------------------
#### - Creating the ARM64 IBM MQ image for the first time.
#### - Rebuilding the image after a relevant MQ/container change.
#### - Revalidating the image on another ARM64 environment.
####
#### DO NOT USE THIS PROCEDURE WHEN
#### ------------------------------
#### - You only need to start an already-published IBM MQ image.
#### - You are building the AMD64 image.
#### - You are testing the application itself.
####
#### IBM BUILD IMAGE
#### ---------------
#### ibm-mqadvanced-server-dev:9.4.5.1-arm64
####
#### DOCKER HUB IMAGE
#### ----------------
#### theencodedbong/ibm-mq:9.4.5.1
####
#### ============================================================


#### 1. CLONE IBM'S OFFICIAL MQ CONTAINER REPOSITORY
#### ------------------------------------------------
#### Purpose:
#### Obtain IBM's official container build source.
####
#### This repository is the build source for the IBM MQ Developer
#### Server image used by this procedure.

git clone https://github.com/ibm-messaging/mq-container.git


#### 2. ENTER THE MQ CONTAINER REPOSITORY
#### -----------------------------------
#### Purpose:
#### Move into the repository containing IBM's MQ container build files.

cd mq-container


#### 3. CHECK OUT THE REQUIRED IBM MQ VERSION
#### ----------------------------------------
#### Purpose:
#### Pin the build to IBM MQ 9.4.5.1 rather than building from the
#### moving master branch.

git checkout v9.4.5.1


#### 4. BUILD THE ARM64 IBM MQ DEVELOPER IMAGE
#### ------------------------------------------
#### Purpose:
#### Build IBM MQ Advanced for Developers 9.4.5.1 using IBM's
#### provided build procedure.
####
#### The build creates the ARM64 Developer Server image:
#### ibm-mqadvanced-server-dev:9.4.5.1-arm64

make build-devserver


#### 5. VERIFY THE IMAGE ARCHITECTURE
#### --------------------------------
#### Purpose:
#### Confirm that the locally created image is actually ARM64 before
#### spending time testing or publishing it.
####
#### Expected result:
#### linux/arm64

docker image inspect ibm-mqadvanced-server-dev:9.4.5.1-arm64 \
--format '{{.Os}}/{{.Architecture}}'


#### 6. RUN THE IBM MQ CONTAINER
#### ---------------------------
#### Purpose:
#### Start a temporary IBM MQ container so that the MQ runtime and
#### queue manager can be validated independently of the application.
####
#### Queue manager:
#### QM1
####
#### Port 1414:
#### IBM MQ listener
####
#### Port 9443:
#### IBM MQ web console / REST endpoint

docker run -d \
--name ibm-mq-test \
-e LICENSE=accept \
-e MQ_QMGR_NAME=QM1 \
-p 1414:1414 \
-p 9443:9443 \
ibm-mqadvanced-server-dev:9.4.5.1-arm64


#### 7. VERIFY THE CONTAINER IS RUNNING
#### ----------------------------------
#### Purpose:
#### Confirm that Docker successfully started the IBM MQ container.

docker ps


#### 8. CHECK IBM MQ STARTUP
#### -----------------------
#### Purpose:
#### Confirm that the IBM MQ web/runtime components have completed
#### startup successfully.

docker logs ibm-mq-test --tail 50


#### 9. VERIFY THE QUEUE MANAGER
#### ---------------------------
#### Purpose:
#### Confirm that the configured queue manager has started successfully.
####
#### Expected result:
#### QMNAME(QM1) STATUS(Running)

docker exec ibm-mq-test dspmq


#### 10. VERIFY THE MQ LISTENER
#### --------------------------
#### Purpose:
#### Confirm that the queue manager has MQ listeners configured and
#### that MQSC can successfully query them.
####
#### The command should complete without syntax errors.

docker exec ibm-mq-test sh -c 'echo "DISPLAY LISTENER(*)" | runmqsc QM1'


#### 11. VERIFY HOST-TO-MQ CONNECTIVITY
#### ----------------------------------
#### Purpose:
#### Confirm that the MQ listener is reachable from the Mac host.
####
#### Expected result:
#### Connection to localhost port 1414 ... succeeded!

nc -vz localhost 1414


#### 12. REMOVE THE TEMPORARY TEST CONTAINER
#### ---------------------------------------
#### Purpose:
#### Remove the temporary validation container.
####
#### The validated image itself is retained because it will be tagged
#### and published.

docker rm -f ibm-mq-test


#### 13. TAG THE VALIDATED IMAGE FOR DOCKER HUB
#### ------------------------------------------
#### Purpose:
#### Create the Docker Hub tag from the locally validated IBM MQ image.
####
#### IBM build image:
####   ibm-mqadvanced-server-dev:9.4.5.1-arm64
####
#### Docker Hub image:
####   theencodedbong/ibm-mq:9.4.5.1

docker tag \
ibm-mqadvanced-server-dev:9.4.5.1-arm64 \
theencodedbong/ibm-mq:9.4.5.1


#### 14. VERIFY THE DOCKER HUB TAG LOCALLY
#### -------------------------------------
#### Purpose:
#### Confirm that the Docker Hub tag exists locally before pushing it.

docker images theencodedbong/ibm-mq


#### 15. PUSH THE IMAGE TO DOCKER HUB
#### -------------------------------
#### Purpose:
#### Publish the validated ARM64 IBM MQ image to Docker Hub.

docker push theencodedbong/ibm-mq:9.4.5.1


#### 16. VERIFY THE PUBLISHED REGISTRY MANIFEST
#### ------------------------------------------
#### Purpose:
#### Verify what architecture(s) are actually represented by the
#### published Docker Hub tag.
####
#### Expected result for this ARM64 phase:
#### linux/arm64
####
#### An additional unknown/unknown attestation manifest may be present.
#### This is expected and does not represent another runtime architecture.

docker buildx imagetools inspect theencodedbong/ibm-mq:9.4.5.1