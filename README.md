
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

* CI/CD Status: [![CI](https://github.com/OpenNMS/opennms-provisioning-integration-server/actions/workflows/ci.yml/badge.svg)](https://github.com/OpenNMS/opennms-provisioning-integration-server/actions/workflows/ci.yml)
* CI/CD System: [GitHub Actions]
* Container Image Repository: [GitHub Container Registry]
* Issue- and Bug-Tracking: [JIRA]
* Source code: [GitHub]
* Chat: [IRC] or [Web Chat]
* Maintainer: ronny@opennms.org
* Illustrations created in documentation with [yED]
* [Documentation]

## Run PRIS as a Docker Container

Docker Tags

* `latest` floating tag for a build from latest stable release
* to run a specific stable version see the tags of the [GitHub Container Registry] package

Current releases of PRIS are published on the [GitHub Container Registry].
You can download and start the container image with:

    docker run --name mypris --detach --publish 8000:8000 ghcr.io/opennms/opennms-provisioning-integration-server:latest

The container will be downloaded from the GitHub Container Registry and is started in background with name `mypris`.
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
    image: ghcr.io/opennms/opennms-provisioning-integration-server:latest
    environment:
      - TZ=Europe/Berlin
      - JAVA_OPTS=-XX:+PrintGCDetails -XX:+UnlockExperimentalVMOptions
    volumes:
      - pris.data:/opt/opennms-pris/requisitions
      - pris.data:/opt/opennms-pris/scriptsteps
    healthcheck:
      test: ["CMD", "curl", "-f", "-I", "http://localhost:8000/index.html"]
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

* [OpenJDK] or [Oracle Java Development Kit] with javac Version 17
* Apache [Maven]
* [git-scm]
* `java`, `javac`, `git`, `make` and `mvn` should be in your search path
* Internet connection to download maven dependencies
* Documentation is build with [Antora] and requires to have `antora` in your search path, alternatively `make docs-docker` builds the docs with a Docker container

The Makefile is the front door for all build goals. Get an overview with

    make help

In your source directory run the command

    make all

It make sure everything from previous builds is cleaned away.
Then is compiles the code and build everything as a runnable jar as well as .tar.gz and .zip file in the `opennms-pris-dist/target` directory.

The PRIS server is started in foreground with the following command executed in your PRIS directory:

    java -cp ./lib/*:./opennms-pris.jar org.opennms.pris.Starter 

Connect your browser to http://localhost:8000, if you don't point to any requisition from a source, you will be redirected to the documentation page.
The example requisition from a provided Excel sheet can be accessed with http://localhost:8000/requisitions/myServer and http://localhost:8000/requisitions/myRouter.

## Build a Docker Container Image

You can build a local container image for your host platform with

```
make oci
```

It packages the release archive (if missing) and builds an image tagged `pris:$(cat version.txt)`.
The image name and tag can be overridden with `make oci IMAGE=mypris VERSION=1.2.3`.
Run the built container image with

```
docker run --name mypris --detach --publish 8000:8000 pris:$(cat version.txt)
```

`make clean` removes the build artifacts including the local container image.
Multi-arch images (amd64/arm64) are built and published by the release workflow in GitHub Actions.

# Development and Releases

Releases are made by pushing a git tag with the pattern `v<major>.<minor>.<patch>` (e.g. v1.2.1).
The tag name without the `v` prefix is used as the version number to be released.
Releases are published to the following places:

* Multi-arch (amd64/arm64) OCI container images to the [GitHub Container Registry] of the repository the release is cut from
* .tar.gz and .zip files to the GitHub releases of this repository

All other pushes and pull requests are just built, packaged and smoke tested.

Steps to make a release:

1. Set the new version number in docs and code artifacts

```
bin/changeversion.sh -o BLEEDING -n 1.2.1
```

2. Commit the changes and tag the release

```
git commit -m "release: version 1.2.1"
git tag -a v1.2.1 -m "Release 1.2.1"
git push origin v1.2.1
```

The CI/CD workflows can be found in the `.github/workflows` directory.
The release workflow needs no repository secrets, container images are pushed with the built-in workflow token.
The package created by the very first release is private, its visibility has to be flipped to public once in the GitHub package settings.

[GitHub]: https://github.com/OpenNMS/opennms-provisioning-integration-server.git
[GitHub Actions]: https://github.com/OpenNMS/opennms-provisioning-integration-server/actions
[GitHub Container Registry]: https://github.com/OpenNMS/opennms-provisioning-integration-server/pkgs/container/opennms-provisioning-integration-server
[JIRA]: https://issues.opennms.org/projects/PRIS
[OpenJDK]: http://openjdk.java.net/
[Oracle Java Development Kit]: http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html
[Maven]: http://maven.apache.org/
[git-scm]: http://git-scm.com/
[yED]: http://www.yworks.com/en/products_yed_about.html
[Web Chat]: https://chats.opennms.org/opennms-discuss
[Documentation]: https://docs.opennms.com
[Antora]: https://docs.antora.org/antora/2.3/install/install-antora/
