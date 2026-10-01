============================================================
AMD64 / x86_64 PHASE SUMMARY
docker-multiarch-lab
============================================================

STATUS
------------------------------------------------------------
AMD64 work is COMPLETE.

We validated all 5 infrastructure images on a genuine
linux/amd64 GitHub Actions runner and then merged the AMD64
images with the existing ARM64 images into the final
multi-architecture Docker Hub tags.

Final full-stack validation was also completed successfully
using the FINAL multi-architecture tags.


============================================================
1. WHY WE NEEDED AMD64
   ============================================================

Original environment:
- MacBook M4
- ARM64 / aarch64
- Colima
- Docker runtime
- ARM64 images could be built and tested locally

Problem:
- ARM64 testing alone does not prove AMD64 compatibility.
- We specifically wanted genuine x86_64 execution.
- We did NOT want:
    - x86 emulation on the M4
    - an x86 Linux VM
    - a separate physical Linux machine
    - AWS/GCP paid infrastructure

Decision:
- Use GitHub Actions' standard Ubuntu runner.
- GitHub's public runner provides genuine x86_64 hardware.
- This gave us a real AMD64 execution environment without
  needing another physical machine.


============================================================
2. GITHUB ACTIONS RUNNER
   ============================================================

Environment used:
- GitHub Actions
- ubuntu-latest
- Architecture: x86_64
- Docker architecture: x86_64
- CPU: 4
- Memory: approximately 15 GiB
- Disk: approximately 145 GB
- Docker: 28.0.4

Important distinction:
- This was NOT ARM64 emulation.
- Containers actually executed as linux/amd64.
- Therefore the runtime results are meaningful AMD64 results.


============================================================
3. TOMCAT AMD64
   ============================================================

Image:
theencodedbong/tomcat:10.1-jdk21

AMD64 process:
- Used official upstream:
  tomcat:10.1-jdk21
- Built an AMD64-specific image.
- Copied:
  service/target/service.war
  into:
  /usr/local/tomcat/webapps/service.war
- Built using:
  platforms: linux/amd64
- Pushed temporary AMD64 tag:
  theencodedbong/tomcat:10.1-jdk21-amd64
- Inspected the published image.
- Validated it on the genuine AMD64 runner.
- Created the final multi-architecture manifest.
- Combined:
  ARM64 + AMD64
- Final temporary AMD64 tag was subsequently removed.

Final tag:
theencodedbong/tomcat:10.1-jdk21

Final platforms:
linux/arm64
linux/amd64

Important detail:
- Tomcat's Docker image itself is straightforward.
- The application WAR was built with Java 21 before the image
  was assembled.


============================================================
4. KAFKA AMD64
   ============================================================

Image:
theencodedbong/kafka:3.9.2

Initial discovery:
- bitnami/kafka:3.9.0 was rejected because the required
  manifest/platform availability was not suitable.
- We selected:
  apache/kafka:3.9.2

AMD64 process:
- Created AMD64-specific Dockerfile:
  FROM apache/kafka:3.9.2
- Built for:
  linux/amd64
- Pushed:
  theencodedbong/kafka:3.9.2-amd64
- Inspected the image architecture.
- Started Kafka natively on the AMD64 GitHub runner.
- Configured:
  broker + controller
  node ID
  controller quorum
  listeners
  advertised listeners
  replication factors
- Waited for Kafka to reach:
  Kafka Server started
- Validation passed.
- Created final multi-architecture manifest.
- Combined:
  ARM64 + AMD64
- Temporary AMD64 tag removed.

Final tag:
theencodedbong/kafka:3.9.2

Final platforms:
linux/arm64
linux/amd64

Additional validation:
- Final merged Kafka image was also pulled/run on the M4
  ARM64/Colima environment.
- Kafka started successfully there.


============================================================
5. ORACLE 19c AMD64
   ============================================================

Image:
theencodedbong/oracle:19.3.0

This was the most complicated image.

ARM64 side:
- Oracle 19c ARM64 binaries had already been used to create
  the ARM64 image.
- ARM64 image existed as:
  theencodedbong/oracle:19.3.0

AMD64 challenge:
- Oracle's X64 database ZIP was approximately 2.8 GB.
- Oracle's normal download path redirected to Oracle Identity
  Cloud Service authentication.
- GitHub Actions could not simply download it anonymously.
- Git LFS was not viable because the file exceeded GitHub's
  individual LFS file limit.
- GitHub Release assets were also unsuitable for this file.

Oracle Registry solution:
- Accepted Oracle Standard Terms and Restrictions.
- Used Oracle Container Registry:
  container-registry.oracle.com
