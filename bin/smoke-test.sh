#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
# Copyright 2026 The OpenNMS Group, Inc.
# Created by Ronny Trommer <ronny@opennms.com>
#
# Smoke test a PRIS container image: start it, verify the documentation and
# the example requisitions are served on port 8000, then tear it down.
# Writes the fetched requisitions to myRouter.xml and myServer.xml.
#
# Usage: smoke-test.sh <image>

set -euo pipefail

IMAGE="${1:?Usage: $0 <image>}"

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

echo "Run PRIS container from image ${IMAGE} ..."
CONTAINER_ID="$(docker run --detach "${IMAGE}")"

echo "Wait for PRIS to serve HTTP on port 8000 ..."
ready=false
for _ in {1..30}; do
  if docker exec "${CONTAINER_ID}" curl -s -o /dev/null http://localhost:8000/ 2>/dev/null; then
    ready=true
    break
  fi
  sleep 1
done
if [ "${ready}" != "true" ]; then
  echo "ERROR: PRIS did not serve HTTP on port 8000 within 30 seconds" >&2
  exit 1
fi

echo "Smoke test against documentation ..."
docker exec "${CONTAINER_ID}" curl -L -f -s http://localhost:8000/ | grep "location=\"pris/.*/index.html\""

echo "Smoke test against example requisition myRouter ..."
docker exec "${CONTAINER_ID}" curl -f -s http://localhost:8000/requisitions/myRouter > myRouter.xml

echo "Smoke test against example requisition myServer ..."
docker exec "${CONTAINER_ID}" curl -f -s http://localhost:8000/requisitions/myServer > myServer.xml

echo "Smoke tests passed."
