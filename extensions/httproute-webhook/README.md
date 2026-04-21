# HTTPRoute Admission Policies

A server-less Kubernetes admission policy for `HTTPRoute` resources
(`gateway.networking.k8s.io`). It provides two behaviours implemented with
**CEL (Common Expression Language)**, evaluated directly inside the API server —
no webhook pod, TLS certificate, or container image is required.

| Policy type | Kind | What it does |
|---|---|---|
| **Mutating** | `MutatingAdmissionPolicy` | Injects a parent Gateway `parentRef` into every admitted HTTPRoute that does not already reference the configured gateway. |
| **Validating** | `ValidatingAdmissionPolicy` | Rejects HTTPRoutes whose `spec.hostnames` entries do not match a configurable regular-expression pattern parameterised with the route's namespace. |

## How it works

### Parent gateway injection

When an `HTTPRoute` is created or updated, the mutating policy checks whether
`spec.parentRefs` already contains **exactly** the configured gateway as its
sole entry. If not, it replaces the entire `parentRefs` array with a
single-element slice:

```yaml
spec:
  parentRefs:
    - group: gateway.networking.k8s.io
      kind: Gateway
      name: <gatewayName>
      namespace: <gatewayNamespace>
```

The operation is idempotent: if `parentRefs` already contains only the
configured gateway the object is admitted unchanged (the `matchCondition` skips
evaluation entirely).

### Hostname pattern validation

The validating policy evaluates every entry in `spec.hostnames` against the
effective regular expression built from `hostnamePattern` by substituting the
`{namespace}` placeholder with the HTTPRoute's own namespace.

**Example**

```
hostnamePattern: ^[a-z0-9-]+\.{namespace}\.example\.com$
```

For a route in namespace `team-alpha` the effective pattern becomes:

```
^[a-z0-9-]+\.team-alpha\.example\.com$
```

This means:

- `api.team-alpha.example.com` ✅ allowed
- `api.team-beta.example.com` ❌ rejected — cross-namespace hostname

Any route with a non-matching hostname is rejected with a descriptive error
message listing the offending hostnames and the effective pattern.
Setting `hostnamePattern` to an empty string disables hostname validation.

### Same-namespace backend enforcement

The validating policy also rejects any HTTPRoute whose `spec.rules[*].backendRefs`
contain an explicit `namespace` field that refers to a namespace other than the
route's own namespace. This prevents tenants from routing traffic to services
they do not own.

When the `namespace` field is absent the Gateway API defaults it to the route's
namespace, so omitting the field is always allowed:

```yaml
spec:
  rules:
    - backendRefs:
        - name: my-service   # namespace omitted → same-namespace → ✅ allowed
          port: 80
        - name: other-svc
          namespace: team-b  # explicit foreign namespace → ❌ rejected
          port: 80
```

On rejection the error message lists each offending `<name>.<namespace>` reference.

### Opt-in label selector

Both policy bindings use an `objectSelector` to intercept only `HTTPRoute`
objects that carry a specific label. Routes without the label are never
evaluated by the policies.

The label key and value are set at deploy time via `WEBHOOK_LABEL_KEY` and
`WEBHOOK_LABEL_VALUE`. To opt a route in, add the label to its metadata:

```yaml
metadata:
  labels:
    networking.example.com/managed-gateway: "true"
```

## Kubernetes version requirements

| Feature | Min version | State |
|---|---|---|
| `ValidatingAdmissionPolicy` | 1.28 | beta (GA in 1.30) |
| `MutatingAdmissionPolicy` | 1.32 | beyta (feature gate required) |

To enable the alpha gate on your cluster (required for gateway injection):
```
--feature-gates=MutatingAdmissionPolicy=true
```

`deploy-cel.sh` warns gracefully when the `MutatingAdmissionPolicy` API is
unavailable, so clusters that only need hostname validation can still use the
validating policy.

## Prerequisites

- Kubernetes ≥ 1.28 with the Gateway API CRDs installed
- `kubectl` configured for the target cluster
- `envsubst` (part of **GNU gettext**; `brew install gettext` on macOS)

## Quick start

### 1. Deploy

```bash
cd extensions/httproute-webhook

# Required
export GATEWAY_NAME=prod-gateway
export GATEWAY_NAMESPACE=gateway-system
export WEBHOOK_LABEL_KEY=networking.example.com/managed-gateway
export WEBHOOK_LABEL_VALUE=true

# Optional
# export HOSTNAME_PATTERN='^[a-z0-9-]+\.{namespace}\.example\.com$'
# export NAMESPACE=httproute-webhook   # namespace for the params ConfigMap

bash scripts/deploy-cel.sh
```

`deploy-cel.sh` will:

1. Ensure the `${NAMESPACE}` namespace exists.
2. Apply the `httproute-webhook-params` ConfigMap with gateway and hostname
   pattern configuration.
3. Apply the `ValidatingAdmissionPolicy` and its binding (hostname validation).
4. Apply the `MutatingAdmissionPolicy` and its binding (gateway injection),
   warning if the alpha API is unavailable.

### 2. Verify

```bash
# Create a test HTTPRoute with the opt-in label
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

# Check that parentRef was injected
kubectl get httproute test-route -n default -o yaml | grep -A6 parentRefs
```

### 3. Remove

```bash
# Keep the namespace:
bash scripts/undeploy-cel.sh

# Also delete the namespace:
DELETE_NS=true bash scripts/undeploy-cel.sh
```

## Configuration reference

All configuration is stored in the `httproute-webhook-params` ConfigMap in
the `${NAMESPACE}` namespace:

