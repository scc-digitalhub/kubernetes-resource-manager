#!/usr/bin/env bash
# deploy.sh – Build (optional), generate TLS certificates, and deploy the
# httproute-webhook to the target Kubernetes cluster.
#
# Prerequisites: kubectl, openssl, envsubst (part of GNU gettext), docker (optional)
#
# Required environment variables:
#   GATEWAY_NAME        – name of the parent Gateway resource
#   GATEWAY_NAMESPACE   – namespace that contains the parent Gateway
#   WEBHOOK_LABEL_KEY   – label key an HTTPRoute must carry to be intercepted
#                         (e.g. "networking.example.com/managed-gateway")
#   WEBHOOK_LABEL_VALUE – expected value for that label (e.g. "true")
#
# Optional environment variables:
#   NAMESPACE           – webhook deployment namespace (default: httproute-webhook)
#   IMAGE               – container image to deploy (default: below)
#   HOSTNAME_PATTERN    – Go regexp with optional {namespace} placeholder
#                         (default: any valid DNS label sequence)
#   BUILD               – set to "true" to build and push the image before deploying
#   DAYS                – TLS certificate validity in days (default: 3650)
#
# Example:
#   export GATEWAY_NAME=prod-gateway
#   export GATEWAY_NAMESPACE=gateway-system
#   export HOSTNAME_PATTERN=^[a-z0-9-]+\.{namespace}\.example\.com$
#   export WEBHOOK_LABEL_KEY=networking.example.com/managed-gateway
#   export WEBHOOK_LABEL_VALUE=true
#   bash scripts/deploy.sh

set -euo pipefail

: "${GATEWAY_NAME:?Required: export GATEWAY_NAME=<gateway-name>}"
: "${GATEWAY_NAMESPACE:?Required: export GATEWAY_NAMESPACE=<gateway-namespace>}"
: "${WEBHOOK_LABEL_KEY:?Required: export WEBHOOK_LABEL_KEY=<label-key>}"
: "${WEBHOOK_LABEL_VALUE:?Required: export WEBHOOK_LABEL_VALUE=<label-value>}"

export NAMESPACE="${NAMESPACE:-httproute-webhook}"
export WEBHOOK_LABEL_KEY
export WEBHOOK_LABEL_VALUE
export IMAGE="${IMAGE:-ghcr.io/scc-digitalhub/httproute-webhook:latest}"
export HOSTNAME_PATTERN="${HOSTNAME_PATTERN:-^[a-z0-9]([a-z0-9\\-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9\\-]*[a-z0-9])?)*\$}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="${SCRIPT_DIR}/../deploy"

# ── Optional image build ────────────────────────────────────────────────────
# Set BUILD=true to build and push a multi-arch image before deploying.
# build-push.sh requires GITHUB_TOKEN and GITHUB_OWNER to be set.
if [[ "${BUILD:-false}" == "true" ]]; then
    echo "==> Building and pushing multi-arch image via build-push.sh..."
    bash "${SCRIPT_DIR}/build-push.sh"
fi

# ── TLS certificates ────────────────────────────────────────────────────────
echo "==> Generating TLS certificates..."
NAMESPACE="${NAMESPACE}" bash "${SCRIPT_DIR}/generate-certs.sh"

# Load the CA bundle written by generate-certs.sh
if [[ ! -f "${SCRIPT_DIR}/../.ca-bundle" ]]; then
    echo "ERROR: .ca-bundle file not found after cert generation." >&2
    exit 1
fi
# shellcheck source=/dev/null
source "${SCRIPT_DIR}/../.ca-bundle"
export CA_BUNDLE

# ── Core resources ──────────────────────────────────────────────────────────
echo "==> Applying namespace..."
kubectl apply -f "${DEPLOY_DIR}/namespace.yaml"

echo "==> Applying RBAC..."
kubectl apply -f "${DEPLOY_DIR}/rbac.yaml"

# ── Deployment ──────────────────────────────────────────────────────────────
echo "==> Applying deployment (image: ${IMAGE})..."
envsubst '${IMAGE} ${GATEWAY_NAME} ${GATEWAY_NAMESPACE} ${HOSTNAME_PATTERN}' \
    < "${DEPLOY_DIR}/deployment.yaml" \
    | kubectl apply -f -

echo "==> Applying service..."
kubectl apply -f "${DEPLOY_DIR}/service.yaml"

# ── Webhook configurations ──────────────────────────────────────────────────
echo "==> Applying MutatingWebhookConfiguration..."
envsubst '${CA_BUNDLE} ${WEBHOOK_LABEL_KEY} ${WEBHOOK_LABEL_VALUE}' \
    < "${DEPLOY_DIR}/mutating-webhook-config.yaml" \
    | kubectl apply -f -

echo "==> Applying ValidatingWebhookConfiguration..."
envsubst '${CA_BUNDLE} ${WEBHOOK_LABEL_KEY} ${WEBHOOK_LABEL_VALUE}' \
    < "${DEPLOY_DIR}/validating-webhook-config.yaml" \
    | kubectl apply -f -

# ── Rollout wait ────────────────────────────────────────────────────────────
echo "==> Waiting for deployment rollout..."
kubectl rollout status deployment/httproute-webhook \
    -n "${NAMESPACE}" \
    --timeout=120s

echo ""
echo "==> httproute-webhook deployed successfully."
echo "    Namespace:         ${NAMESPACE}"
echo "    Gateway:           ${GATEWAY_NAMESPACE}/${GATEWAY_NAME}"
echo "    Hostname pattern:  ${HOSTNAME_PATTERN}"
echo "    Opt-in label:      ${WEBHOOK_LABEL_KEY}=${WEBHOOK_LABEL_VALUE}"
