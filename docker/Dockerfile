FROM openjdk:8-jre-alpine

LABEL maintainer "Ronny Trommer <ronny@opennms.org>"

ARG OPENNMS_PRIS_VERSION=SNAPSHOT
ARG OPENNMS_PRIS_HOME=/opt/opennms-pris
ENV JAVA_OPTS ""

ADD deploy/pris-release-archive.tar.gz /opt

RUN apk add --no-cache curl

## Volumes for storing data outside of the container
VOLUME [ "${OPENNMS_PRIS_HOME}/requisitions", "${OPENNMS_PRIS_HOME}/scriptsteps" ]

WORKDIR ${OPENNMS_PRIS_HOME}

LABEL license="AGPLv3" \
      org.opennms.pris.version="${OPENNMS_PRIS_VERSION}" \
      vendor="OpenNMS Community" \
      name="PRIS"

ENTRYPOINT ./opennms-pris.sh

EXPOSE 8000
