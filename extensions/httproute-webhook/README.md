# HTTPRoute Admission Webhook

A Kubernetes admission webhook for `HTTPRoute` resources
(`gateway.networking.k8s.io`). It provides two behaviours:

| Webhook type | Path | What it does |
|---|---|---|
| **Mutating** | `/mutate` | Injects a parent Gateway `parentRef` (name + namespace from env vars) into every admitted HTTPRoute that does not already reference that gateway. |
| **Validating** | `/validate` | Rejects HTTPRoutes whose `spec.hostnames` entries do not match a configurable regular-expression pattern parameterised with the route's namespace. |

## How it works

### Parent gateway injection

When an `HTTPRoute` is created or updated, the mutating webhook checks whether
`spec.parentRefs` already contains an entry for the configured gateway. If not,
it appends one:

```yaml
spec:
  parentRefs:
    - group: gateway.networking.k8s.io
      kind: Gateway
      name: <GATEWAY_NAME>
      namespace: <GATEWAY_NAMESPACE>
```

The operation is idempotent: if the parentRef is already present the webhook
passes the object through unchanged.

### Hostname pattern validation

The validating webhook evaluates every entry in `spec.hostnames` against the
effective regular expression built from `HOSTNAME_PATTERN` by substituting the
`{namespace}` placeholder with the HTTPRoute's own namespace.

**Example**

```
HOSTNAME_PATTERN=^[a-z0-9-]+\.{namespace}\.example\.com$
```

For a route in namespace `team-alpha` the effective pattern becomes:

```
^[a-z0-9-]+\.team-alpha\.example\.com$
```

This means:

- `api.team-alpha.example.com` ✅ allowed
- `api.team-beta.example.com` ❌ rejected — cross-namespace hostname

Any route with a hostname that does not match is rejected with a descriptive
error message.

### Opt-in label selector

Both webhook configurations use an `objectSelector` to intercept only
`HTTPRoute` objects that carry a specific label. Routes without the label are
never sent to the webhook by the API server.

The label key and value are set at deploy time via `WEBHOOK_LABEL_KEY` and
`WEBHOOK_LABEL_VALUE`. To opt a route in, add the label to its metadata:

```yaml
metadata:
  labels:
    networking.example.com/managed-gateway: "true"
```

## Environment variables

### Webhook server (pod env vars)

| Variable | Required | Default | Description |
|---|---|---|---|
| `GATEWAY_NAME` | ✅ | – | Name of the parent `Gateway` to inject |
| `GATEWAY_NAMESPACE` | ✅ | – | Namespace of the parent `Gateway` |
| `HOSTNAME_PATTERN` | | `^[a-z0-9]([a-z0-9\-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9\-]*[a-z0-9])?)*$` | Go regexp for hostname validation; `{namespace}` is replaced at runtime |
| `TLS_CERT_FILE` | | `/certs/tls.crt` | Path to the TLS server certificate |
| `TLS_KEY_FILE` | | `/certs/tls.key` | Path to the TLS server private key |
| `PORT` | | `8443` | HTTPS listening port |

### deploy.sh variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GATEWAY_NAME` | ✅ | – | Passed to the pod and used to configure the mutating webhook |
| `GATEWAY_NAMESPACE` | ✅ | – | Passed to the pod and used to configure the mutating webhook |
| `WEBHOOK_LABEL_KEY` | ✅ | – | Label key an `HTTPRoute` must carry to be intercepted by the webhook (e.g. `networking.example.com/managed-gateway`) |
| `WEBHOOK_LABEL_VALUE` | ✅ | – | Expected value for `WEBHOOK_LABEL_KEY` (e.g. `true`) |
| `NAMESPACE` | | `httproute-webhook` | Kubernetes namespace for the webhook deployment |
| `IMAGE` | | `ghcr.io/scc-digitalhub/httproute-webhook:latest` | Container image to deploy |
| `HOSTNAME_PATTERN` | | any valid DNS label sequence | Go regexp passed to the pod; `{namespace}` is replaced at runtime |
| `BUILD` | | `false` | Set to `true` to build and push the image before deploying |
| `DAYS` | | `3650` | TLS certificate validity in days |

