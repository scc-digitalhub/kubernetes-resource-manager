// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package webhook_test

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/scc-digitalhub/httproute-webhook/pkg/webhook"
)

// ── helpers ─────────────────────────────────────────────────────────────────

var defaultCfg = webhook.Config{
	GatewayName:      "prod-gateway",
	GatewayNamespace: "gateway-system",
	HostnamePattern:  `^[a-z0-9-]+\.{namespace}\.example\.com$`,
}

// makeRoute builds a minimal HTTPRoute JSON object for use in test requests.
func makeRoute(namespace, name string, hostnames []string, parentRefs []webhook.ParentRef) json.RawMessage {
	type spec struct {
		ParentRefs []webhook.ParentRef `json:"parentRefs,omitempty"`
		Hostnames  []string            `json:"hostnames,omitempty"`
	}
	type meta struct {
		Name      string `json:"name"`
		Namespace string `json:"namespace"`
	}
	type route struct {
		Metadata meta `json:"metadata"`
		Spec     spec `json:"spec"`
	}
	r := route{
		Metadata: meta{Name: name, Namespace: namespace},
		Spec: spec{
			Hostnames:  hostnames,
			ParentRefs: parentRefs,
		},
	}
	b, _ := json.Marshal(r)
	return b
}

// makeReview wraps an object JSON into an AdmissionReview payload.
func makeReview(uid, namespace string, obj json.RawMessage) []byte {
	review := webhook.AdmissionReview{
		APIVersion: "admission.k8s.io/v1",
		Kind:       "AdmissionReview",
		Request: &webhook.AdmissionRequest{
			UID:       uid,
			Namespace: namespace,
			Operation: "CREATE",
			Object:    obj,
		},
	}
	b, _ := json.Marshal(review)
	return b
}

// postReview sends a POST request to path and returns the decoded AdmissionReview response.
func postReview(t *testing.T, handler http.Handler, path string, body []byte) webhook.AdmissionReview {
	t.Helper()
	req := httptest.NewRequest(http.MethodPost, path, bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	rr := httptest.NewRecorder()
	handler.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("%s returned HTTP %d: %s", path, rr.Code, rr.Body.String())
	}
	var review webhook.AdmissionReview
	if err := json.NewDecoder(rr.Body).Decode(&review); err != nil {
		t.Fatalf("decoding response: %v", err)
	}
	return review
}

// decodePatch decodes a JSONPatch slice from the bytes in an AdmissionResponse.
func decodePatch(t *testing.T, resp *webhook.AdmissionResponse) []webhook.JSONPatch {
	t.Helper()
	if resp.Patch == nil {
		return nil
	}
	var ops []webhook.JSONPatch
	if err := json.Unmarshal(resp.Patch, &ops); err != nil {
		t.Fatalf("decoding patch: %v", err)
	}
	return ops
}

// ── mutator tests ────────────────────────────────────────────────────────────

func TestMutator_InjectsParentRef_WhenAbsent(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	obj := makeRoute("team-a", "my-route", []string{"api.team-a.example.com"}, nil)
	body := makeReview("uid-1", "team-a", obj)

	resp := postReview(t, mux(h), "/mutate", body).Response
	if !resp.Allowed {
		t.Fatalf("expected allowed=true, got false: %v", resp.Result)
	}
	if resp.PatchType == nil || *resp.PatchType != "JSONPatch" {
		t.Fatal("expected a JSONPatch in the response")
	}
	ops := decodePatch(t, resp)
	if len(ops) != 1 {
		t.Fatalf("expected 1 patch operation, got %d", len(ops))
	}
	if ops[0].Op != "add" || ops[0].Path != "/spec/parentRefs" {
		t.Fatalf("unexpected patch op: %+v", ops[0])
	}
}

func TestMutator_ReplacesParentRefs_WhenOtherRefsExist(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	existing := []webhook.ParentRef{
		{Name: "other-gateway", Namespace: "other-ns"},
		{Name: "another-gateway", Namespace: "another-ns"},
	}
	obj := makeRoute("team-a", "my-route", []string{"api.team-a.example.com"}, existing)
	body := makeReview("uid-2", "team-a", obj)

	resp := postReview(t, mux(h), "/mutate", body).Response
	if !resp.Allowed {
		t.Fatalf("expected allowed, got rejected: %v", resp.Result)
	}
	ops := decodePatch(t, resp)
	if len(ops) != 1 {
		t.Fatalf("expected 1 patch operation, got %d", len(ops))
	}
	// Existing refs must be replaced (not appended to) with a single-element slice.
	if ops[0].Op != "replace" || ops[0].Path != "/spec/parentRefs" {
		t.Fatalf("expected replace on /spec/parentRefs, got op=%q path=%q", ops[0].Op, ops[0].Path)
	}
}

