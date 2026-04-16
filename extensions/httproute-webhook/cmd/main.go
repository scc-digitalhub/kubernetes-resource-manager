// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package main

import (
	"crypto/tls"
	"log"
	"net/http"
	"os"

	"github.com/scc-digitalhub/httproute-webhook/pkg/webhook"
)

func main() {
	certFile := getEnv("TLS_CERT_FILE", "/certs/tls.crt")
	keyFile := getEnv("TLS_KEY_FILE", "/certs/tls.key")
	port := getEnv("PORT", "8443")

	cfg := webhook.Config{
		GatewayName:      requireEnv("GATEWAY_NAME"),
		GatewayNamespace: requireEnv("GATEWAY_NAMESPACE"),
		// HostnamePattern uses {namespace} as a placeholder that is replaced at
		// runtime with the HTTPRoute's actual namespace before regexp matching.
		// Default: any valid DNS hostname (no namespace enforcement).
		HostnamePattern: getEnv("HOSTNAME_PATTERN", `^[a-z0-9]([a-z0-9\-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9\-]*[a-z0-9])?)*$`),
	}

	h := webhook.NewHandler(cfg)

	mux := http.NewServeMux()
	mux.HandleFunc("/mutate", h.Mutate)
	mux.HandleFunc("/validate", h.Validate)
	mux.HandleFunc("/healthz", func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	server := &http.Server{
		Addr:    ":" + port,
		Handler: mux,
		TLSConfig: &tls.Config{
			MinVersion: tls.VersionTLS12,
		},
	}

	log.Printf("Starting HTTPRoute admission webhook server on :%s", port)
	if err := server.ListenAndServeTLS(certFile, keyFile); err != nil {
		log.Fatalf("Server error: %v", err)
	}
}

func requireEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		log.Fatalf("Required environment variable %q is not set", key)
	}
	return v
}

func getEnv(key, defaultVal string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return defaultVal
}
