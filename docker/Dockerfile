FROM openjdk:11.0.9-jre-slim

ARG OPENNMS_PRIS_VERSION=SNAPSHOT
ARG OPENNMS_PRIS_HOME=/opt/opennms-pris
ENV JAVA_OPTS ""

ADD deploy/opennms-pris-release-archive.tar.gz /opt

RUN apk add --no-cache curl

## Volumes for storing data outside of the container
VOLUME [ "${OPENNMS_PRIS_HOME}/requisitions", "${OPENNMS_PRIS_HOME}/scriptsteps" ]

WORKDIR ${OPENNMS_PRIS_HOME}

ARG BUILD_DATE="1970-01-01T00:00:00+0000"
ARG SOURCE
ARG REVISION
ARG BUILD_JOB_ID
ARG BUILD_NUMBER
ARG BUILD_URL
ARG BUILD_BRANCH
ARG JDK_VERSION
ARG VERSION

LABEL org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.title="OpenNMS Provisioning Integration Server ${OPENNMS_PRIS_VERSION}" \
      org.opencontainers.image.source="${SOURCE}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="The OpenNMS Group, Inc." \
      org.opencontainers.image.authors="OpenNMS Community" \
      org.opencontainers.image.licenses="AGPL-3.0" \
      org.opennms.image.base="${BASE_IMAGE}:${BASE_IMAGE_VERSION}" \
      org.opennme.cicd.jobid="${BUILD_JOB_ID}" \
      org.opennms.cicd.buildnumber="${BUILD_NUMBER}" \
      org.opennms.cicd.buildurl="${BUILD_URL}" \
      org.opennms.cicd.branch="${BUILD_BRANCH}"

ENTRYPOINT ./opennms-pris.sh

EXPOSE 8000
