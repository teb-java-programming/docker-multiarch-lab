#### ============================================================
#### ORACLE DATABASE 19c ARM64 IMAGE
#### ============================================================
#
#### PURPOSE
#### -------
#### Build, validate, publish, and verify an ARM64 Oracle Database
#### 19c Enterprise Edition container image using Oracle's official
#### Docker build repository and build procedure.
#
#### This procedure is independent of the application project
#### structure.
#
#### Oracle's repository is used as the build source because Oracle
#### supplies the Dockerfile, build context, installation logic,
#### checksum handling, and buildContainerImage.sh utility.
#
#### USE THIS PROCEDURE WHEN
#### ----------------------
#### - Creating the ARM64 Oracle 19c image for the first time.
#### - Rebuilding the image after a relevant Oracle image/build change.
#### - Revalidating the image on another ARM64 environment.
#
#### DO NOT USE THIS PROCEDURE WHEN
#### ------------------------------
#### - You only need to start an already-published Oracle image.
####   In that case, use the project's Docker Compose/runtime configuration.
#
#### - You are testing the application itself.
####   In that case, use the application/integration test procedure.
#
#### LOCAL IMAGE
#### -----------
#### multiarch-lab/oracle:19.3.0
#
#### DOCKER HUB IMAGE
#### ----------------
#### theencodedbong/oracle:19.3.0
#
#### IMPORTANT
#### ---------
#### Oracle Database 19c Enterprise Edition is supported on ARM64.
#
#### The required ARM64 installation binary is:
#
#### LINUX.ARM64_1919000_db_home.zip
#
#### The ZIP must remain compressed. Oracle's build process extracts it
#### as required.
#
#### ============================================================


#### 1. CLONE ORACLE'S OFFICIAL DOCKER BUILD REPOSITORY
#### --------------------------------------------------
#### Purpose:
#### Obtain Oracle's official Dockerfiles, build context, scripts,
#### and supporting files.
#
#### Keep this repository outside the application project. It is the
#### source used to build the Oracle image; it is not part of our
#### application source tree.

git clone https://github.com/oracle/docker-images.git


#### 2. MOVE INTO ORACLE'S SINGLE INSTANCE DOCKERFILE DIRECTORY
#### -----------------------------------------------------------
#### Purpose:
#### Run the Oracle build procedure from the directory expected by
#### Oracle's buildContainerImage.sh utility.

cd docker-images/OracleDatabase/SingleInstance/dockerfiles


#### 3. VERIFY THE ORACLE 19c BUILD DIRECTORY
#### ----------------------------------------
#### Purpose:
#### Confirm that Oracle's supplied 19.3.0 build context exists before
#### adding the installation binary.

ls -la 19.3.0


#### 4. PLACE THE ARM64 ORACLE 19c INSTALLATION ZIP
#### ----------------------------------------------
#### Purpose:
#### Provide Oracle's build process with the ARM64 Oracle Database
#### installation media.
#
#### Required file:
#
#### LINUX.ARM64_1919000_db_home.zip
#
#### Place the file directly inside:
#
#### dockerfiles/19.3.0/
#
#### IMPORTANT:
#### Do NOT extract the ZIP.
#
#### Oracle's build process handles extraction itself.
#
#### The Oracle installation binary is subject to Oracle's licensing
#### and download terms, so obtain it through Oracle's official source.


#### 5. VERIFY THE INSTALLATION BINARY
#### ---------------------------------
#### Purpose:
#### Confirm that the expected ARM64 installation ZIP is present
#### before starting the potentially lengthy image build.

ls -lh 19.3.0/LINUX.ARM64_1919000_db_home.zip


#### 6. VERIFY THE OFFICIAL BUILD WRAPPER
#### ------------------------------------
#### Purpose:
#### Confirm that Oracle's supplied build utility is available.
#
#### We use Oracle's wrapper rather than creating our own build logic.
#
#### The wrapper performs checks such as installation-media validation
#### and then invokes the appropriate container build.

ls -lh buildContainerImage.sh


#### 7. BUILD THE ARM64 ORACLE IMAGE
#### -------------------------------
#### Purpose:
#### Build Oracle Database 19c Enterprise Edition as an ARM64 image.
#
#### -v 19.3.0
####     Selects Oracle Database 19c.
#
#### -e
####     Builds the Enterprise Edition image.
#
#### -t
####     Gives the resulting image our local image/tag name.
#
#### Oracle's script performs the actual Docker build using its supplied
#### Dockerfile and build context.

