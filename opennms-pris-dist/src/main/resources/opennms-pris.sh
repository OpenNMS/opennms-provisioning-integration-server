#!/bin/sh

# Set debug on
# set -x

# Set Java home environment to use
JAVA_BIN="/usr/bin/java"

PROG="opennms-pris.jar"
PROG_DIR=`dirname $0`
PROG_MAIN="org.opennms.pris.Starter"

${JAVA_BIN} ${JAVA_OPTS} -cp ${PROG_DIR}/lib/*:${PROG_DIR}/${PROG} ${PROG_MAIN}
