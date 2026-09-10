// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Audit trail of the API: one JSON line per request on the {@code AUDIT_LOGGER} logger.
 *
 * Logged requests are the ones that change state (POST, PUT, PATCH, DELETE) plus reads of secrets
 * ({@code GET /api/k8s_secret/**}). Each entry carries timestamp, user, client ip, method, endpoint,
 * status and, for POST/PUT/PATCH, the request payload with sensitive keys redacted.
 *
 * The user is taken exclusively from the Spring Security context, i.e. from the authentication already
 * validated by the security filter chain: the email claim of the JWT principal when present, the
 * authentication name otherwise. Tokens carried by the request are never parsed here, since their
 * signature is not verified at this stage.
 *
 * The response is not buffered: only its status code is recorded.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOGGER");

    private static final String ANONYMOUS = "anonymous";
    private static final String SECRETS_PATH = "/api/k8s_secret";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if (!shouldLog(method, uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToProcess = request;
        if (hasBody(method)) {
            requestToProcess = new ContentCachingRequestWrapper(request);
        }

        try {
            filterChain.doFilter(requestToProcess, response);
        } finally {
            logAuditEntry(requestToProcess, response);
        }
    }

    private boolean shouldLog(String method, String uri) {
        if (
            "POST".equalsIgnoreCase(method) ||
            "PUT".equalsIgnoreCase(method) ||
            "PATCH".equalsIgnoreCase(method) ||
            "DELETE".equalsIgnoreCase(method)
        ) {
            return true;
        }
        return "GET".equalsIgnoreCase(method) && uri != null && uri.contains(SECRETS_PATH);
    }

    private boolean hasBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private void logAuditEntry(HttpServletRequest request, HttpServletResponse response) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("timestamp", Instant.now().toString());
            entry.put("user", resolveUser());
            entry.put("ip", resolveIp(request));
            entry.put("method", request.getMethod());
            entry.put("endpoint", request.getRequestURI());
            // the resource being modified is represented by the URI in REST
            entry.put("resource", request.getRequestURI());
            entry.put("status", response.getStatus());

            if (request instanceof ContentCachingRequestWrapper) {
                entry.put("payload", readPayload((ContentCachingRequestWrapper) request));
            }

            AUDIT_LOGGER.info(objectMapper.writeValueAsString(entry));
        } catch (Exception e) {
            logger.error("Failed to write audit log", e);
        }
    }

    /**
     * User identity as established by the security filter chain: email claim of a JWT principal when
     * available, authentication name otherwise.
     */
    private String resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ANONYMOUS;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt) {
            String email = ((Jwt) principal).getClaimAsString("email");
            if (StringUtils.hasText(email)) {
                return email;
            }
        }

        return StringUtils.hasText(authentication.getName()) ? authentication.getName() : ANONYMOUS;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor) && !"unknown".equalsIgnoreCase(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Object readPayload(ContentCachingRequestWrapper wrapper) {
        try {
            // make sure the body is fully cached: the handler may not have consumed it (e.g. on 403)
            wrapper.getInputStream().readAllBytes();
        } catch (Exception e) {
            // ignore, body already consumed via getReader()
        }

        byte[] body = wrapper.getContentAsByteArray();
        if (body.length == 0) {
            return "";
        }

        try {
            String encoding = wrapper.getCharacterEncoding() != null
                ? wrapper.getCharacterEncoding()
                : StandardCharsets.UTF_8.name();
            String sanitized = sanitizePayload(new String(body, encoding), wrapper.getContentType());
            try {
                // nested logging when the payload is JSON
                return objectMapper.readTree(sanitized);
            } catch (Exception e) {
                return sanitized;
            }
        } catch (Exception e) {
            return "[UNREADABLE PAYLOAD]";
        }
    }

    private String sanitizePayload(String payload, String contentType) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }
        try {
            if (contentType != null && contentType.contains("application/json")) {
                JsonNode node = objectMapper.readTree(payload);
                if (node.isObject()) {
                    ObjectNode objectNode = (ObjectNode) node;
                    maskJsonNode(objectNode);
                    return objectMapper.writeValueAsString(objectNode);
                }
            } else {
                // assume form urlencoded or general query params
                String[] pairs = payload.split("&");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pairs.length; i++) {
                    if (i > 0) {
                        sb.append("&");
                    }
                    String pair = pairs[i];
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        String key = pair.substring(0, idx);
                        String val = pair.substring(idx + 1);
                        if (isSensitiveKey(key)) {
                            sb.append(key).append("=[REDACTED]");
                        } else {
                            sb.append(key).append("=").append(val);
                        }
                    } else {
                        sb.append(pair);
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "[UNPARSABLE PAYLOAD - REDACTED]";
        }
        return payload;
    }

    private void maskJsonNode(ObjectNode objectNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            if (isSensitiveKey(key)) {
                field.setValue(objectNode.textNode("[REDACTED]"));
            } else if (field.getValue().isObject()) {
                maskJsonNode((ObjectNode) field.getValue());
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return (
            lower.contains("token") ||
            lower.contains("password") ||
            lower.contains("secret") ||
            lower.contains("credential")
        );
    }
}
