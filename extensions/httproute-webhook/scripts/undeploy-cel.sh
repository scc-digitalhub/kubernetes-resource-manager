#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# undeploy-cel.sh – Remove all CEL-based HTTPRoute admission policy resources.
#
# Optional environment variables:
#   NAMESPACE  – namespace that holds the params ConfigMap (default: httproute-webhook)
#   DELETE_NS  – set to "true" to also delete the namespace (default: false)
#
# Example:
#   DELETE_NS=true bash scripts/undeploy-cel.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-httproute-webhook}"

echo "==> Removing MutatingAdmissionPolicyBinding 'httproute-gateway-injection'..."
kubectl delete mutatingadmissionpolicybinding httproute-gateway-injection \
  --ignore-not-found

echo "==> Removing MutatingAdmissionPolicy 'httproute-gateway-injection'..."
kubectl delete mutatingadmissionpolicy httproute-gateway-injection \
  --ignore-not-found

echo "==> Removing ValidatingAdmissionPolicyBinding 'httproute-hostname-policy'..."
kubectl delete validatingadmissionpolicybinding httproute-hostname-policy \
  --ignore-not-found

echo "==> Removing ValidatingAdmissionPolicy 'httproute-hostname-policy'..."
kubectl delete validatingadmissionpolicy httproute-hostname-policy \
  --ignore-not-found

echo "==> Removing params ConfigMap 'httproute-webhook-params'..."
kubectl delete configmap httproute-webhook-params \
  -n "${NAMESPACE}" --ignore-not-found

if [[ "${DELETE_NS:-false}" == "true" ]]; then
  echo "==> Deleting namespace '${NAMESPACE}'..."
  kubectl delete namespace "${NAMESPACE}" --ignore-not-found
fi

echo ""
echo "==> CEL-based admission policies removed."
