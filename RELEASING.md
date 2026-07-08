# Releasing PRIS

This document describes how a release of the OpenNMS Provisioning Integration
Server (PRIS) is cut and what the automated release pipeline publishes.

## Overview

A release is triggered by pushing a git tag matching `v<major>.<minor>.<patch>`
(for example `v2.1.2`). The tag name **without** the leading `v` is used as the
version number.

The [`release`](.github/workflows/release.yml) workflow reacts to `v*` tags and:

1. Validates the documentation cross-references (`make docs-check`).
2. Builds the docs and packages the `.tar.gz` / `.zip` release archives.
3. Builds an `amd64` image and runs the container smoke test (`make smoke-test`).
4. Builds and pushes a **multi-arch** (`linux/amd64`, `linux/arm64`) OCI image to
   `ghcr.io/<owner>/pris`, tagged with both the version and `latest`.
5. Creates a GitHub release for the tag with auto-generated notes and attaches
   the `.tar.gz` and `.zip` archives.

The `master` branch carries a `-SNAPSHOT` development version between releases.
All non-tag pushes and pull requests are only built, packaged and smoke tested
by the [`ci`](.github/workflows/ci.yml) workflow — they do not publish anything.

## Prerequisites

* Push access to `master` and permission to push tags.
* A clean, up-to-date `master` working tree that builds green
  (`make compile`).
* Nothing else is required for the pipeline itself: the release workflow uses
  the built-in `GITHUB_TOKEN` and needs **no repository secrets**. Images are
  pushed with that token to the GHCR namespace of the repository the release is
  cut from.

## Cutting a release

The steps below use `2.1.2` as the release version and `2.1.3-SNAPSHOT` as the
next development version. Substitute your own version numbers.

`bin/changeversion.sh` rewrites the version in every `pom.xml`, in the Antora
`docs/**/antora.yml` files (and flips `prerelease: true` to `false`), and in
`version.txt`.

### 1. Set the release version

```
bin/changeversion.sh -o 2.1.2-SNAPSHOT -n 2.1.2
```

Review the changes and confirm the project still builds:

```
git diff
make compile
```

### 2. Commit and tag

```
git commit -s -am "chore: release version 2.1.2"
git tag -a v2.1.2 -m "Release 2.1.2"
git push origin master v2.1.2
```

Pushing the `v2.1.2` tag starts the release workflow. Watch it in the
repository's **Actions** tab until it completes.

### 3. Set the next development version

```
bin/changeversion.sh -o 2.1.2 -n 2.1.3-SNAPSHOT
git commit -s -am "chore: set development version to 2.1.3-SNAPSHOT"
git push origin master
```

## After the release

Verify the published artifacts:

* The GitHub release for the tag exists and has the `.tar.gz` and `.zip`
  archives attached.
* The container image is available:

  ```
  docker run --rm --publish 8000:8000 ghcr.io/<owner>/pris:2.1.2
  ```

### First release only

The GHCR package created by the very first release is **private**. Flip its
visibility to public once, in the GitHub package settings for the repository
owner; subsequent releases inherit that visibility.
