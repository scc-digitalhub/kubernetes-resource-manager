#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# deploy-cel.sh – Deploy the CEL-based HTTPRoute admission policies.
#
# This is the server-less alternative to deploy.sh. It does NOT require a
# running webhook pod, TLS certificates, or a container image. All admission
# logic (gateway injection and hostname validation) is evaluated in-process by
# the Kubernetes API server using CEL (Common Expression Language).
#
# Prerequisites:
#   kubectl  – connected to the target cluster
#   envsubst – part of GNU gettext (brew install gettext on macOS)
#
# Kubernetes version requirements:
#   >= 1.28  – ValidatingAdmissionPolicy (beta; GA in 1.30)
#   >= 1.32  – MutatingAdmissionPolicy (alpha, feature gate required)
#              Enable with: --feature-gates=MutatingAdmissionPolicy=true
#
# Required environment variables:
#   GATEWAY_NAME        – name of the parent Gateway resource
#   GATEWAY_NAMESPACE   – namespace that contains the parent Gateway
#   WEBHOOK_LABEL_KEY   – label key an HTTPRoute must carry to be intercepted
#                         (e.g. "networking.example.com/managed-gateway")
#   WEBHOOK_LABEL_VALUE – expected value for that label (e.g. "true")
#
# Optional environment variables:
#   NAMESPACE           – namespace for the params ConfigMap (default: httproute-webhook)
#   HOSTNAME_PATTERN    – regex with optional {namespace} placeholder
#                         (default: any valid DNS hostname sequence)
#                         Set to empty string "" to disable hostname validation.
#
# Example:
#   export GATEWAY_NAME=prod-gateway
#   export GATEWAY_NAMESPACE=gateway-system
#   export HOSTNAME_PATTERN='^[a-z0-9-]+\.{namespace}\.example\.com$'
#   export WEBHOOK_LABEL_KEY=networking.example.com/managed-gateway
#   export WEBHOOK_LABEL_VALUE=true
#   bash scripts/deploy-cel.sh

set -euo pipefail

: "${GATEWAY_NAME:?Required: export GATEWAY_NAME=<gateway-name>}"
: "${GATEWAY_NAMESPACE:?Required: export GATEWAY_NAMESPACE=<gateway-namespace>}"
: "${WEBHOOK_LABEL_KEY:?Required: export WEBHOOK_LABEL_KEY=<label-key>}"
: "${WEBHOOK_LABEL_VALUE:?Required: export WEBHOOK_LABEL_VALUE=<label-value>}"

export NAMESPACE="${NAMESPACE:-httproute-webhook}"
# Default: accept any valid DNS hostname (no namespace scoping).
export HOSTNAME_PATTERN="${HOSTNAME_PATTERN:-^[a-z0-9]([a-z0-9\-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9\-]*[a-z0-9])?)*$}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="${SCRIPT_DIR}/../deploy/cel"

# ── Namespace ────────────────────────────────────────────────────────────────
echo "==> Ensuring namespace '${NAMESPACE}' exists..."
kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1 \
  || kubectl create namespace "${NAMESPACE}" \
       --dry-run=client -o yaml | kubectl apply -f -

# ── Parameters ConfigMap ─────────────────────────────────────────────────────
echo "==> Applying params ConfigMap (${NAMESPACE}/httproute-webhook-params)..."
envsubst '${GATEWAY_NAME} ${GATEWAY_NAMESPACE} ${HOSTNAME_PATTERN} ${NAMESPACE}' \
  < "${DEPLOY_DIR}/params-configmap.yaml" \
  | kubectl apply -f -

# ── ValidatingAdmissionPolicy ────────────────────────────────────────────────
# GA in k8s 1.30; apply unconditionally and let the API server reject it if
# the cluster does not support the resource version yet.
echo "==> Applying ValidatingAdmissionPolicy 'httproute-hostname-policy'..."
kubectl apply -f "${DEPLOY_DIR}/validating-admission-policy.yaml"

echo "==> Applying ValidatingAdmissionPolicyBinding 'httproute-hostname-policy'..."
envsubst '${WEBHOOK_LABEL_KEY} ${WEBHOOK_LABEL_VALUE} ${NAMESPACE}' \
  < "${DEPLOY_DIR}/validating-admission-policy-binding.yaml" \
  | kubectl apply -f -

# ── MutatingAdmissionPolicy ──────────────────────────────────────────────────
# Alpha in k8s 1.32; requires the MutatingAdmissionPolicy feature gate.
# Warn the operator rather than hard-failing so that clusters that only need
# hostname validation can still use the validating policy above.
echo "==> Applying MutatingAdmissionPolicy 'httproute-gateway-injection'..."
if kubectl apply -f "${DEPLOY_DIR}/mutating-admission-policy.yaml" 2>/tmp/map-err; then
  echo "==> Applying MutatingAdmissionPolicyBinding 'httproute-gateway-injection'..."
  envsubst '${WEBHOOK_LABEL_KEY} ${WEBHOOK_LABEL_VALUE} ${NAMESPACE}' \
    < "${DEPLOY_DIR}/mutating-admission-policy-binding.yaml" \
    | kubectl apply -f -
else
  echo ""
  echo "WARNING: MutatingAdmissionPolicy could not be applied (see error below)."
  echo "         This resource requires Kubernetes >= 1.32 with the"
  echo "         MutatingAdmissionPolicy feature gate enabled."
  echo "         Hostname validation (ValidatingAdmissionPolicy) was still deployed."
  cat /tmp/map-err
fi

echo ""
echo "==> CEL-based admission policies deployed."
echo "    Mutation  policy : httproute-gateway-injection  (MutatingAdmissionPolicy)"
echo "    Validation policy: httproute-hostname-policy    (ValidatingAdmissionPolicy)"
echo "    Params ConfigMap : ${NAMESPACE}/httproute-webhook-params"
