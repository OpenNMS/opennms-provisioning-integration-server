#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
# Copyright 2026 The OpenNMS Group, Inc.
#
# Smoke test a PRIS release archive as a standalone installation: extract it,
# start it with plain java and verify both operating modes.
#
# Pass 1 (default configuration): the documentation and the example
# requisitions are served and the configuration API is absent - proving a
# default installation behaves exactly as before.
#
# Pass 2 (configuration API enabled): an authenticated CRUD round-trip
# through /api/v1/config, a hand edit of the written properties file that
# must be visible on the next request, and rejection of unauthenticated
# requests.
#
# Usage: smoke-test-standalone.sh <release-archive.tar.gz>

set -euo pipefail

ARCHIVE="${1:?Usage: $0 <release-archive.tar.gz>}"

PORT_OFF=18001
PORT_ON=18002
TOKEN="standalone-smoke-token"
AUTH=(-H "Authorization: Bearer ${TOKEN}")

WORK_DIR="$(mktemp -d)"
JAVA_PID=""

cleanup() {
  status=$?
  if [ -n "${JAVA_PID}" ]; then
    kill "${JAVA_PID}" >/dev/null 2>&1 || true
    wait "${JAVA_PID}" 2>/dev/null || true
  fi
  if [ "${status}" -ne 0 ] && [ -f "${WORK_DIR}/pris.log" ]; then
    echo "Smoke test failed, server log:" >&2
    cat "${WORK_DIR}/pris.log" >&2
  fi
  rm -rf "${WORK_DIR}"
}
trap cleanup EXIT

start_pris() {
  # exec makes the subshell PID the java PID, so stop_pris kills the server
  # itself and not just a wrapper shell
  ( cd "${WORK_DIR}/opennms-pris" && \
    exec java "$@" -cp './lib/*:./opennms-pris.jar' org.opennms.pris.Starter \
      >> "${WORK_DIR}/pris.log" 2>&1 ) &
  JAVA_PID=$!
}

stop_pris() {
  kill "${JAVA_PID}" >/dev/null 2>&1 || true
  wait "${JAVA_PID}" 2>/dev/null || true
  JAVA_PID=""
}

wait_for_http() {
  port=$1
  for _ in {1..30}; do
    if curl -s -o /dev/null "http://127.0.0.1:${port}/"; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: PRIS did not serve HTTP on port ${port} within 30 seconds" >&2
  return 1
}

expect_status() {
  expected=$1
  shift
  actual="$(curl -s -o /dev/null -w '%{http_code}' "$@")"
  if [ "${actual}" != "${expected}" ]; then
    echo "ERROR: expected HTTP ${expected} but got ${actual} for: $*" >&2
    return 1
  fi
}

echo "Extract release archive to ${WORK_DIR} ..."
tar -xzf "${ARCHIVE}" -C "${WORK_DIR}"

echo "Pass 1: start PRIS with the default configuration ..."
start_pris -Dport=${PORT_OFF}
wait_for_http ${PORT_OFF}

echo "Smoke test against documentation ..."
curl -L -f -s "http://127.0.0.1:${PORT_OFF}/" | grep -q "location=\"pris/.*/index.html\""

echo "Smoke test against the example requisitions ..."
expect_status 200 "http://127.0.0.1:${PORT_OFF}/requisitions/myServer"
expect_status 200 "http://127.0.0.1:${PORT_OFF}/requisitions/myRouter"

echo "Smoke test that the configuration API is absent by default ..."
expect_status 404 "http://127.0.0.1:${PORT_OFF}/api/v1/config/global"

stop_pris

echo "Pass 2: start PRIS with the configuration API enabled ..."
start_pris -Dport=${PORT_ON} -Dconfig.api.enabled=true -Dconfig.api.token=${TOKEN}
wait_for_http ${PORT_ON}

BASE="http://127.0.0.1:${PORT_ON}"

echo "Smoke test that unauthenticated requests are rejected ..."
expect_status 401 "${BASE}/api/v1/config/global"
expect_status 401 -H "Authorization: Bearer wrong-token" "${BASE}/api/v1/config/global"

echo "Smoke test the metadata endpoint ..."
curl -f -s "${AUTH[@]}" "${BASE}/api/v1/config/metadata" | grep -q '"xls"'

echo "Smoke test creating a requisition through the API ..."
REQUISITION_XML="${WORK_DIR}/opennms-pris/smokeTest.xml"
cat > "${REQUISITION_XML}" <<'EOF'
<model-import xmlns="http://xmlns.opennms.org/xsd/config/model-import" foreign-source="smokeTest">
    <node node-label="node-a" foreign-id="node-a"/>
</model-import>
EOF

expect_status 201 -X PUT "${AUTH[@]}" -d "{
    \"source\": \"file\",
    \"source.file\": \"${REQUISITION_XML}\",
    \"mapper\": \"echo\"
  }" "${BASE}/api/v1/config/requisitions/smokeTest"

echo "Smoke test that the created requisition is served without a restart ..."
curl -f -s "${BASE}/requisitions/smokeTest" | grep -q 'node-label="node-a"'

echo "Smoke test validating the requisition ..."
curl -f -s -X POST "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest/validate" | grep -q '"ok":true'
curl -f -s -X POST "${AUTH[@]}" -d '{"source":"file","source.file":"/does/not/exist.xml","mapper":"echo"}' \
  "${BASE}/api/v1/config/requisitions/smokeTest/validate" | grep -q '"ok":false'

echo "Smoke test that a hand edit is picked up on the next request ..."
sed 's/node-a/node-b/g' "${REQUISITION_XML}" > "${WORK_DIR}/opennms-pris/smokeTestEdited.xml"
PROPERTIES_FILE="${WORK_DIR}/opennms-pris/requisitions/smokeTest/requisition.properties"
sed -i.bak "s|smokeTest.xml|smokeTestEdited.xml|" "${PROPERTIES_FILE}" && rm -f "${PROPERTIES_FILE}.bak"
curl -f -s "${BASE}/requisitions/smokeTest" | grep -q 'node-label="node-b"'
curl -f -s "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest" | grep -q 'smokeTestEdited.xml'

echo "Smoke test deleting the requisition through the API ..."
expect_status 204 -X DELETE "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest"
expect_status 404 "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest"

echo "Smoke test that the example requisitions still work with the API enabled ..."
expect_status 200 "${BASE}/requisitions/myServer"

stop_pris

echo "Standalone smoke tests passed."
