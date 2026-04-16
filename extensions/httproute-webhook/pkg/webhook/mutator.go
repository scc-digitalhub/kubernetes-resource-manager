//
// SPDX-License-Identifier: Apache-2.0

package webhook

import (
	"encoding/json"
	"fmt"
	"log"
)

const (
	gatewayGroup = "gateway.networking.k8s.io"
	gatewayKind  = "Gateway"
)

// Mutator injects a parent Gateway reference into every HTTPRoute that does not
// already reference the configured gateway.
type Mutator struct {
	cfg Config
}

// NewMutator creates a new Mutator from the given configuration.
func NewMutator(cfg Config) *Mutator {
	return &Mutator{cfg: cfg}
}

// Mutate processes a mutating admission request.
//
// It ensures spec.parentRefs contains exactly one entry pointing to the
// configured Gateway. Any pre-existing parentRefs are replaced so that no
// other gateway can be referenced. The operation is idempotent: if parentRefs
// already contains only the configured gateway the object is admitted unchanged.
func (m *Mutator) Mutate(req *AdmissionRequest) (*AdmissionResponse, error) {
	// DELETE operations carry no object; admit unconditionally.
	if len(req.Object) == 0 {
		return allow(req.UID), nil
	}

	var route httpRoute
	if err := json.Unmarshal(req.Object, &route); err != nil {
		return nil, fmt.Errorf("parsing HTTPRoute: %w", err)
	}

	gatewayRef := ParentRef{
		Group:     gatewayGroup,
		Kind:      gatewayKind,
		Name:      m.cfg.GatewayName,
		Namespace: m.cfg.GatewayNamespace,
	}

	// Idempotency check: parentRefs already contains exactly the one expected
	// gateway ref and nothing else – no patch needed.
	if route.Spec.ParentRefs != nil &&
		len(*route.Spec.ParentRefs) == 1 &&
		(*route.Spec.ParentRefs)[0].Name == m.cfg.GatewayName &&
		(*route.Spec.ParentRefs)[0].Namespace == m.cfg.GatewayNamespace {
		log.Printf("HTTPRoute %s/%s already has the correct sole parentRef %s/%s – skipping mutation",
			route.Metadata.Namespace, route.Metadata.Name,
			m.cfg.GatewayNamespace, m.cfg.GatewayName)
		return allow(req.UID), nil
	}

	// Always replace parentRefs with a single-element slice containing only the
	// configured gateway. Using "replace" when the field exists and "add" when
	// it is absent keeps the patch minimal and correct.
	var patches []JSONPatch
	if route.Spec.ParentRefs == nil {
		patches = []JSONPatch{
			{Op: "add", Path: "/spec/parentRefs", Value: []ParentRef{gatewayRef}},
		}
	} else {
		// "replace" overwrites the entire parentRefs array in one operation,
		// removing any extra entries regardless of how many there were.
		patches = []JSONPatch{
			{Op: "replace", Path: "/spec/parentRefs", Value: []ParentRef{gatewayRef}},
		}
	}

	patchBytes, err := json.Marshal(patches)
	if err != nil {
		return nil, fmt.Errorf("marshalling JSON patch: %w", err)
	}

	log.Printf("Mutating HTTPRoute %s/%s: setting parentRefs to sole gateway %s/%s",
		route.Metadata.Namespace, route.Metadata.Name,
		m.cfg.GatewayNamespace, m.cfg.GatewayName)

	pt := "JSONPatch"
	return &AdmissionResponse{
		UID:       req.UID,
		Allowed:   true,
		Patch:     patchBytes,
		PatchType: &pt,
	}, nil
}
