#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
# Copyright 2026 The OpenNMS Group, Inc.
# Created by Ronny Trommer <ronny@opennms.com>
#
# Smoke test a PRIS container image in both operating modes.
#
# Run 1 (default configuration): the documentation and the example
# requisitions are served on port 8000 and the configuration API is absent -
# proving a default container behaves exactly as before. Writes the fetched
# requisitions to myRouter.xml and myServer.xml.
#
# Run 2 (configuration API enabled via system properties): an authenticated
# CRUD round-trip through /api/v1/config and rejection of unauthenticated
# requests.
#
# Usage: smoke-test.sh <image>

set -euo pipefail

IMAGE="${1:?Usage: $0 <image>}"

TOKEN="container-smoke-token"

CONTAINER_ID=""
cleanup() {
  status=$?
  if [ -n "${CONTAINER_ID}" ]; then
    if [ "${status}" -ne 0 ]; then
      echo "Smoke test failed, container logs:" >&2
      docker logs "${CONTAINER_ID}" >&2 || true
    fi
    docker rm -f "${CONTAINER_ID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

wait_for_http() {
  for _ in {1..30}; do
    if docker exec "${CONTAINER_ID}" curl -s -o /dev/null http://localhost:8000/ 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "ERROR: PRIS did not serve HTTP on port 8000 within 30 seconds" >&2
  return 1
}

expect_status() {
  expected=$1
  shift
  actual="$(docker exec "${CONTAINER_ID}" curl -s -o /dev/null -w '%{http_code}' "$@")"
  if [ "${actual}" != "${expected}" ]; then
    echo "ERROR: expected HTTP ${expected} but got ${actual} for: $*" >&2
    return 1
  fi
}

echo "Run 1: PRIS container with the default configuration from image ${IMAGE} ..."
CONTAINER_ID="$(docker run --detach "${IMAGE}")"

echo "Wait for PRIS to serve HTTP on port 8000 ..."
wait_for_http

echo "Smoke test against documentation ..."
docker exec "${CONTAINER_ID}" curl -L -f -s http://localhost:8000/ | grep "location=\"pris/.*/index.html\""

echo "Smoke test against example requisition myRouter ..."
docker exec "${CONTAINER_ID}" curl -f -s http://localhost:8000/requisitions/myRouter > myRouter.xml

echo "Smoke test against example requisition myServer ..."
docker exec "${CONTAINER_ID}" curl -f -s http://localhost:8000/requisitions/myServer > myServer.xml

echo "Smoke test that the configuration API is absent by default ..."
expect_status 404 http://localhost:8000/api/v1/config/global

docker rm -f "${CONTAINER_ID}" >/dev/null
CONTAINER_ID=""

echo "Run 2: PRIS container with the configuration API enabled ..."
# The image ENTRYPOINT is plain java, so the configuration is passed as
# system properties by overriding the CMD
CONTAINER_ID="$(docker run --detach "${IMAGE}" \
  -Dconfig.api.enabled=true "-Dconfig.api.token=${TOKEN}" \
  -cp '/opt/opennms-pris/lib/*:/opt/opennms-pris/opennms-pris.jar' org.opennms.pris.Starter)"

wait_for_http

BASE="http://localhost:8000"
AUTH=(-H "Authorization: Bearer ${TOKEN}")

echo "Smoke test that unauthenticated requests are rejected ..."
expect_status 401 "${BASE}/api/v1/config/global"
expect_status 401 -H "Authorization: Bearer wrong-token" "${BASE}/api/v1/config/global"

echo "Smoke test the metadata endpoint ..."
docker exec "${CONTAINER_ID}" curl -f -s "${AUTH[@]}" "${BASE}/api/v1/config/metadata" | grep -q '"xls"'

echo "Smoke test an authenticated CRUD round-trip ..."
expect_status 201 -X PUT "${AUTH[@]}" \
  -d '{"source": "xls", "source.file": "../myInventory.xls", "mapper": "echo"}' \
  "${BASE}/api/v1/config/requisitions/smokeTest"
docker exec "${CONTAINER_ID}" curl -f -s "${AUTH[@]}" "${BASE}/api/v1/config/requisitions" | grep -q '"smokeTest"'
docker exec "${CONTAINER_ID}" curl -f -s "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest" | grep -q 'myInventory.xls'
expect_status 204 -X DELETE "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest"
expect_status 404 "${AUTH[@]}" "${BASE}/api/v1/config/requisitions/smokeTest"

echo "Smoke test that the example requisitions still work with the API enabled ..."
expect_status 200 "${BASE}/requisitions/myServer"

echo "Smoke tests passed."
