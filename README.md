
# OpenNMS Provisioning Integration Server

The _Provisioning Integration Server (pris)_ is a tool which provides the ability to get external information from your inventory into an OpenNMS requisition model.
The output from pris is provided as XML over HTTP and can be used in OpenNMS Provisiond to import and discover nodes from.
This tool can be used to normalize inventory data and gives the ability to cleanup and manipulate the information before uploading into OpenNMS.

The project is divided in the following Maven modules:

* `parent` This module ties all components together 
* `opennms-pris-api` generic programming interfaces for configuration, mapper and different data sources
* `opennms-pris-dist` module to assemble the compiled code into a runnable and distributable format
* `opennms-pris-docs` documentation of the application for user and developers 
* `opennms-pris-main` provisioning integration server itself
* `opennms-pris-model` The OpenNMS requisition model
* `opennms-pris-plugins` plugins which implement specific data sources such as XLS, scripts, JDBC or OCS Inventory

## General Project Information

* CI/CD Status: [![CircleCI](https://circleci.com/gh/OpenNMS/opennms-provisioning-integration-server.svg?style=svg)](https://circleci.com/gh/OpenNMS/opennms-provisioning-integration-server)
* Container Image Info: [![](https://images.microbadger.com/badges/version/opennms/pris.svg)](https://microbadger.com/images/opennms/pris "Get your own version badge on microbadger.com") [![](https://images.microbadger.com/badges/image/opennms/pris.svg)](https://microbadger.com/images/opennms/pris "Get your own image badge on microbadger.com") [![](https://images.microbadger.com/badges/license/opennms/pris.svg)](https://microbadger.com/images/opennms/pris "Get your own license badge on microbadger.com")
* CI/CD System: [CircleCI]
* Docker Container Image Repository: [DockerHub]
* Issue- and Bug-Tracking: [JIRA]
* Source code: [GitHub]
* Chat: [IRC] or [Web Chat]
* Maintainer: ronny@opennms.org
* Illustrations created in documentation with [yED]

## Run PRIS as a Docker Container

Docker Tags

* `bleeding` floating tag for a build from develop bleeding edge branch
* `latest` floating tag for a build from latest stable release
* to run a specific stable version see the [DockerHub Tag] section

Current releases of PRIS are published on [DockerHub].
You can download and start the container image with:

    docker run --name mypris --detach --publish 8000:8000 opennms/pris:latest

The container will be downloaded from DockerHub and is started in background with name `mypris`.
A port 8000 is published on your local machine which can be used with your browser.
The unique container is returned which identifies the running instance of your container.

If you want to embed your PRIS service in an existing Docker Compose service stack:

```
version: '2.3'

volumes:
  pris.data:
    driver: local

services:
  pris:
    container_name: opennms.pris
    image: opennms/pris:latest
    environment:
      - TZ=Europe/Berlin
      - JAVA_OPTS=-XX:+PrintGCDetails -XX:+UnlockExperimentalVMOptions
    volumes:
      - pris.data:/opt/opennms-pris/requisitions
      - pris.data:/opt/opennms-pris/scriptsteps
    healthcheck:
      test: ["CMD", "curl", "-f", "-I", "http://localhost:8000/documentation/index.html"]
      interval: 30s
      timeout: 5s
      retries: 1
    ports:
      - "8000:8000"
```   

Your configuration for data sources is persisted to a named volume.
Mount a local volume if you want to use scripts and requisition source configuration from your local system.

To add JMX monitoring you can add Java options as environment variable like:

```
- JAVA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=19110 -Dcom.sun.management.jmxremote.rmi.port=19111 -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=<my-docker-host-ip>
```

As `-Djava.rmi.server.hostname` you have to take the IP address of your Docker Host machine and make sure you expose both RMI ports in `-Dcom.sun.management.jmxremote.port=19110` and `-Dcom.sun.management.jmxremote.rmi.port=19111`.

# Compile and Install PRIS from source

This guide describes how you can checkout the source code from GitHub and how you can compile from source.
The following parts are required: 

* [OpenJDK] or [Oracle Java Development Kit] with javac Version 8
* Apache [Maven]
* [git-scm]
* `java`, `javac`, `git` and `mvn` should be in your search path
* Internet connection to download maven dependencies

In your source directory run the command

    mvn clean package

It make sure everything from previous builds is cleaned away.
Then is compiles the code and build everything as a runnable jar as well as .tar.gz and .zip file in the `opennms-pris-dist/target` directory.

The PRIS server is started in foreground with the following command executed in your PRIS directory:

    java -cp ./lib/*:./opennms-pris.jar org.opennms.pris.Starter 

Connect your browser to http://localhost:8000, if you don't point to any requisition from a source, you will be redirected to the documentation page.
The example requisition from a provided Excel sheet can be accessed with http://localhost:8000/requisitions/myServer and http://localhost:8000/requisitions/myRouter.

## Build a Docker Container Image

You can build a Docker Container Image by using the provided `Docker/Dockerfile`.

### Step 0:

Checkout the source code repository to a subdirectory name `pris` and change into the directory

```
git clone https://github.com/OpenNMS/opennms-provisioning-integration-server.git pris
cd pris
```

### Step 1:

Compile and assemble the source code to get the distributable tar.gz file

```
mvn clean package
```

### Step 2:

Build the image with the command:

```
cp opennms-pris-dist/target/opennms-pris-release-archive.tar.gz Docker/deploy
docker build -t mypris .
```

### Step 3:

Run the build container image with

```
docker run --name mypris --detach --publish 8000:8000 mypris
```

# Development and Releases

There are several other branches with the following naming convention:

* `master` branch for next release, tagged as `bleeding` on [DockerHub]
* `release-*` a specific stable release, tagged as `latest` and with specific version number from `git describe` as history on [DockerHub]

The branches above will be automatically published to [DockerHub].
All other branches are just built and tested.
You can download build artifacts like Docker images from your branch from [CircleCI].

The CI/CD workflows can be found in the `.circleci` directory.

[GitHub]: https://github.com/OpenNMS/opennms-provisioning-integration-server.git
[CircleCI]: https://circleci.com/gh/opennms/opennms-provisioning-integration-server
[DockerHub]: https://hub.docker.com/r/opennms/pris
[DockerHub Tag]: https://hub.docker.com/r/opennms/pris/tags/
[JIRA]: https://issues.opennms.org/projects/PRIS
[OpenJDK]: http://openjdk.java.net/
[Oracle Java Development Kit]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
[Maven]: http://maven.apache.org/
[git-scm]: http://git-scm.com/
[yED]: http://www.yworks.com/en/products_yed_about.html
[Web Chat]: https://chats.opennms.org/opennms-discuss
[IRC]: irc://freenode.org/#opennms

