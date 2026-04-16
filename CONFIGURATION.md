# Configuration Reference

This document describes all environment variables and application settings for the K8S Resource Manager (KRM), and explains how to configure specific custom resources such as HTTPRoute and the corresponding Envoy Security Policies.

---

## Table of Contents

- [Configuration Reference](#configuration-reference)
  - [Table of Contents](#table-of-contents)
  - [1. Back-end environment variables](#1-back-end-environment-variables)
    - [Server](#server)
    - [Database](#database)
    - [Kubernetes connection](#kubernetes-connection)
    - [Resource filtering](#resource-filtering)
    - [Custom Resource ownership](#custom-resource-ownership)
    - [PersistentVolumeClaim settings](#persistentvolumeclaim-settings)
    - [Secret filtering](#secret-filtering)
    - [Authentication](#authentication)
    - [CORS](#cors)
    - [Management endpoints](#management-endpoints)
  - [2. Front-end environment variables](#2-front-end-environment-variables)
  - [3. Role-based access control](#3-role-based-access-control)
  - [4. Сustom resources](#4-сustom-resources)
    - [HTTPRoute](#httproute)
      - [Prerequisites](#prerequisites)
      - [Enabling HTTPRoute management](#enabling-httproute-management)
      - [Gateway reference](#gateway-reference)
      - [Form fields](#form-fields)
      - [Security policies](#security-policies)
        - [No authentication](#no-authentication)
        - [Basic authentication](#basic-authentication)
        - [API Key authentication](#api-key-authentication)
        - [JWT authentication](#jwt-authentication)

---

## 1. Back-end environment variables

Back-end properties are defined in `src/main/resources/application.yaml`. Every property can be overridden by the corresponding environment variable shown in parentheses.

### Server

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `server.port` | `SERVER_PORT` | `8080` | HTTP port the application listens on |
| `server.servlet.context-path` | `SERVER_CONTEXT` | `/` | Servlet context path |
| `application.url` | `APPLICATION_URL` | _(empty)_ | Public URL of the application |
| `application.core-name` | `KRM_APPLICATION_CORE_NAME` | `dhcore` | Logical name of the KRM instance |

### Database

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | `JDBC_URL` | `jdbc:h2:file:./data/db` | JDBC URL |
| `spring.datasource.driverClassName` | `JDBC_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `spring.datasource.username` | `JDBC_USER` | `sa` | Database username |
| `spring.datasource.password` | `JDBC_PASS` | `password` | Database password |
| `spring.jpa.database-platform` | `JDBC_DIALECT` | `org.hibernate.dialect.H2Dialect` | Hibernate dialect |

### Kubernetes connection

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `kubernetes.config` | `K8S_CONFIG` | _(empty – in-cluster)_ | Path to kubeconfig file, e.g. `file:///home/user/.kube/config`. Leave empty when running inside a Kubernetes pod. |
| `kubernetes.namespace` | `K8S_NAMESPACE` | `default` | Namespace KRM operates in |

### Resource filtering

`kubernetes.crd.allowed` and `kubernetes.crd.denied` are mutually exclusive. Set one or the other, not both.

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `kubernetes.crd.allowed` | `K8S_CRD_ALLOWED` | _(empty)_ | Comma-separated list of CRD names KRM is allowed to manage. When set, all other CRDs are ignored. |
| `kubernetes.crd.denied` | `K8S_CRD_DENIED` | _(empty)_ | Comma-separated list of CRD names KRM must not manage. When set, all other CRDs are allowed. |

Label selectors for standard Kubernetes resources (pipe-separated `key=value` pairs):

| Property | Environment variable | Description |
|---|---|---|
| `kubernetes.selector.service` | `K8S_SELECTOR_SERVICE` | Filter Services |
| `kubernetes.selector.deployment` | `K8S_SELECTOR_DEPLOYMENT` | Filter Deployments |
| `kubernetes.selector.job` | `K8S_SELECTOR_JOB` | Filter Jobs |
| `kubernetes.selector.pvc` | `K8S_SELECTOR_PVC` | Filter PersistentVolumeClaims |
| `kubernetes.selector.quota` | `K8S_SELECTOR_QUOTA` | Filter ResourceQuotas |

**Example** — show only Services labelled `app.kubernetes.io/managed-by=krm`:
```
K8S_SELECTOR_SERVICE=app.kubernetes.io/managed-by=krm
```

### Custom Resource ownership

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `kubernetes.cr.created-by` | `K8S_CR_CREATED_BY` | `krm` | Value written to the `app.kubernetes.io/created-by` label on every CR created by KRM. When set, KRM will only list, update, or delete CRs that carry this label (ownership check). Set to an empty string to disable ownership enforcement. |

### PersistentVolumeClaim settings

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `kubernetes.pvc.managed-by` | `K8S_PVC_MANAGED_BY` | `krm` | Value of the `app.kubernetes.io/managed-by` label injected on PVCs created by KRM |
| `kubernetes.pvc.storage-classes` | `K8S_PVC_STORAGE_CLASSES` | `krm` | Comma-separated list of StorageClass names that can be used when creating PVCs. Leave empty to allow all classes. |

### Secret filtering

Secrets are shown when **at least one** of the following filters matches:

| Property | Environment variable | Description |
|---|---|---|
| `kubernetes.secret.labels` | `K8S_SELECTOR_LABELS` | Pipe-separated `key=value` label selectors |
| `kubernetes.secret.owners` | `K8S_SELECTOR_OWNERS` | Comma-separated owner API versions (full form) |
| `kubernetes.secret.annotations` | `K8S_SELECTOR_ANNOTATIONS` | Pipe-separated `key=value` annotation filters |
| `kubernetes.secret.names` | `K8S_SELECTOR_NAMES` | Comma-separated regular expressions matched against secret names |

### Authentication

KRM supports Basic Auth and OAuth2. Configure one or the other:

**Basic Auth**

| Property | Environment variable | Description |
|---|---|---|
| `auth.basic.username` | `KRM_AUTH_BASIC_USERNAME` | Username |
| `auth.basic.password` | `KRM_AUTH_BASIC_PASSWORD` | Password |

**OAuth2 / OIDC**

| Property | Environment variable | Description |
|---|---|---|
| `auth.oauth2.issuer-uri` | `KRM_AUTH_OAUTH2_ISSUER` | OIDC issuer URL (used to auto-discover JWKS and token endpoints) |
| `auth.oauth2.audience` | `KRM_AUTH_OAUTH2_AUDIENCE` | Expected audience in access tokens |
| `auth.oauth2.client-id` | `KRM_AUTH_OAUTH2_CLIENT_ID` | OAuth2 client ID (also used as default audience) |
| `auth.oauth2.role-claim` | `KRM_AUTH_OAUTH2_ROLE_CLAIM` | JWT claim whose value is mapped to KRM roles |
| `auth.oauth2.scopes` | `KRM_AUTH_OAUTH2_SCOPES` | Comma-separated scopes requested by the front-end |


### CORS

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `security.cors.origins` | `SECURITY_CORS_ORIGINS` | _(empty)_ | Comma-separated list of allowed CORS origins. Set to the front-end URL when they are served from different origins. |

### Management endpoints

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `management.server.port` | `MANAGEMENT_PORT` | `8081` | Port for actuator endpoints (`/health`, `/info`) |

---

## 2. Front-end environment variables

Create a `.env` file (or `.env.development` / `.env.production`) inside the `frontend/` directory.

| Variable | Description | Example |
|---|---|---|
| `PUBLIC_URL` | Public base path of the app | `/` |
| `REACT_APP_CONTEXT_PATH` | Context path (should match `PUBLIC_URL`) | `/` |
| `REACT_APP_AUTH` | Authentication mode: `basic` or `oauth2` | `basic` |
| `REACT_APP_API_URL` | Base URL of the KRM backend (no trailing slash) | `http://localhost:8080` |
| `REACT_APP_APPLICATION_URL` | Public URL of the frontend app | `http://localhost:3000` |
| `REACT_APP_AUTHORITY` | OAuth2 issuer URL _(OAuth2 only)_ | `https://auth.example.com` |
| `REACT_APP_CLIENT_ID` | OAuth2 client ID _(OAuth2 only)_ | `my-client` |
| `REACT_APP_AUTH_CALLBACK_PATH` | OAuth2 redirect callback path _(OAuth2 only)_ | `/auth-callback` |
| `REACT_APP_SCOPE` | OAuth2 scopes _(OAuth2 only)_ | `openid profile` |
| `REACT_APP_SOURCE` | URL of the source code repository | `https://github.com/...` |

---

## 3. Role-based access control

When OAuth2 is configured and `auth.oauth2.role-claim` is set, KRM reads roles from that JWT claim (expects a comma-separated string). Without OAuth2 every authenticated user receives `ROLE_USER`.

Permissions are configured in `application.yaml` under the `access.roles` block:

```yaml
access:
  roles:
    - role: ROLE_USER
      resources: "-"          # deny all
    - role: ROLE_ADMIN
      resources: "*"          # allow all
    - role: ROLE_OPERATOR
      resources: >
        httproutes.gateway.networking.k8s.io,
        securitypolicies.gateway.envoyproxy.io,
        k8s_service::read
```

Permission syntax: `<resource>::<operation>`. Operation is one of `list`, `read`, or `write` (write implies read and list). Omitting the operation defaults to `write`. Wildcards (`*`) are supported for both resource and operation.

---

## 4. Сustom resources

### HTTPRoute

KRM provides dedicated CRUD views for the `HTTPRoute` resource (API group `gateway.networking.k8s.io`) and automatically manages the corresponding `SecurityPolicy` resource (API group `gateway.envoyproxy.io`) to enforce authentication.

#### Prerequisites

- [Envoy Gateway](https://gateway.envoyproxy.io/) must be installed in the cluster.
- An `Envoy Gateway` `GatewayClass` and at least one `Gateway` must already exist. KRM does not create these resources.

#### Enabling HTTPRoute management

Add both CRD names to the allowed list:

```
K8S_CRD_ALLOWED=httproutes.gateway.networking.k8s.io,securitypolicies.gateway.envoyproxy.io
```

or in `application.yaml`:

```yaml
kubernetes:
  crd:
    allowed: httproutes.gateway.networking.k8s.io,securitypolicies.gateway.envoyproxy.io
```

#### Gateway reference

TODO

#### Form fields

| Field | Kubernetes path | Required | Description |
|---|---|---|---|
| Name | `metadata.name` | Yes | HTTPRoute name (immutable after creation) |
| Hostname | `spec.hostnames[0]` | Yes | Virtual host the route serves |
| Backend Service | `spec.rules[0].backendRefs[0].name` | Yes | Kubernetes Service to forward traffic to (selected from the services visible to KRM) |
| Backend Port | `spec.rules[0].backendRefs[0].port` | Yes | Port on the backend Service (populated as a dropdown from the Service's exposed ports) |
| Match Path Prefix | `spec.rules[0].matches[0].path.value` | No | Request path prefix to match (`PathPrefix` type). When empty, all paths are matched. |
| Rewrite Path Prefix | `spec.rules[0].filters[0].urlRewrite.path.replacePrefixMatch` | No | Replaces the matched prefix before forwarding to the backend (`URLRewrite` / `ReplacePrefixMatch` filter). Requires **Match Path Prefix** to also be set. |

#### Security policies

When an authentication type other than **None** is selected, KRM automatically creates (or updates/deletes) a `SecurityPolicy` resource named `<route-name>-sp` that targets the HTTPRoute.

##### No authentication

```yaml
# SecurityPolicy is not created (or deleted if it previously existed).
spec:
  auth:
    type: none
```

##### Basic authentication

Credentials (htpasswd-formatted) are stored in a Kubernetes Secret. The Secret must be created separately before the route is configured.

| Form field | SecurityPolicy path | Description |
|---|---|---|
| Credentials Secret | `spec.basicAuth.users.name` | Name of the Secret containing htpasswd credentials |

Resulting `SecurityPolicy` fragment:
```yaml
spec:
  basicAuth:
    users:
      name: my-htpasswd-secret
```

##### API Key authentication

The API key value is stored in a Kubernetes Secret. The Secret must be created separately.

| Form field | SecurityPolicy path | Description |
|---|---|---|
| Credentials Secret | `spec.apiKeyAuth.credentials[0].name` | Name of the Secret containing the API key |
| API Key Header | `spec.apiKeyAuth.extractFrom[0].header` | HTTP header from which the key is read (default: `x-api-key`) |

Resulting `SecurityPolicy` fragment:
```yaml
spec:
  apiKeyAuth:
    credentials:
      - name: my-apikey-secret
    extractFrom:
      - header: x-api-key
```

##### JWT authentication

KRM configures the Gateway to validate JWTs using a remote JWKS endpoint. Optionally, selected JWT claims can be forwarded to the backend as HTTP headers.

| Form field | SecurityPolicy path | Required | Description |
|---|---|---|---|
| JWT Issuer | `spec.jwt.providers[0].issuer` | Yes | Expected `iss` claim value |
| JWKS URI | `spec.jwt.providers[0].remoteJWKS.uri` | Yes | URL of the JSON Web Key Set endpoint |
| Audiences | `spec.jwt.providers[0].audiences[]` | No | List of accepted `aud` values. When empty the audience check is skipped. |
| Claim → Header mappings | `spec.jwt.providers[0].claimToHeaders[]` | No | Each entry maps a JWT claim name to an HTTP request header forwarded to the backend. |

Resulting `SecurityPolicy` fragment:
```yaml
spec:
  jwt:
    providers:
      - name: default
        issuer: https://auth.example.com
        remoteJWKS:
          uri: https://auth.example.com/.well-known/jwks.json
        audiences:
          - my-audience
        claimToHeaders:
          - claim: sub
            header: x-user-id
          - claim: email
            header: x-user-email
```
