FROM opennms/openjdk:8u151-jdk

LABEL maintainer "Ronny Trommer <ronny@opennms.org>"

ARG OPENNMS_PRIS_VERSION=SNAPSHOT
ARG OPENNMS_PRIS_HOME=/opt/opennms-pris
ENV JAVA_OPTS ""

COPY ./deploy/pris-release-archive.tar.gz /tmp

RUN yum -y --setopt=tsflags=nodocs update && \
    mkdir -p ${OPENNMS_PRIS_HOME} && \
    tar xzf /tmp/pris-release-archive.tar.gz -C ${OPENNMS_PRIS_HOME} --strip-components=1 && \
    yum clean all && \
    rm -rf /tmp/*.tar.gz && \
    rm -rf /var/cache/yum

## Volumes for storing data outside of the container
VOLUME [ "${OPENNMS_PRIS_HOME}/requisitions", "${OPENNMS_PRIS_HOME}/scriptsteps" ]

WORKDIR ${OPENNMS_PRIS_HOME}

LABEL license="AGPLv3" \
      org.opennms.horizon.version="${OPENNMS_PRIS_VERSION}" \
      vendor="OpenNMS Community" \
      name="PRIS"

ENTRYPOINT ./opennms-pris.sh

EXPOSE 8000