## Repository layout

```
extensions/httproute-webhook/
├── cmd/
│   └── main.go                         # Entry point – sets up TLS server
├── pkg/
│   └── webhook/
│       ├── types.go                    # Shared types (AdmissionReview, JSONPatch, …)
│       ├── handler.go                  # HTTP handler + Config type
│       ├── mutator.go                  # Mutating logic (parentRef injection)
│       └── validator.go                # Validating logic (hostname pattern check)
├── deploy/
│   ├── namespace.yaml
│   ├── rbac.yaml                       # ServiceAccount
│   ├── deployment.yaml                 # Template – uses ${VAR} placeholders
│   ├── service.yaml
│   ├── mutating-webhook-config.yaml    # Template – caBundle filled by deploy.sh
│   └── validating-webhook-config.yaml  # Template – caBundle filled by deploy.sh
├── scripts/
│   ├── generate-certs.sh               # Generates self-signed CA + server cert
│   ├── deploy.sh                       # Full deploy (certs + manifests)
│   └── undeploy.sh                     # Remove all resources
├── Dockerfile
├── go.mod
└── README.md
```

## Prerequisites

- Kubernetes ≥ 1.25 with the Gateway API CRDs installed
- `kubectl` configured for the target cluster
- `openssl` (cert generation)
- `envsubst` (part of **GNU gettext**; `brew install gettext` on macOS)
- Docker with the **buildx** plugin (included in Docker Desktop ≥ 4.x; for Linux install via `docker-buildx-plugin`)
- A GitHub personal access token with **`write:packages`** scope (only for pushing to ghcr.io)

## Quick start

### 1. Build and push the image (optional)

Skip this step if you use the pre-built image from ghcr.io.

`scripts/build-push.sh` uses **Docker buildx** to produce a single multi-arch
manifest covering `linux/amd64` and `linux/arm64` and pushes it to ghcr.io.

```bash
cd extensions/httproute-webhook

# Authenticate (one-time or use a CI secret)
export GITHUB_TOKEN=$(gh auth token)          # or a PAT with write:packages
export GITHUB_OWNER=scc-digitalhub           # GitHub user or org

# Optional overrides:
# export IMAGE_NAME=httproute-webhook         # default
# export TAG=v1.0.0                           # default: latest
# export EXTRA_TAGS="v1.0 v1"                # additional tags
# export PLATFORMS=linux/amd64,linux/arm64   # default
# export BUILDER=multiarch-builder           # buildx builder name

bash scripts/build-push.sh
```

The script will:
1. Log in to `ghcr.io` using `GITHUB_TOKEN`.
2. Create (or reuse) a `docker-container` buildx builder capable of cross-compilation.
3. Build for all `PLATFORMS` in a single `docker buildx build --push` call.
4. Tag the manifest as `ghcr.io/${GITHUB_OWNER}/${IMAGE_NAME}:${TAG}` (plus any `EXTRA_TAGS`).

#### GitHub Actions example

```yaml
- name: Build and push multi-arch image
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    GITHUB_OWNER: ${{ github.repository_owner }}
    TAG: ${{ github.ref_name }}
    EXTRA_TAGS: latest
  run: bash extensions/httproute-webhook/scripts/build-push.sh
```

### 2. Deploy

```bash
cd extensions/httproute-webhook

# Required
export GATEWAY_NAME=prod-gateway
export GATEWAY_NAMESPACE=gateway-system
export WEBHOOK_LABEL_KEY=networking.example.com/managed-gateway
export WEBHOOK_LABEL_VALUE=true
export HOSTNAME_PATTERN=^[a-z0-9-]+\.{namespace}\.example\.com$

# Optional
# export NAMESPACE=httproute-webhook
# export IMAGE=ghcr.io/<your-org>/httproute-webhook:latest
# export DAYS=3650

bash scripts/deploy.sh
```

`deploy.sh` will:

