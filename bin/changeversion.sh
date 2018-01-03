#!/bin/bash
#
# Script changes the pom.xml files in the current project.
#
# Usage example:
#   changeversion.sh -o 1.0.4-SNAPSHOT -n 1.0.5-SNAPSHOT
#
# Created:
#   ronny@opennms.org
#

CWD=$(pwd)

# Turn on debug mode
# set -x

#
# Function print usage help text
#
printUsage() {
  echo ""
  echo "Script to change the version number in pom.xml files"
  echo "for the opennms-pris project"
  echo ""
  echo "  Usage: ${0} -o <old-version> -n <new-version>"
  echo "  Example: ${0} -o 1.0.4-SNAPSHOT -n 1.0.5-SNAPSHOT"
  echo ""
  exit 0
}

# Check if we have enough arguments
if [ ! $# -eq 4 ];then
    printUsage
fi

# Evaluate arguments
while [ $# -gt 0 ] ; do
    case "$1" in
    "-o")
        OLD_VERSION=$2
        shift 2
        ;;
    "-n")
        NEW_VERSION=$2
        shift 2
        ;;
    *)
        printUsage
        ;;
    esac
done

# Go through all pom.xml files and replace the given version number
for i in $(find ${CWD} -name "pom.xml"); do
  cat ${i} | sed -e "s/${OLD_VERSION}/${NEW_VERSION}/g" > ${i}.new;
  mv ${i}.new ${i};
done

# Replace version number in documentation config.toml file
for i in $(find ${CWD}/docs -name "config.toml"); do
  cat ${i} | sed -e "s/${OLD_VERSION}/${NEW_VERSION}/g" > ${i}.new;
  mv ${i}.new ${i};
done
