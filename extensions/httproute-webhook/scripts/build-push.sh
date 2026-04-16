#!/usr/bin/env bash
# build-push.sh – Build a multi-arch container image and push it to ghcr.io.
#
# Prerequisites:
#   - Docker with the buildx plugin (Docker Desktop ≥ 4.x includes it)
#   - A GHCR personal access token (or GITHUB_TOKEN in CI) with
#     write:packages scope exported as GITHUB_TOKEN
#
# Required environment variables:
#   GITHUB_TOKEN  – token used to authenticate with ghcr.io
#   GITHUB_OWNER  – GitHub username or org that owns the package
#                   (e.g. "scc-digitalhub")
#
# Optional environment variables:
#   IMAGE_NAME    – image name without registry prefix
#                   (default: httproute-webhook)
#   TAG           – primary image tag           (default: latest)
#   EXTRA_TAGS    – space-separated list of additional tags to apply
#                   (e.g. "v1.2.3 v1.2")
#   PLATFORMS     – comma-separated buildx platform list
#                   (default: linux/amd64,linux/arm64)
#   BUILDER       – buildx builder instance name (default: multiarch-builder)
#   CONTEXT       – Docker build context path   (default: directory of this script's parent)
#
# Example – release tag:
#   export GITHUB_TOKEN=$(gh auth token)
#   export GITHUB_OWNER=scc-digitalhub
#   export TAG=v1.0.0
#   export EXTRA_TAGS="v1.0 v1"
#   bash scripts/build-push.sh
#
# Example – CI (GitHub Actions):
#   env:
#     GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
#     GITHUB_OWNER: ${{ github.repository_owner }}
#     TAG: ${{ github.sha }}
#   run: bash scripts/build-push.sh

set -euo pipefail

: "${GITHUB_TOKEN:?Required: export GITHUB_TOKEN=<ghcr-token>}"
: "${GITHUB_OWNER:?Required: export GITHUB_OWNER=<github-user-or-org>}"

IMAGE_NAME="${IMAGE_NAME:-httproute-webhook}"
TAG="${TAG:-latest}"
PLATFORMS="${PLATFORMS:-linux/amd64,linux/arm64}"
BUILDER="${BUILDER:-multiarch-builder}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTEXT="${CONTEXT:-${SCRIPT_DIR}/..}"

REGISTRY="ghcr.io/${GITHUB_OWNER}/${IMAGE_NAME}"

# ── Authenticate with ghcr.io ───────────────────────────────────────────────
echo "==> Logging in to ghcr.io as ${GITHUB_OWNER}..."
echo "${GITHUB_TOKEN}" | docker login ghcr.io -u "${GITHUB_OWNER}" --password-stdin

# ── Ensure a multi-arch buildx builder is available ────────────────────────
if ! docker buildx inspect "${BUILDER}" &>/dev/null; then
    echo "==> Creating buildx builder '${BUILDER}'..."
    docker buildx create --name "${BUILDER}" --driver docker-container --bootstrap
else
    echo "==> Reusing existing buildx builder '${BUILDER}'."
fi
docker buildx use "${BUILDER}"

# ── Assemble --tag flags ────────────────────────────────────────────────────
TAG_FLAGS="--tag ${REGISTRY}:${TAG}"
for extra in ${EXTRA_TAGS:-}; do
    TAG_FLAGS="${TAG_FLAGS} --tag ${REGISTRY}:${extra}"
done

echo "==> Building and pushing multi-arch image"
echo "    Registry:  ${REGISTRY}"
echo "    Tags:      ${TAG} ${EXTRA_TAGS:-}"
echo "    Platforms: ${PLATFORMS}"

# shellcheck disable=SC2086
docker buildx build \
    --platform "${PLATFORMS}" \
    ${TAG_FLAGS} \
    --push \
    "${CONTEXT}"

echo ""
echo "==> Done. Image pushed:"
for tag in ${TAG} ${EXTRA_TAGS:-}; do
    echo "    ${REGISTRY}:${tag}"
done