./buildContainerImage.sh \
-v 19.3.0 \
-e \
-t multiarch-lab/oracle:19.3.0


#### 8. VERIFY THE LOCAL IMAGE ARCHITECTURE
#### --------------------------------------
#### Purpose:
#### Confirm that the image produced by Oracle's build process is
#### actually ARM64 before starting the database.
#
#### Expected result:
#
#### linux/arm64

docker image inspect multiarch-lab/oracle:19.3.0 \
--format '{{.Os}}/{{.Architecture}}'


#### 9. RUN THE ORACLE DATABASE CONTAINER
#### ------------------------------------
#### Purpose:
#### Start a temporary Oracle Database container so the image can be
#### validated independently of the application.
#
#### Port mappings:
#
#### 1521:1521
####     Oracle's standard database listener.
####     This is the port our application will eventually use to
####     connect to Oracle.
#
#### 5500:5500
####     Oracle Enterprise Manager Express.
####     We expose it during validation so the Oracle runtime can be
####     tested through its documented management endpoint as well.
#
#### The ulimits below are part of Oracle's documented container
#### runtime requirements for Oracle Database 19.3+.
#
#### ORACLE_PWD:
####     Supplies the initial database password for this temporary
####     validation container.
#
#### Replace <oracle-test-password> with a temporary local test
#### password. Do not commit a real password to the project.
#
#### NOTE:
#### Database creation happens during the first container startup.
#### Therefore, "container is running" does NOT immediately mean
#### "database is ready".
#
#### We validate readiness in the next step.

docker run -d \
--name oracle-test \
-p 1521:1521 \
-p 5500:5500 \
--ulimit nofile=1024:65536 \
--ulimit nproc=2047:16384 \
--ulimit stack=10485760:33554432 \
--ulimit memlock=3221225472 \
-e ORACLE_PWD='<oracle-test-password>' \
multiarch-lab/oracle:19.3.0


#### 10. VERIFY THE CONTAINER IS RUNNING
#### -----------------------------------
#### Purpose:
#### Confirm that Docker successfully created and started the Oracle
#### container.
#
#### IMPORTANT:
#### A running container at this point does not yet prove that the
#### Oracle database has completed initialization.

docker ps


#### 11. VERIFY ORACLE DATABASE INITIALIZATION
#### -----------------------------------------
#### Purpose:
#### Watch the Oracle startup logs and wait for the database creation
#### process to complete.
#
#### Oracle's documented image reports when the database is ready for
#### use. Do not treat the container as successfully validated merely
#### because it appears in "docker ps".
#
#### The startup can take considerably longer than Tomcat or Kafka
#### because the initial Oracle database is created during first
#### startup.

docker logs -f oracle-test


#### 12. VERIFY DATABASE CONNECTIVITY
#### --------------------------------
#### Purpose:
#### Perform a real Oracle connection test from inside the running
#### container.
#
#### This goes one step beyond checking startup logs: it confirms that
#### the database is actually accepting SQL connections.
#
#### Use Ctrl+C after the connection test if the command remains
#### attached.

docker exec -it oracle-test \
sqlplus pdbadmin@ORCLPDB1


#### 13. REMOVE THE TEMPORARY TEST CONTAINER
#### ---------------------------------------
#### Purpose:
#### Remove the temporary validation container after Oracle has been
#### successfully tested.
#
#### The image itself is retained because it will be tagged and
#### published.

docker rm -f oracle-test


#### 14. TAG THE VALIDATED IMAGE FOR DOCKER HUB
#### ------------------------------------------
#### Purpose:
#### Create the Docker Hub tag from the locally validated image.

docker tag \
multiarch-lab/oracle:19.3.0 \
theencodedbong/oracle:19.3.0


#### 15. VERIFY THE DOCKER HUB TAG LOCALLY
#### -------------------------------------
#### Purpose:
#### Confirm that the Docker Hub tag exists locally before publishing.

docker images theencodedbong/oracle


#### 16. PUSH THE IMAGE TO DOCKER HUB
#### --------------------------------
#### Purpose:
#### Publish the validated ARM64 Oracle image.

docker push theencodedbong/oracle:19.3.0


#### 17. VERIFY THE PUBLISHED REGISTRY MANIFEST
#### ------------------------------------------
#### Purpose:
#### Confirm what architecture(s) are represented by the published
#### Docker Hub tag.
#
#### Expected result for this ARM64 phase:
#
#### linux/arm64

docker buildx imagetools inspect theencodedbong/oracle:19.3.0