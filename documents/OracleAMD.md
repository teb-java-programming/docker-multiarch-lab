#### ============================================================
#### ORACLE DATABASE 19c AMD64 VALIDATION
#### ============================================================
#
#### PURPOSE
#### -------
#### Validate Oracle Database 19c Enterprise Edition on a genuine
#### AMD64 environment using Oracle's official pre-built container
#### image.
#
#### This is a validation procedure only.
#
#### The image is NOT yet published to our Docker Hub repository as
#### the AMD64 side of the final multi-architecture image.
#
#### ============================================================


#### 18. VERIFY ORACLE'S OFFICIAL CONTAINER REGISTRY IMAGE
#### -----------------------------------------------------
#### Purpose:
#### Confirm that Oracle provides the required 19c Enterprise Edition
#### image through its official Container Registry.

container-registry.oracle.com/database/enterprise:19.3.0.0


#### 19. LOG IN TO ORACLE CONTAINER REGISTRY
#### ---------------------------------------
#### Purpose:
#### Authenticate Docker against Oracle Container Registry.
#
#### Oracle requires authentication and acceptance of its applicable
#### Standard Terms and Restrictions.
#
#### Use an Oracle authentication token rather than the Oracle account
#### password.
#
#### Do NOT place the token in project files or source control.

docker login container-registry.oracle.com


#### 20. VERIFY ORACLE REGISTRY ACCESS
#### ---------------------------------
#### Purpose:
#### Confirm that the authenticated account can access the official
#### Oracle 19c Enterprise image.

docker manifest inspect \
container-registry.oracle.com/database/enterprise:19.3.0.0


#### 21. PULL THE OFFICIAL ORACLE IMAGE FOR AMD64
#### --------------------------------------------
#### Purpose:
#### Explicitly retrieve the Oracle image for the AMD64 architecture.

docker pull --platform linux/amd64 \
container-registry.oracle.com/database/enterprise:19.3.0.0


#### 22. VERIFY THE LOCAL IMAGE ARCHITECTURE
#### ---------------------------------------
#### Purpose:
#### Confirm that the pulled Oracle image is AMD64.

docker image inspect \
container-registry.oracle.com/database/enterprise:19.3.0.0 \
--format '{{.Architecture}}'


#### Expected:
#
#### amd64


#### NOTE
#### ----
#### The Mac/Colima environment is ARM64.
#
#### Therefore, running this AMD64 image locally on the Mac uses
#### emulation and is NOT considered the genuine AMD64 validation.
#
#### The actual AMD64 runtime validation is performed on GitHub
#### Actions' native x86_64 runner.


#### 23. VERIFY THE GITHUB ACTIONS AMD64 ENVIRONMENT
#### -----------------------------------------------
#### Purpose:
#### Confirm that the validation environment is genuinely AMD64.

uname -m

docker info --format '{{.Architecture}}'


#### Expected:
#
#### x86_64
#### amd64


#### 24. CONFIGURE GITHUB ACTIONS ORACLE REGISTRY SECRETS
#### ----------------------------------------------------
#### Purpose:
#### Allow GitHub Actions to authenticate against Oracle Container
#### Registry.
#
#### GitHub repository secrets:
#
#### ORACLE_REGISTRY_USERNAME
#### ORACLE_REGISTRY_TOKEN
#
#### The token must never be committed to the repository.


#### 25. PULL THE OFFICIAL ORACLE IMAGE ON THE AMD64 RUNNER
#### ------------------------------------------------------
#### Purpose:
#### Ensure the genuine AMD64 runner receives the AMD64 Oracle image.

docker pull \
--platform linux/amd64 \
container-registry.oracle.com/database/enterprise:19.3.0.0


#### 26. VERIFY THE ORACLE IMAGE ARCHITECTURE ON AMD64
#### -------------------------------------------------
#### Purpose:
#### Confirm the image running on the GitHub runner is AMD64.

docker image inspect \
container-registry.oracle.com/database/enterprise:19.3.0.0 \
--format '{{.Architecture}}'


#### Expected:
#
#### amd64


#### 27. START ORACLE ON THE GENUINE AMD64 RUNNER
#### ---------------------------------------------
#### Purpose:
#### Start the official Oracle 19c Enterprise image in a native AMD64
#### environment.

docker run -d \
--name oracle-official-amd64-test \
--platform linux/amd64 \
-e ORACLE_PWD=Oracl3124 \
-p 1521:1521 \
-p 5500:5500 \
container-registry.oracle.com/database/enterprise:19.3.0.0


#### 28. WAIT FOR ORACLE DATABASE READINESS
#### --------------------------------------
#### Purpose:
#### Wait until Oracle has completed database initialization.
#
#### The important readiness message is:
#
#### DATABASE IS READY TO USE
#
#### Do not treat a running container as sufficient proof that the
#### database is ready.


