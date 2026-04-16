#!/usr/bin/env bash
# undeploy.sh – Remove all httproute-webhook resources from the cluster.
#
# Optional environment variables:
#   NAMESPACE  – webhook namespace (default: httproute-webhook)
#   DELETE_NS  – set to "true" to also delete the namespace (default: false)
#
# Example:
#   DELETE_NS=true bash scripts/undeploy.sh

set -euo pipefail

NAMESPACE="${NAMESPACE:-httproute-webhook}"

echo "==> Removing ValidatingWebhookConfiguration..."
kubectl delete validatingwebhookconfiguration httproute-webhook --ignore-not-found

echo "==> Removing MutatingWebhookConfiguration..."
kubectl delete mutatingwebhookconfiguration httproute-webhook --ignore-not-found

echo "==> Removing Service..."
kubectl delete service httproute-webhook -n "${NAMESPACE}" --ignore-not-found

echo "==> Removing Deployment..."
kubectl delete deployment httproute-webhook -n "${NAMESPACE}" --ignore-not-found

echo "==> Removing TLS Secret..."
kubectl delete secret httproute-webhook-certs -n "${NAMESPACE}" --ignore-not-found

echo "==> Removing ServiceAccount..."
kubectl delete serviceaccount httproute-webhook -n "${NAMESPACE}" --ignore-not-found

if [[ "${DELETE_NS:-false}" == "true" ]]; then
    echo "==> Deleting namespace '${NAMESPACE}'..."
    kubectl delete namespace "${NAMESPACE}" --ignore-not-found
fi

echo ""
echo "==> httproute-webhook removed."
