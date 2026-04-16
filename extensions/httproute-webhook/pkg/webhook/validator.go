//
// SPDX-License-Identifier: Apache-2.0

package webhook

import (
	"encoding/json"
	"fmt"
	"log"
	"regexp"
	"strings"
)

// Validator enforces that every hostname declared in an HTTPRoute matches a
// cluster-operator-defined pattern that is parameterised with the route's
// namespace, preventing cross-namespace hostname hijacking.
type Validator struct {
	cfg Config
}

// NewValidator creates a new Validator from the given configuration.
func NewValidator(cfg Config) *Validator {
	return &Validator{cfg: cfg}
}

// Validate processes a validating admission request.
//
// It rejects the request when at least one hostname in spec.hostnames does not
// match the effective pattern. The effective pattern is produced by replacing
// the literal string "{namespace}" inside HostnamePattern with the
// regexp.QuoteMeta-escaped namespace of the HTTPRoute being admitted.
//
// Example configuration:
//
//	HOSTNAME_PATTERN=^[a-z0-9-]+\.{namespace}\.example\.com$
//
// For a route in namespace "team-a" the effective pattern becomes:
//
//	^[a-z0-9-]+\.team-a\.example\.com$
func (v *Validator) Validate(req *AdmissionRequest) (*AdmissionResponse, error) {
	// DELETE operations carry no object; admit unconditionally.
	if len(req.Object) == 0 {
		return allow(req.UID), nil
	}

	var route httpRoute
	if err := json.Unmarshal(req.Object, &route); err != nil {
		return nil, fmt.Errorf("parsing HTTPRoute: %w", err)
	}

	// Prefer namespace from the object metadata; fall back to the request field.
	namespace := route.Metadata.Namespace
	if namespace == "" {
		namespace = req.Namespace
	}

	// Substitute the {namespace} placeholder so the pattern becomes
	// namespace-specific. regexp.QuoteMeta ensures that namespace values
	// containing regexp meta-characters (unlikely for DNS labels, but possible)
	// are treated as literals.
	rawPattern := strings.ReplaceAll(
		v.cfg.HostnamePattern,
		"{namespace}",
		regexp.QuoteMeta(namespace),
	)

	re, err := regexp.Compile(rawPattern)
	if err != nil {
		return nil, fmt.Errorf("invalid effective hostname pattern %q: %w", rawPattern, err)
	}

	var invalid []string
	for _, hostname := range route.Spec.Hostnames {
		if !re.MatchString(hostname) {
			invalid = append(invalid, hostname)
		}
	}

	if len(invalid) > 0 {
		msg := fmt.Sprintf(
			"hostnames %v in namespace %q do not match required pattern %q",
			invalid, namespace, rawPattern,
		)
		log.Printf("Rejecting HTTPRoute %s/%s: %s", namespace, route.Metadata.Name, msg)
		return &AdmissionResponse{
			UID:     req.UID,
			Allowed: false,
			Result:  &Status{Message: msg, Code: 403},
		}, nil
	}

	log.Printf("HTTPRoute %s/%s validated successfully (pattern: %s)", namespace, route.Metadata.Name, rawPattern)
	return allow(req.UID), nil
}