- Official image:
  container-registry.oracle.com/database/enterprise:19.3.0.0
- Logged into Oracle Container Registry using GitHub secrets.
- Pulled the official Oracle image as:
  linux/amd64
- Verified the image architecture.

Native AMD64 validation:
- Started Oracle on GitHub's AMD64 runner.
- Waited for:
  DATABASE IS READY TO USE
- Verified Oracle version/edition.
- Verified:
  ORCLPDB1
- Verified:
  ORCLPDB1 READ WRITE
- Verified port 1521.
- Verified SYSTEM login to ORCLPDB1.
- Verified:
  select sys_context(...)
  returned:
  ORCLPDB1

Most importantly:
- Built the Spring Boot service.
- Started the actual application.
- Called the application endpoint.
- Application successfully connected to Oracle.
- Nonexistent order returned:
  HTTP 404

Therefore:
- This wasn't merely "Oracle container starts".
- We proved the actual application could connect to the
  AMD64 Oracle database.

Final merge:
- ARM64 Oracle image:
  theencodedbong/oracle:19.3.0
- AMD64 Oracle image:
  Oracle Container Registry enterprise:19.3.0.0
- Created final Docker Hub multi-architecture manifest.

Final tag:
theencodedbong/oracle:19.3.0

Final platforms:
linux/arm64
linux/amd64

Final manifest contained:
linux/arm64
linux/amd64
Oracle attestation metadata

Important caveat:
- The AMD64 Oracle binary used for the AMD64 side came from
  Oracle's official container registry rather than rebuilding
  the Oracle image from the X64 ZIP inside GitHub.


============================================================
6. WIREMOCK AMD64
   ============================================================

Image:
theencodedbong/wiremock:3.13.2

Existing Dockerfile:
FROM wiremock/wiremock:3.13.2

AMD64 process:
- Built the existing Dockerfile for:
  linux/amd64
- Loaded the image into the GitHub runner.
- Verified architecture.
- Started WireMock.
- Mounted:
  core/wiremock
  mappings into the container.
- Verified:
  /__admin/health
- Verified:
  /__admin/
- Tested valid customer/product mapping.
- Tested invalid customer/product mapping.
- Validation passed.

Then:
- Built and pushed:
  theencodedbong/wiremock:3.13.2-amd64
- Verified the published AMD64 image.
- Created final multi-architecture manifest.
- Combined:
  ARM64 + AMD64
- Temporary AMD64 tag removed.

Final tag:
theencodedbong/wiremock:3.13.2

Final platforms:
linux/arm64
linux/amd64


============================================================
7. IBM MQ AMD64
   ============================================================

Image:
theencodedbong/ibm-mq:9.4.5.1

Source:
github.com/ibm-messaging/mq-container

Version:
v9.4.5.1

AMD64 process:
- Cloned IBM MQ container repository.
- Checked out:
  v9.4.5.1
- Ran:
  make build-devserver

Important discovery:
- The build produces:
  ibm-mqadvanced-server-dev:9.4.5.1-amd64
- It does NOT produce:
  ibm-mqadvanced-server-dev:9.4.5.1

The first validation workflow therefore failed because it
looked for the wrong local tag.

We corrected the workflow to use:
ibm-mqadvanced-server-dev:9.4.5.1-amd64

AMD64 validation:
- Verified:
  linux/amd64
- Started IBM MQ.
- Queue manager:
  QM1
- Waited for:
  STATUS(Running)
- Verified QM1 was running.
- Verified listener on:
  1414
- IBM MQ logs showed:
  queue manager started
  listener started
  mqweb ready

Another small debugging point:
- Initial grep expected:
  STATUS(RUNNING)
- Actual output was:
  STATUS(Running)
- Corrected the check to match the actual IBM MQ output.

Publishing:
- Tagged:
  theencodedbong/ibm-mq:9.4.5.1-amd64
- Pushed to Docker Hub.
- Verified the published image.
- Created final multi-architecture manifest.
- Combined:
  ARM64 + AMD64
- Temporary AMD64 tag was later removed.

Final tag:
theencodedbong/ibm-mq:9.4.5.1

Final platforms:
linux/arm64
linux/amd64


============================================================
8. MULTI-ARCH MANIFEST STRATEGY
   ============================================================

For the images where we had separate ARM64 and AMD64 images,
we used Docker Buildx imagetools.

Conceptually:

    ARM64 image
          +
    AMD64 image
          |
          v
    Multi-architecture manifest
          |
          v
    One final Docker Hub tag