func TestMutator_Replaces_WhenGatewayIsPresentAlongsideOthers(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	// The correct gateway is present but so is an extra one – must still replace.
	existing := []webhook.ParentRef{
		{
			Group:     "gateway.networking.k8s.io",
			Kind:      "Gateway",
			Name:      defaultCfg.GatewayName,
			Namespace: defaultCfg.GatewayNamespace,
		},
		{Name: "extra-gateway", Namespace: "extra-ns"},
	}
	obj := makeRoute("team-a", "my-route", []string{"api.team-a.example.com"}, existing)
	body := makeReview("uid-2b", "team-a", obj)

	resp := postReview(t, mux(h), "/mutate", body).Response
	if !resp.Allowed {
		t.Fatalf("expected allowed, got rejected: %v", resp.Result)
	}
	ops := decodePatch(t, resp)
	if len(ops) != 1 || ops[0].Op != "replace" || ops[0].Path != "/spec/parentRefs" {
		t.Fatalf("expected replace on /spec/parentRefs to remove extra refs, got %+v", ops)
	}
}

func TestMutator_Idempotent_WhenExactlyOneCorrectRef(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	existing := []webhook.ParentRef{
		{
			Group:     "gateway.networking.k8s.io",
			Kind:      "Gateway",
			Name:      defaultCfg.GatewayName,
			Namespace: defaultCfg.GatewayNamespace,
		},
	}
	obj := makeRoute("team-a", "my-route", []string{"api.team-a.example.com"}, existing)
	body := makeReview("uid-3", "team-a", obj)

	resp := postReview(t, mux(h), "/mutate", body).Response
	if !resp.Allowed {
		t.Fatalf("expected allowed, got rejected: %v", resp.Result)
	}
	if resp.Patch != nil {
		t.Fatal("expected no patch when parentRefs already contains exactly the configured gateway")
	}
}

func TestMutator_AllowsDelete_WithNoObject(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	review := webhook.AdmissionReview{
		APIVersion: "admission.k8s.io/v1",
		Kind:       "AdmissionReview",
		Request: &webhook.AdmissionRequest{
			UID:       "uid-del",
			Namespace: "team-a",
			Operation: "DELETE",
			Object:    nil,
		},
	}
	body, _ := json.Marshal(review)
	resp := postReview(t, mux(h), "/mutate", body).Response
	if !resp.Allowed {
		t.Fatal("expected DELETE to be allowed")
	}
}

// ── validator tests ──────────────────────────────────────────────────────────

func TestValidator_Accepts_MatchingHostname(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	obj := makeRoute("team-a", "my-route", []string{"api.team-a.example.com"}, nil)
	body := makeReview("uid-v1", "team-a", obj)

	resp := postReview(t, mux(h), "/validate", body).Response
	if !resp.Allowed {
		t.Fatalf("expected allowed, got rejected: %v", resp.Result)
	}
}

func TestValidator_Rejects_CrossNamespaceHostname(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	// team-b hostname admitted into team-a namespace
	obj := makeRoute("team-a", "my-route", []string{"api.team-b.example.com"}, nil)
	body := makeReview("uid-v2", "team-a", obj)

	resp := postReview(t, mux(h), "/validate", body).Response
	if resp.Allowed {
		t.Fatal("expected rejection for cross-namespace hostname")
	}
	if resp.Result == nil || resp.Result.Code != 403 {
		t.Fatalf("expected 403 status, got: %+v", resp.Result)
	}
}

func TestValidator_Rejects_OneOfManyInvalidHostnames(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	obj := makeRoute("team-a", "my-route", []string{
		"api.team-a.example.com", // valid
		"api.team-b.example.com", // invalid – wrong namespace
	}, nil)
	body := makeReview("uid-v3", "team-a", obj)

	resp := postReview(t, mux(h), "/validate", body).Response
	if resp.Allowed {
		t.Fatal("expected rejection when at least one hostname is invalid")
	}
}