1. Generate a self-signed CA and server certificate via `generate-certs.sh`.
2. Store the certificates in the `httproute-webhook-certs` Kubernetes secret.
3. Apply the `Namespace`, `ServiceAccount`, `Deployment`, and `Service`.
4. Inject the CA bundle into the `MutatingWebhookConfiguration` and
   `ValidatingWebhookConfiguration` and apply them.
5. Wait for the deployment rollout to complete.

### 3. Verify

```bash
# Check that the pods are running
kubectl get pods -n httproute-webhook

# Check the webhook is reachable
kubectl run curl-test --image=curlimages/curl --rm -it --restart=Never \
  -- curl -k https://httproute-webhook.httproute-webhook.svc/healthz

# Create a test HTTPRoute with the opt-in label and check it gets the parentRef injected
kubectl apply -f - <<'EOF'
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: test-route
  namespace: default
  labels:
    networking.example.com/managed-gateway: "true"
spec:
  hostnames:
    - test.default.example.com
  rules:
    - backendRefs:
        - name: my-service
          port: 80
EOF

kubectl get httproute test-route -n default -o yaml | grep -A5 parentRefs
```

### 4. Remove

```bash
# Keep the namespace:
bash scripts/undeploy.sh

# Also delete the namespace:
DELETE_NS=true bash scripts/undeploy.sh
```

## Certificate rotation

Re-run `generate-certs.sh` to issue new certificates and update both the
Kubernetes secret and the webhook configurations:

```bash
cd extensions/httproute-webhook

NAMESPACE=httproute-webhook bash scripts/generate-certs.sh

source .ca-bundle
export CA_BUNDLE

# Update the secret (generate-certs.sh already does this via kubectl apply)

# Patch the webhook configurations with the new CA bundle
kubectl patch mutatingwebhookconfiguration httproute-webhook \
    --type='json' \
    -p="[{\"op\":\"replace\",\"path\":\"/webhooks/0/clientConfig/caBundle\",\"value\":\"${CA_BUNDLE}\"}]"

kubectl patch validatingwebhookconfiguration httproute-webhook \
    --type='json' \
    -p="[{\"op\":\"replace\",\"path\":\"/webhooks/0/clientConfig/caBundle\",\"value\":\"${CA_BUNDLE}\"}]"

# Restart pods to pick up the new secret
kubectl rollout restart deployment/httproute-webhook -n httproute-webhook
```

## cert-manager integration (alternative TLS)

If your cluster runs [cert-manager](https://cert-manager.io), you can replace
the self-signed certificate generation with a `Certificate` resource. Annotate
the webhook configurations with
`cert-manager.io/inject-ca-from: httproute-webhook/httproute-webhook-cert`
and cert-manager will manage rotation automatically.

## Hostname pattern examples

| Pattern | Effective pattern (ns=`team-a`) | Effect |
|---|---|---|
| `^[a-z0-9-]+\.{namespace}\.example\.com$` | `^[a-z0-9-]+\.team-a\.example\.com$` | Each namespace can only expose hostnames under its own subdomain |
| `^[a-z0-9-]+\.(staging\|prod)\.example\.com$` | Same (no `{namespace}`) | All namespaces share the same two subdomains |
| `^[a-z0-9-]+\.{namespace}\.[a-z0-9-]+\.example\.com$` | `^[a-z0-9-]+\.team-a\.[a-z0-9-]+\.example\.com$` | Namespace subdomain + arbitrary environment segment |

## Security considerations

- The webhook server runs as a non-root user (UID 65532) in a read-only root
  filesystem with all Linux capabilities dropped.
- TLS 1.2 is the minimum accepted version.
- The webhook is excluded from `kube-system`, `kube-public`, `kube-node-lease`,
  and its own namespace to prevent admission bootstrap deadlocks.
- `failurePolicy: Fail` ensures that if the webhook is unreachable, HTTPRoute
  admission is blocked. Adjust to `Ignore` in development environments if needed.

## License

Apache-2.0 – see the root [LICENSE](../../LICENSE) file.
