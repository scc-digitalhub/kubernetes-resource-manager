// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package webhook

import "encoding/json"

// AdmissionReview mirrors admission.k8s.io/v1 AdmissionReview.
type AdmissionReview struct {
	APIVersion string             `json:"apiVersion"`
	Kind       string             `json:"kind"`
	Request    *AdmissionRequest  `json:"request,omitempty"`
	Response   *AdmissionResponse `json:"response,omitempty"`
}

// AdmissionRequest contains the details of an admission request.
type AdmissionRequest struct {
	UID       string          `json:"uid"`
	Namespace string          `json:"namespace,omitempty"`
	Operation string          `json:"operation,omitempty"`
	Object    json.RawMessage `json:"object,omitempty"`
}

// AdmissionResponse is the webhook response payload.
type AdmissionResponse struct {
	UID       string  `json:"uid"`
	Allowed   bool    `json:"allowed"`
	Result    *Status `json:"status,omitempty"`
	Patch     []byte  `json:"patch,omitempty"`
	PatchType *string `json:"patchType,omitempty"`
}

// Status is a simplified Kubernetes Status object.
type Status struct {
	Message string `json:"message,omitempty"`
	Code    int32  `json:"code,omitempty"`
}

// JSONPatch is a single RFC 6902 JSON Patch operation.
type JSONPatch struct {
	Op    string      `json:"op"`
	Path  string      `json:"path"`
	Value interface{} `json:"value,omitempty"`
}

// ParentRef represents a Gateway API parentRef entry.
type ParentRef struct {
	Group     string `json:"group,omitempty"`
	Kind      string `json:"kind,omitempty"`
	Name      string `json:"name"`
	Namespace string `json:"namespace,omitempty"`
}

// httpRouteSpec holds the HTTPRoute spec fields relevant to this webhook.
// ParentRefs is a pointer so that a nil value (field absent in JSON) can be
// distinguished from an empty slice (field present but empty).
type httpRouteSpec struct {
	ParentRefs *[]ParentRef `json:"parentRefs,omitempty"`
	Hostnames  []string     `json:"hostnames,omitempty"`
}

// httpRouteMeta is a minimal subset of ObjectMeta.
type httpRouteMeta struct {
	Name      string `json:"name"`
	Namespace string `json:"namespace"`
}

// httpRoute is a minimal HTTPRoute representation used for unmarshalling.
type httpRoute struct {
	Metadata httpRouteMeta `json:"metadata"`
	Spec     httpRouteSpec `json:"spec"`
}
