//
// SPDX-License-Identifier: Apache-2.0

package webhook

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
)

// Config holds the runtime configuration for the webhook.
type Config struct {
	// GatewayName is the name of the parent Gateway to inject into HTTPRoutes.
	GatewayName string
	// GatewayNamespace is the namespace of the parent Gateway.
	GatewayNamespace string
	// HostnamePattern is a Go regular expression used to validate HTTPRoute
	// hostnames. The literal placeholder {namespace} is replaced with the
	// HTTPRoute's own namespace before the pattern is compiled and evaluated,
	// allowing per-namespace hostname enforcement.
	//
	// Example: "^[a-z0-9-]+\\.{namespace}\\.example\\.com$"
	HostnamePattern string
}

// Handler dispatches admission webhook HTTP requests to the Mutator and Validator.
type Handler struct {
	mutator   *Mutator
	validator *Validator
}

// NewHandler creates a new Handler from the given configuration.
func NewHandler(cfg Config) *Handler {
	return &Handler{
		mutator:   NewMutator(cfg),
		validator: NewValidator(cfg),
	}
}

// Mutate handles mutating admission requests (POST /mutate).
func (h *Handler) Mutate(w http.ResponseWriter, r *http.Request) {
	h.handle(w, r, h.mutator.Mutate)
}

// Validate handles validating admission requests (POST /validate).
func (h *Handler) Validate(w http.ResponseWriter, r *http.Request) {
	h.handle(w, r, h.validator.Validate)
}

// admitFunc is the signature of a function that processes an admission request.
type admitFunc func(req *AdmissionRequest) (*AdmissionResponse, error)

// maxBodyBytes is the maximum size of an admission request body (1 MiB).
const maxBodyBytes = 1 << 20

func (h *Handler) handle(w http.ResponseWriter, r *http.Request, admit admitFunc) {
	if r.Method != http.MethodPost {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	defer r.Body.Close()
	body, err := io.ReadAll(io.LimitReader(r.Body, maxBodyBytes))
	if err != nil {
		http.Error(w, fmt.Sprintf("reading body: %v", err), http.StatusBadRequest)
		return
	}

	var review AdmissionReview
	if err := json.Unmarshal(body, &review); err != nil {
		http.Error(w, fmt.Sprintf("decoding AdmissionReview: %v", err), http.StatusBadRequest)
		return
	}
	if review.Request == nil {
		http.Error(w, "missing request field", http.StatusBadRequest)
		return
	}

	resp, err := admit(review.Request)
	if err != nil {
		log.Printf("admission error uid=%s: %v", review.Request.UID, err)
		resp = &AdmissionResponse{
			UID:     review.Request.UID,
			Allowed: false,
			Result:  &Status{Message: err.Error(), Code: 500},
		}
	}

	out := AdmissionReview{
		// Mirror the apiVersion/kind from the incoming review so the API server
		// can match the response to the correct webhook version.
		APIVersion: review.APIVersion,
		Kind:       review.Kind,
		Response:   resp,
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(out); err != nil {
		log.Printf("encoding response uid=%s: %v", review.Request.UID, err)
	}
}

// allow returns a simple "allowed" AdmissionResponse for the given request UID.
func allow(uid string) *AdmissionResponse {
	return &AdmissionResponse{UID: uid, Allowed: true}
}