func TestValidator_AcceptsNoHostnames(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	obj := makeRoute("team-a", "my-route", nil, nil)
	body := makeReview("uid-v4", "team-a", obj)

	resp := postReview(t, mux(h), "/validate", body).Response
	if !resp.Allowed {
		t.Fatalf("expected empty hostname list to be accepted: %v", resp.Result)
	}
}

func TestValidator_NoNamespacePlaceholder_AllNamespacesSharePattern(t *testing.T) {
	cfg := webhook.Config{
		GatewayName:      "gw",
		GatewayNamespace: "gw-ns",
		// Pattern without {namespace}: same rule for every namespace
		HostnamePattern: `^[a-z0-9-]+\.shared\.example\.com$`,
	}
	h := webhook.NewHandler(cfg)

	for _, ns := range []string{"team-a", "team-b", "team-c"} {
		obj := makeRoute(ns, "route", []string{fmt.Sprintf("api.shared.example.com")}, nil)
		body := makeReview("uid-"+ns, ns, obj)
		resp := postReview(t, mux(h), "/validate", body).Response
		if !resp.Allowed {
			t.Errorf("namespace %q: expected allowed, got rejected: %v", ns, resp.Result)
		}
	}
}

func TestValidator_AllowsDelete_WithNoObject(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	review := webhook.AdmissionReview{
		APIVersion: "admission.k8s.io/v1",
		Kind:       "AdmissionReview",
		Request: &webhook.AdmissionRequest{
			UID:       "uid-del-v",
			Namespace: "team-a",
			Operation: "DELETE",
		},
	}
	body, _ := json.Marshal(review)
	resp := postReview(t, mux(h), "/validate", body).Response
	if !resp.Allowed {
		t.Fatal("expected DELETE to be allowed")
	}
}

// ── handler HTTP behaviour tests ─────────────────────────────────────────────

func TestHandler_RejectsNonPost(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	for _, method := range []string{http.MethodGet, http.MethodPut, http.MethodDelete} {
		req := httptest.NewRequest(method, "/mutate", nil)
		rr := httptest.NewRecorder()
		mux(h).ServeHTTP(rr, req)
		if rr.Code != http.StatusMethodNotAllowed {
			t.Errorf("method %s: expected 405, got %d", method, rr.Code)
		}
	}
}

func TestHandler_ReturnsBadRequest_OnMalformedJSON(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	req := httptest.NewRequest(http.MethodPost, "/mutate", bytes.NewBufferString("{bad json"))
	rr := httptest.NewRecorder()
	mux(h).ServeHTTP(rr, req)
	if rr.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", rr.Code)
	}
}

func TestHandler_ReturnsBadRequest_OnMissingRequest(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	review := webhook.AdmissionReview{APIVersion: "admission.k8s.io/v1", Kind: "AdmissionReview"}
	body, _ := json.Marshal(review)
	req := httptest.NewRequest(http.MethodPost, "/mutate", bytes.NewReader(body))
	rr := httptest.NewRecorder()
	mux(h).ServeHTTP(rr, req)
	if rr.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", rr.Code)
	}
}

func TestHandler_Healthz(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	req := httptest.NewRequest(http.MethodGet, "/healthz", nil)
	rr := httptest.NewRecorder()
	mux(h).ServeHTTP(rr, req)
	if rr.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", rr.Code)
	}
}

func TestHandler_ResponseMirrorsAPIVersionAndKind(t *testing.T) {
	h := webhook.NewHandler(defaultCfg)
	obj := makeRoute("team-a", "route", []string{"api.team-a.example.com"}, nil)
	body := makeReview("uid-mirror", "team-a", obj)

	reviewReq := httptest.NewRequest(http.MethodPost, "/mutate", bytes.NewReader(body))
	rr := httptest.NewRecorder()
	mux(h).ServeHTTP(rr, reviewReq)

	var review webhook.AdmissionReview
	_ = json.NewDecoder(rr.Body).Decode(&review)
	if review.APIVersion != "admission.k8s.io/v1" {
		t.Errorf("apiVersion not mirrored: %q", review.APIVersion)
	}
	if review.Kind != "AdmissionReview" {
		t.Errorf("kind not mirrored: %q", review.Kind)
	}
}

// ── local helper ─────────────────────────────────────────────────────────────

// mux wires a Handler to an http.ServeMux matching the real server layout.
func mux(h *webhook.Handler) http.Handler {
	m := http.NewServeMux()
	m.HandleFunc("/mutate", h.Mutate)
	m.HandleFunc("/validate", h.Validate)
	m.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
	})
	return m
}
