#!/usr/bin/env bash
# generate-certs.sh – Generate a self-signed CA and a server certificate for
# the httproute-webhook, then store them in a Kubernetes generic secret.
#
# Usage:
#   [NAMESPACE=httproute-webhook] [SERVICE=httproute-webhook] \
#   [SECRET_NAME=httproute-webhook-certs] [DAYS=3650] \
#   bash scripts/generate-certs.sh
#
# Outputs:
#   <TMPDIR>/ca.crt  – CA certificate (also copied to ./ca.crt)
#   .ca-bundle       – file containing CA_BUNDLE=<base64-encoded CA cert>
#
# The script requires: openssl, kubectl
set -euo pipefail

NAMESPACE="${NAMESPACE:-httproute-webhook}"
SERVICE="${SERVICE:-httproute-webhook}"
SECRET_NAME="${SECRET_NAME:-httproute-webhook-certs}"
DAYS="${DAYS:-3650}"

TMPDIR="$(mktemp -d)"
trap 'rm -rf "$TMPDIR"' EXIT

echo "==> Generating CA private key (4096-bit RSA)..."
openssl genrsa -out "$TMPDIR/ca.key" 4096 2>/dev/null

echo "==> Generating self-signed CA certificate (${DAYS} days)..."
openssl req -new -x509 \
    -key "$TMPDIR/ca.key" \
    -out "$TMPDIR/ca.crt" \
    -days "$DAYS" \
    -subj "/CN=httproute-webhook-ca/O=httproute-webhook"

echo "==> Generating server private key (4096-bit RSA)..."
openssl genrsa -out "$TMPDIR/tls.key" 4096 2>/dev/null

echo "==> Generating server certificate signing request..."
openssl req -new \
    -key "$TMPDIR/tls.key" \
    -out "$TMPDIR/tls.csr" \
    -subj "/CN=${SERVICE}.${NAMESPACE}.svc/O=httproute-webhook"

# Subject Alternative Names required for the Kubernetes API server to trust
# the webhook's TLS certificate.
cat > "$TMPDIR/san.cnf" <<EOF
[req]
req_extensions = v3_req
distinguished_name = req_distinguished_name

[req_distinguished_name]

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${SERVICE}
DNS.2 = ${SERVICE}.${NAMESPACE}
DNS.3 = ${SERVICE}.${NAMESPACE}.svc
DNS.4 = ${SERVICE}.${NAMESPACE}.svc.cluster.local
EOF

echo "==> Signing server certificate with CA (${DAYS} days)..."
openssl x509 -req \
    -in "$TMPDIR/tls.csr" \
    -CA "$TMPDIR/ca.crt" \
    -CAkey "$TMPDIR/ca.key" \
    -CAcreateserial \
    -out "$TMPDIR/tls.crt" \
    -days "$DAYS" \
    -extensions v3_req \
    -extfile "$TMPDIR/san.cnf" 2>/dev/null

echo "==> Ensuring namespace '${NAMESPACE}' exists..."
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo "==> Storing certificates in Kubernetes secret '${SECRET_NAME}'..."
kubectl create secret generic "$SECRET_NAME" \
    --from-file=tls.crt="$TMPDIR/tls.crt" \
    --from-file=tls.key="$TMPDIR/tls.key" \
    --from-file=ca.crt="$TMPDIR/ca.crt" \
    --namespace="$NAMESPACE" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "==> Saving CA certificate to ./ca.crt ..."
cp "$TMPDIR/ca.crt" ./ca.crt

CA_BUNDLE="$(base64 < "$TMPDIR/ca.crt" | tr -d '\n')"
echo "CA_BUNDLE=${CA_BUNDLE}" > .ca-bundle

echo ""
echo "==> Done. CA bundle written to .ca-bundle"
echo "    The deploy.sh script will read it automatically."