Example:

    theencodedbong/kafka:3.9.2
        |
        +-- linux/arm64
        |
        +-- linux/amd64

The same strategy was used for:
- Tomcat
- Kafka
- WireMock
- IBM MQ
- Oracle

This means Docker can select the correct image automatically
based on the platform.


============================================================
9. FINAL DOCKER HUB IMAGES
   ============================================================

FINAL TAGS:

    theencodedbong/tomcat:10.1-jdk21
        ARM64 + AMD64

    theencodedbong/kafka:3.9.2
        ARM64 + AMD64

    theencodedbong/oracle:19.3.0
        ARM64 + AMD64

    theencodedbong/wiremock:3.13.2
        ARM64 + AMD64

    theencodedbong/ibm-mq:9.4.5.1
        ARM64 + AMD64


============================================================
10. FULL AMD64 STACK VALIDATION
    ============================================================

After all five images had been merged into their FINAL
multi-architecture tags, we deliberately ran the complete
stack again.

Reason:
- Earlier AMD64 validation had used temporary AMD64 tags.
- We wanted to prove the FINAL Docker Hub tags actually worked
  when forced to linux/amd64.

GitHub runner:
genuine x86_64 / AMD64

Images:
theencodedbong/tomcat:10.1-jdk21
theencodedbong/kafka:3.9.2
theencodedbong/oracle:19.3.0
theencodedbong/wiremock:3.13.2
theencodedbong/ibm-mq:9.4.5.1

Each image was explicitly pulled as:
linux/amd64

Then:

    Oracle
       |
    Kafka
       |
    WireMock
       |
    IBM MQ
       |
    Tomcat
       |
    Spring Boot application

Application context:
/service

Validation included:

    POST valid order
        -> HTTP 201

    GET created order
        -> HTTP 200

    Invalid quantity
        -> HTTP 400

    Invalid customer/product
        -> HTTP 400

    Kafka
        -> OrderCreated event verified

    IBM MQ
        -> DEV.QUEUE.1 message verified

    Oracle
        -> persistence/application connection verified

Result:
COMPLETE SUCCESS


============================================================
11. WHY GITHUB ACTIONS WAS USEFUL
    ============================================================

PROS
------------------------------------------------------------

1. Genuine AMD64 execution
    - GitHub's standard runner is x86_64.
    - No ARM64-to-AMD64 emulation was involved.

2. No additional hardware
    - No physical Linux machine required.
    - No second laptop/server required.

3. No Linux VM on the M4
    - Avoided another layer of virtualization.
    - Avoided running x86 Linux through emulation.

4. No AWS/GCP infrastructure
    - No cloud VM management.
    - No hourly infrastructure cost for this work.

5. Clean environment
    - Each workflow starts from a fresh runner.
    - Reduced "works on my machine" contamination.

6. Repeatability
    - The exact AMD64 process is captured in YAML.
    - The workflow can be rerun later.

7. Good CI/CD foundation
    - The same mechanism can eventually build/validate images
      automatically after changes.

8. Easy architecture verification
    - uname
    - docker info
    - docker image inspect
    - buildx imagetools inspect

9. Excellent for multi-architecture work
    - ARM64 can remain local on the M4.
    - AMD64 can be validated remotely.
    - Docker manifests combine both.

10. Good isolation
    - Oracle credentials and Docker credentials remain in
      GitHub Secrets rather than project files.

11. Useful failure visibility
    - Logs are retained by the workflow.
    - Failed steps identify exactly where validation stopped.


============================================================
12. GITHUB ACTIONS CONS
    ============================================================

1. It is NOT the same as owning a Linux machine
    - We have a temporary CI runner.
    - We don't control the underlying host permanently.

2. Ephemeral environment
    - Runner disappears after the workflow.
    - Anything installed/built locally is lost.

3. More YAML
    - Things that would be a simple shell command on Linux
      become workflow steps.

4. More CI-specific debugging
    - Shell behavior matters.
    - GitHub Actions environment variables matter.
    - Step boundaries matter.
    - Secrets must be configured correctly.

5. Credentials require extra setup
    - Docker Hub secrets.
    - Oracle Registry secrets.
    - Local Linux might simply use:
      docker login
      once.

6. No persistent Docker environment
    - Images, containers and volumes disappear after the runner.
    - Persistent infrastructure testing is more awkward.

7. Network dependency
    - Pulling source repositories and images depends on
      GitHub/network availability.

8. Registry dependency
    - For this project, Docker Hub became part of the validation
      path.
    - Oracle Registry was also required for Oracle AMD64.

