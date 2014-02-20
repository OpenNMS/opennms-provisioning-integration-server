#!/bin/bash
# Script to change version number
#
# Usage:
#   changeversion.sh -v 1.0.5-SNAPSHOT
# 
# Script changes the pom.xml files in the current project.

if [ ! $# -eq "2" ];then 
  echo ""
  echo "Script to change the version number in pom.xml files"
  echo "for the opennms-pris project"
  echo ""
  echo "  Usage: ${0} -v <new-version>"
  echo "  Example: ${0} -v 1.0.5-SNAPSHOT"
  echo ""
  exit 1
fi

PARENT_POM="../pom.xml"
PRIS_POM="../opennms-pris/pom.xml"
PRIS_DOCS_POM="../opennms-pris-docs/pom.xml"

find .. -name "pom.xml" -exec cat {} | sed -e 's/\<version\>.*\<\/version\>/\<version\>${2}\<\/version\>/g' > {}.new \;

