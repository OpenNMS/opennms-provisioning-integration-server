##
# Makefile to build PRIS
#
# SPDX-License-Identifier: GPL-3.0-only
# Copyright 2026 The OpenNMS Group, Inc.
# Created by Ronny Trommer <ronny@opennms.com>
##
.PHONY: help all deps-build compile package deps-docs deps-docs-docker deps-oci docs docs-check docs-docker oci-stage oci smoke-test clean clean-docs clean-docs-cache clean-all

.DEFAULT_GOAL := help

SHELL                := /bin/bash -o nounset -o pipefail -o errexit
WORKING_DIRECTORY    := $(shell pwd)
DOCKER_ANTORA_IMAGE  := antora/antora:3.1.7
SITE_FILE            := antora-playbook-local.yml
IMAGE                ?= pris
VERSION              ?= $(shell cat version.txt)
RELEASE_ARCHIVE      := opennms-pris-dist/target/opennms-pris-release-archive.tar.gz

help: ## Show this help with all build goals
	@echo ""
	@echo "Usage: make <goal>"
	@echo ""
	@echo "Goals:"
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z0-9_-]+:.*?##/ { printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Variables: IMAGE=$(IMAGE), VERSION=$(VERSION)"
	@echo ""

deps-build:
	@command -v javac
	@command -v mvn

compile: ## Compile the project and run the test suite
	@echo "Maven validate ..."
	mvn validate
	@echo "Maven compile ... "
	mvn compile
	@echo "Maven tests ..."
	mvn verify

package: ## Package the release archives in tar.gz and zip format
	@echo "Maven package ..."
	mvn package -DskipTests

deps-docs:
	@command -v antora

deps-docs-docker:
	@command -v docker

deps-oci:
	@command -v docker

docs: deps-docs ## Build the Antora docs
	@echo "Build Antora docs..."
	antora --stacktrace $(SITE_FILE)

docs-check: deps-docs ## Validate xrefs in the Antora docs
	@echo "Validate xrefs in Antora docs ..."
	NODE_PATH="$$(npm -g root)" antora --generator @antora/xref-validator $(SITE_FILE)

docs-docker: deps-docs-docker ## Build the Antora docs with a docker container
	@echo "Build Antora docs with docker ..."
	docker run --rm -v $(WORKING_DIRECTORY):/antora $(DOCKER_ANTORA_IMAGE) --stacktrace generate $(SITE_FILE)

$(RELEASE_ARCHIVE):
	@echo "Release archive is missing, packaging it ..."
	$(MAKE) package

oci-stage: $(RELEASE_ARCHIVE) ## Stage the release archive for the container build
	cp $(RELEASE_ARCHIVE) docker/deploy/

oci: deps-oci oci-stage ## Build a local container image for the host platform, tagged IMAGE:VERSION
	@echo "Build container image $(IMAGE):$(VERSION) ..."
	docker build --build-arg OPENNMS_PRIS_VERSION=$(VERSION) --build-arg VERSION=$(VERSION) -t $(IMAGE):$(VERSION) docker

smoke-test: deps-oci ## Smoke test the container image IMAGE:VERSION
	./bin/smoke-test.sh "$(IMAGE):$(VERSION)"

clean: ## Remove Maven build artifacts, docs output, staged archive and the local container image IMAGE:VERSION
	@if command -v mvn >/dev/null 2>&1; then echo "Maven clean ..."; mvn clean; else echo "mvn not found, skip Maven clean ..."; fi
	@echo "Delete docs build and public artifacts ..."
	@rm -rf build public
	@echo "Delete staged container archive ..."
	@rm -f docker/deploy/opennms-pris-release-archive.tar.gz
	@echo "Delete local container image $(IMAGE):$(VERSION) if it exists ..."
	@if command -v docker >/dev/null 2>&1; then docker rmi -f $(IMAGE):$(VERSION) >/dev/null 2>&1 || true; fi

clean-docs: ## Delete docs build and public artifacts
	@echo "Delete build and public artifacts ..."
	@rm -rf build public

clean-docs-cache: ## Clean Antora cache for git repositories and UI components
	@echo "Clean Antora cache for git repositories and UI components ..."
	@rm -rf .cache

clean-all: clean clean-docs-cache ## Clean everything including the Antora cache

all: compile package docs ## Compile, package and build the docs