| Key | Required | Default | Description |
|---|---|---|---|
| `gatewayName` | ✅ | – | Name of the parent `Gateway` to inject |
| `gatewayNamespace` | ✅ | – | Namespace of the parent `Gateway` |
| `hostnamePattern` | | any valid DNS hostname | Regex with optional `{namespace}` placeholder; empty string disables validation |

### deploy-cel.sh variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GATEWAY_NAME` | ✅ | – | Stored as `gatewayName` in the params ConfigMap |
| `GATEWAY_NAMESPACE` | ✅ | – | Stored as `gatewayNamespace` in the params ConfigMap |
| `WEBHOOK_LABEL_KEY` | ✅ | – | Label key an `HTTPRoute` must carry to be intercepted |
| `WEBHOOK_LABEL_VALUE` | ✅ | – | Expected value for `WEBHOOK_LABEL_KEY` |
| `NAMESPACE` | | `httproute-webhook` | Kubernetes namespace for the params ConfigMap |
| `HOSTNAME_PATTERN` | | any valid DNS hostname | Regex passed as `hostnamePattern`; `{namespace}` replaced at runtime |

### Updating configuration at runtime

Edit the ConfigMap directly — changes take effect immediately for new admission
requests without any restart:

```bash
kubectl patch configmap httproute-webhook-params \
  -n httproute-webhook \
  --patch '{"data":{"hostnamePattern":"^[a-z0-9-]+\\.{namespace}\\.example\\.com$"}}'
```

## How the CEL expressions work

### Mutation — `MutatingAdmissionPolicy` (`httproute-gateway-injection`)

A `matchCondition` skips evaluation when `spec.parentRefs` already contains
exactly the configured gateway as its sole entry (idempotency guard). When
mutation is needed, a `JSONPatch` expression emits either an `add` operation
(field absent) or a `replace` operation (field present) to enforce a
single-gateway `parentRefs`:

```
!has(object.spec.parentRefs)
  ? JSONPatch{op:"add",    path:"/spec/parentRefs", value:[{group, kind, name, namespace}]}
  : JSONPatch{op:"replace", path:"/spec/parentRefs", value:[{group, kind, name, namespace}]}
```

`reinvocationPolicy: IfNeeded` ensures the policy re-evaluates if another
mutating policy modifies the object afterwards.

### Validation — `ValidatingAdmissionPolicy` (`httproute-hostname-policy`)

A CEL `variable` builds the effective pattern by substituting `{namespace}`
with the HTTPRoute's actual namespace using the CEL `replace()` string function.
The validation expression asserts that every declared hostname matches:

```
params.data.hostnamePattern == "" ||
!has(object.spec.hostnames) ||
object.spec.hostnames.size() == 0 ||
object.spec.hostnames.all(h, h.matches(variables.effectivePattern))
```

On failure, a `messageExpression` lists the offending hostnames and the
effective regex for easy diagnosis.

### Validation rule 2 — same-namespace backendRefs

The second validation expression iterates over every rule and every backendRef,
rejecting any entry whose `namespace` field is set and differs from the route's
own namespace:

```
!has(object.spec.rules) ||
object.spec.rules.all(rule,
  !has(rule.backendRefs) ||
  rule.backendRefs.all(ref,
    !has(ref.namespace) || ref.namespace == object.metadata.namespace
  )
)
```

The `messageExpression` flattens the offending `<name>.<namespace>` pairs
across all rules into a single diagnostic string.

## Repository layout

```
extensions/httproute-webhook/
├── deploy/
│   └── cel/
│       ├── params-configmap.yaml                  # ConfigMap template – gateway + hostname config
│       ├── mutating-admission-policy.yaml          # MutatingAdmissionPolicy (k8s >= 1.32 alpha)
│       ├── mutating-admission-policy-binding.yaml  # Binding template – label + NS selectors
│       ├── validating-admission-policy.yaml        # ValidatingAdmissionPolicy (k8s >= 1.28)
│       └── validating-admission-policy-binding.yaml  # Binding template – label + NS selectors
├── scripts/
│   ├── deploy-cel.sh                              # Deploy CEL policies
│   └── undeploy-cel.sh                            # Remove CEL policies
└── README.md
```

## Hostname pattern examples

| Pattern | Effective pattern (ns=`team-a`) | Effect |
|---|---|---|
| `^[a-z0-9-]+\.{namespace}\.example\.com$` | `^[a-z0-9-]+\.team-a\.example\.com$` | Each namespace can only expose hostnames under its own subdomain |
| `^[a-z0-9-]+\.(staging\|prod)\.example\.com$` | Same (no `{namespace}`) | All namespaces share the same two subdomains |
| `^[a-z0-9-]+\.{namespace}\.[a-z0-9-]+\.example\.com$` | `^[a-z0-9-]+\.team-a\.[a-z0-9-]+\.example\.com$` | Namespace subdomain + arbitrary environment segment |
| *(empty)* | – | Hostname validation disabled; any hostname is accepted |

## Security considerations

- `failurePolicy: Fail` on both policies ensures that if the API server cannot
  evaluate a policy (e.g. the params ConfigMap is missing), HTTPRoute admission
  is blocked rather than silently allowed.
- Both policy bindings exclude `kube-system`, `kube-public`, `kube-node-lease`,
  and the namespace that holds the params ConfigMap to prevent bootstrap issues.
- There is no webhook pod, no TLS secret, and no network-accessible admission
  surface — the entire admission logic runs inside the API server process.

## License

Apache-2.0 – see the root [LICENSE](../../LICENSE) file.