#### 29. VERIFY ORACLE VERSION AND EDITION
#### -------------------------------------
#### Purpose:
#### Execute SQL against the running Oracle instance.

docker exec \
-e ORACLE_SID=ORCLCDB \
oracle-official-amd64-test \
bash -c "printf '%s\n' 'select banner from v\$version;' | sqlplus -s / as sysdba"


#### 30. VERIFY ORCLPDB1
#### ------------------
#### Purpose:
#### Confirm that the application PDB exists and is open.

docker exec \
-e ORACLE_SID=ORCLCDB \
oracle-official-amd64-test \
bash -c "printf '%s\n' \"select name, open_mode from v\\\$pdbs where name = 'ORCLPDB1';\" | sqlplus -s / as sysdba"


#### Expected:
#
#### ORCLPDB1
#### READ WRITE


#### 31. VERIFY ORACLE LISTENER PORT
#### ------------------------------
#### Purpose:
#### Confirm that Oracle is accepting connections on its standard
#### listener port.

docker exec oracle-official-amd64-test \
bash -c "echo > /dev/tcp/127.0.0.1/1521"


#### 32. VERIFY SYSTEM LOGIN TO ORCLPDB1
#### -----------------------------------
#### Purpose:
#### Confirm that the SYSTEM account can authenticate against the
#### application PDB.

docker exec \
-e ORACLE_SID=ORCLCDB \
oracle-official-amd64-test \
bash -c "printf '%s\n' 'select sys_context('\''USERENV'\'','\''DB_NAME'\'') from dual;' | sqlplus -s system/Oracl3124@ORCLPDB1"


#### Expected:
#
#### ORCLPDB1


#### 33. BUILD THE ACTUAL SPRING BOOT SERVICE
#### -----------------------------------------
#### Purpose:
#### Build the real application WAR that will be used for the
#### application-level Oracle validation.

mvn -pl service clean package -DskipTests


#### 34. START THE ACTUAL SPRING BOOT APPLICATION
#### ----------------------------------------------
#### Purpose:
#### Run the real Order Processing Service against the AMD64 Oracle
#### instance.

nohup java -jar service/target/service.war \
> spring-boot.log 2>&1 &


#### 35. VERIFY THE APPLICATION CAN CONNECT TO ORACLE
#### -------------------------------------------------
#### Purpose:
#### Confirm that the actual application can establish its normal
#### Oracle connection and execute its database-backed GET operation.

curl -s -o /dev/null -w "%{http_code}" \
http://localhost:8080/api/orders/999999


#### Expected:
#
#### 404


#### IMPORTANT
#### ---------
#### The 404 is expected because order 999999 does not exist.
#
#### The important point is that the application successfully started,
#### connected to Oracle, executed the database lookup, and returned
#### the application's normal "not found" response.
#
#### This is stronger validation than merely checking that Oracle
#### itself is running.


#### 36. CLEAN UP THE AMD64 ORACLE TEST CONTAINER
#### ---------------------------------------------
#### Purpose:
#### Remove the temporary Oracle container after validation.
#
#### The official Oracle image itself is not modified.

docker rm -f oracle-official-amd64-test


#### ============================================================
#### ORACLE AMD64 VALIDATION STATUS
#### ============================================================
#
#### COMPLETED
#### ---------
#### ✓ Genuine AMD64 GitHub runner verified
#### ✓ Official Oracle 19c Enterprise image pulled
#### ✓ Oracle image verified as AMD64
#### ✓ Oracle database initialized successfully
#### ✓ ORCLPDB1 verified
#### ✓ ORCLPDB1 verified READ WRITE
#### ✓ SYSTEM login verified
#### ✓ Listener port 1521 verified
#### ✓ Actual Spring Boot application built
#### ✓ Actual Spring Boot application connected to Oracle
#### ✓ Application database-backed GET request verified
#
#### CURRENT STATUS
#### --------------
#### Oracle AMD64 validation is COMPLETE.
#
#### Oracle is now parked pending full Phase 6 stack validation:
#
#### Tomcat + Kafka + WireMock + Oracle + IBM MQ + Order Service
#
#### ============================================================
#### FINAL ORACLE DOCKER HUB STEP
#### ============================================================
#
#### The remaining Oracle publication task is to incorporate the
#### validated AMD64 Oracle side into:
#
#### theencodedbong/oracle:19.3.0
#
#### together with the existing ARM64 image.
#
#### This should be done AFTER full Phase 6 stack validation.
#
#### Until then:
#
#### ARM64:
#### theencodedbong/oracle:19.3.0
#### already exists on Docker Hub.
#
#### AMD64:
#### validated from Oracle's official Container Registry, but not yet
#### published into our Docker Hub multi-architecture tag.
#
#### ============================================================