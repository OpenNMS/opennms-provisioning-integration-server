# OpenNMS Provisioning Integration Server (PRIS)

[![CI](https://github.com/OpenNMS/opennms-provisioning-integration-server/actions/workflows/ci.yml/badge.svg)](https://github.com/OpenNMS/opennms-provisioning-integration-server/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/OpenNMS/opennms-provisioning-integration-server?sort=semver)](https://github.com/OpenNMS/opennms-provisioning-integration-server/releases)
[![Container image](https://img.shields.io/badge/docker.io-opennms%2Fpris-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/r/opennms/pris)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

PRIS gets external information from your inventory into an OpenNMS requisition
model. It serves the result as XML over HTTP so OpenNMS Provisiond can import
and discover nodes from it, and lets you normalize, clean up and manipulate the
inventory data before it reaches OpenNMS.

## Contents

- [Quick start (container)](#quick-start-container)
- [Documentation](#documentation)
- [Build from source](#build-from-source)
- [Build a container image locally](#build-a-container-image-locally)
- [Project layout](#project-layout)
- [Contributing and releasing](#contributing-and-releasing)
- [Project information](#project-information)
- [License](#license)

## Quick start (container)

Released images are published to [Docker Hub]. Start the
latest stable release with:

```sh
docker run --name mypris --detach --publish 8000:8000 opennms/pris:latest
```

Then open <http://localhost:8000>. If no requisition source is configured yet
you are redirected to the documentation page.

Image tags:

- `latest` — floating tag tracking the most recent stable release
- `<version>` — a specific release (see the [Docker Hub repository][Docker Hub] for available tags)

### Docker Compose

```yaml
volumes:
  pris.data:
    driver: local

services:
  pris:
    container_name: opennms.pris
    image: opennms/pris:latest
    environment:
      - TZ=Europe/Berlin
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

Data source configuration is persisted to the named volume; mount a local
volume instead if you want to manage scripts and requisition sources from your
host.

To enable remote JMX monitoring, pass the options via `JAVA_OPTS`, using your
Docker host IP for `java.rmi.server.hostname` and exposing both RMI ports:

```yaml
environment:
  - JAVA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=19110 -Dcom.sun.management.jmxremote.rmi.port=19111 -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=<my-docker-host-ip>
ports:
  - "19110:19110"
  - "19111:19111"
```

## Documentation

Full user and developer documentation is published at [docs.opennms.com][Documentation].

Two example requisitions ship with the app and are reachable once it is running:
<http://localhost:8000/requisitions/myServer> and
<http://localhost:8000/requisitions/myRouter>.

## Build from source

Prerequisites (all must be on your `PATH`):

- [OpenJDK] or another JDK 17 (`java`, `javac`)
- Apache [Maven] (`mvn`)
- [git][git-scm] and `make`
- An internet connection to download Maven dependencies
- [Antora] to build the docs (`antora`); alternatively `make docs-docker` builds them in a container

The `Makefile` is the front door for every build task — list them with:

```sh
make help
```

Common targets:

| Target | Description |
| --- | --- |
| `make compile` | Compile and run the test suite |
| `make package` | Build the runnable jar and the `.tar.gz` / `.zip` release archives in `opennms-pris-dist/target` |
| `make run` | Build from source and start PRIS in the foreground on <http://localhost:8000> |
| `make docs` | Build the Antora documentation |
| `make all` | Compile, package and build the docs |
| `make clean` | Remove build artifacts and the local container image |

The quickest way to build and run from source is:

```sh
make run
```

To run a packaged build manually, extract a release archive and start the
server from its directory:

```sh
java -cp './lib/*:./opennms-pris.jar' org.opennms.pris.Starter
```

## Build a container image locally

```sh
make oci
```

This packages the release archive (if missing) and builds an image tagged
`pris:$(cat version.txt)` for your host platform. Override the name and tag with
`make oci IMAGE=mypris VERSION=1.2.3`, then run it:

```sh
docker run --name mypris --detach --publish 8000:8000 "pris:$(cat version.txt)"
```

Multi-arch (amd64/arm64) images are built and published only by the release
workflow — see [RELEASING.md](RELEASING.md).

## Project layout

Maven multi-module project:

| Module | Purpose |
| --- | --- |
| `opennms-pris-api` | Programming interfaces for configuration, mappers and data sources |
| `opennms-pris-model` | The OpenNMS requisition model |
| `opennms-pris-main` | The provisioning integration server itself |
| `opennms-pris-plugins` | Data-source plugins: XLS, script, JDBC, OCS Inventory and defaults |
| `opennms-pris-dist` | Assembles the compiled code into a runnable, distributable archive |

User and developer documentation lives under `docs/` and is built with [Antora].

## Contributing and releasing

- CI/CD workflows live in [`.github/workflows`](.github/workflows); every push
  and pull request is built, packaged and smoke tested.
- The release process (tag-driven, publishing container images and archives) is
  documented in **[RELEASING.md](RELEASING.md)**.

## Project information

- CI/CD system: [GitHub Actions]
- Container images: [Docker Hub]
- Issue and bug tracking: [GitHub Issues]
- Source code: [GitHub]
- Chat: [Web Chat]
- Maintainer: ronny@opennms.org

## License

Distributed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for
the full text.

[GitHub]: https://github.com/OpenNMS/opennms-provisioning-integration-server
[GitHub Actions]: https://github.com/OpenNMS/opennms-provisioning-integration-server/actions
[Docker Hub]: https://hub.docker.com/r/opennms/pris
[GitHub Issues]: https://github.com/OpenNMS/opennms-provisioning-integration-server/issues
[OpenJDK]: https://openjdk.org/
[Maven]: https://maven.apache.org/
[git-scm]: https://git-scm.com/
[Web Chat]: https://chats.opennms.org/opennms-discuss
[Documentation]: https://docs.opennms.com
[Antora]: https://docs.antora.org/antora/latest/install/install-antora/