9. Runtime investigation is less interactive
    - On a real Linux machine, we can leave containers running,
      inspect them repeatedly and experiment.
    - GitHub runners are designed around workflow execution.

10. Time limits / CI constraints
    - Long-running infrastructure is less comfortable.
    - Oracle in particular requires substantial startup time.

11. Reproducing a manual experiment is less convenient
    - A Linux shell session can remain open for hours.
    - A GitHub workflow normally needs to encode the experiment
      into YAML.

12. Some failures are workflow failures rather than product
    failures
    - Example:
      STATUS(Running)
      versus:
      STATUS(RUNNING)
    - Example:
      /service/api/orders
      rather than:
      /api/orders
    - Example:
      IBM MQ local image tag included -amd64.


============================================================
13. WHAT WOULD BE DIFFERENT ON A PROPER LINUX MACHINE
    ============================================================

REAL LINUX MACHINE
------------------------------------------------------------

Typical flow:

    SSH / terminal
        |
        v
    docker login
        |
        v
    docker build
        |
        v
    docker run
        |
        v
    docker logs
        |
        v
    manual testing
        |
        v
    docker push

Advantages:

- Direct shell access.
- Persistent filesystem.
- Persistent Docker daemon.
- Containers can stay running.
- Easy to inspect volumes.
- Easy to inspect networks.
- Easy to attach to containers.
- Easy to repeatedly run commands.
- Easier interactive debugging.
- Less YAML.
- Fewer CI-specific variables.
- Easier experimentation.
- Better environment for deep infrastructure debugging.

For example, on Linux we could simply do:

    docker run ...
    docker exec ...
    docker logs -f ...
    docker inspect ...
    docker network inspect ...

and continue interacting with the same environment.


============================================================
14. GITHUB VS REAL LINUX
    ============================================================

AREA                    GITHUB ACTIONS       REAL LINUX
------------------------------------------------------------
AMD64 hardware          Yes                 Yes
Persistent machine      No                  Yes
Persistent Docker       No                  Yes
Interactive debugging   Limited             Excellent
Repeatability           Excellent           Depends on setup
Automation              Excellent           Good
CI integration          Excellent           Manual
YAML overhead            Higher              Low
Filesystem persistence  Temporary           Persistent
Long-running services   Awkward             Easy
Experimentation         Less convenient      Excellent
Isolation                Excellent           Depends
Clean environment       Excellent           Depends
Network dependency       Higher              Depends
Credential management   Secrets              Local credential store
Infrastructure control  Limited              Full
Cost                     Very low/free tier   Hardware/hosting cost
Extra hardware needed    No                  Yes
x86_64 validation       Yes                 Yes


============================================================
15. THE IMPORTANT DISTINCTION
    ============================================================

GitHub Actions was GOOD for answering:

    "Does this image actually run on AMD64?"

It was also GOOD for answering:

    "Can our entire application stack run on AMD64?"

It was LESS ideal for answering:

    "Can I spend an entire day interactively experimenting
     with this Linux Docker environment?"

A real Linux machine wins for the second type of work.

For our specific goal, however, GitHub gave us something
important:

    REAL AMD64 EXECUTION
        +
    CLEAN ENVIRONMENT
        +
    REPEATABLE WORKFLOW
        +
    NO EXTRA HARDWARE
        +
    NO PAID CLOUD VM


============================================================
16. WHAT WE ACTUALLY PROVED
    ============================================================

We did NOT merely build five AMD64 image manifests.

We proved:

    1. Tomcat runs on AMD64.
    2. Kafka runs on AMD64.
    3. Oracle 19c runs on AMD64.
    4. WireMock runs on AMD64.
    5. IBM MQ runs on AMD64.

Then we proved:

    6. Spring Boot runs against the AMD64 infrastructure.
    7. Oracle connectivity works.
    8. Kafka event publishing works.
    9. IBM MQ messaging works.
10. WireMock validation works.
11. Tomcat serves the application.
12. The complete application flow works on AMD64.
13. The FINAL multi-architecture Docker Hub tags work
    when explicitly resolved to AMD64.


============================================================
17. CURRENT STATE / CHECKPOINT
    ============================================================

ARM64:
Complete
Tested on:
MacBook M4
Colima
aarch64

AMD64:
Complete
Tested on:
GitHub Actions
ubuntu-latest
genuine x86_64

Multi-architecture:
Complete

Final Docker Hub images:
COMPLETE

Final AMD64 full-stack validation:
PASSED

Phase 6:
COMPLETE

============================================================
END OF AMD64 CHECKPOINT
============================================================