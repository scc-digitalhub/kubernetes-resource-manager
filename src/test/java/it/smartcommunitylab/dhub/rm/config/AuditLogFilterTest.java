// SPDX-License-Identifier: Apache-2.0
package it.smartcommunitylab.dhub.rm.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuditLogFilterTest {

    private AuditLogFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setUp() {
        filter = new AuditLogFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        appender = new ListAppender<>();
        appender.start();
        auditLogger().addAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        auditLogger().detachAppender(appender);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getOnRegularResourceIsNotLogged() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/k8s_deployment");

        filter.doFilter(request, response, new MockFilterChain());

        assertTrue(appender.list.isEmpty());
    }

    @Test
    public void getOnSecretIsLogged() throws ServletException, IOException {
        request.setMethod("GET");
        request.setRequestURI("/api/k8s_secret/my-secret");

        filter.doFilter(request, response, new MockFilterChain());

        String entry = singleEntry();
        assertTrue(entry.contains("\"method\":\"GET\""));
        assertTrue(entry.contains("\"endpoint\":\"/api/k8s_secret/my-secret\""));
        assertTrue(entry.contains("\"user\":\"anonymous\""));
        assertFalse(entry.contains("payload"));
    }

    @Test
    public void deleteIsLoggedWithoutPayload() throws ServletException, IOException {
        request.setMethod("DELETE");
        request.setRequestURI("/api/k8s_deployment/my-deployment");

        filter.doFilter(request, response, new MockFilterChain());

        String entry = singleEntry();
        assertTrue(entry.contains("\"method\":\"DELETE\""));
        assertFalse(entry.contains("payload"));
    }

    @Test
    public void postPayloadIsRedacted() throws ServletException, IOException {
        request.setMethod("POST");
        request.setRequestURI("/api/k8s_deployment");
        request.setContentType("application/json");
        request.setContent(
            "{\"username\":\"admin\",\"token\":\"secret-token\",\"spec\":{\"password\":\"secret-pass\"}}".getBytes(
                StandardCharsets.UTF_8
            )
        );

        filter.doFilter(request, response, new MockFilterChain());

        String entry = singleEntry();
        assertTrue(entry.contains("\"username\":\"admin\""));
        assertTrue(entry.contains("\"token\":\"[REDACTED]\""));
        assertTrue(entry.contains("\"password\":\"[REDACTED]\""));
        assertFalse(entry.contains("secret-token"));
        assertFalse(entry.contains("secret-pass"));
    }

    @Test
    public void userIsEmailClaimOfJwtPrincipal() throws ServletException, IOException {
        Jwt jwt = Jwt.withTokenValue("t")
            .header("alg", "none")
            .subject("sub-1")
            .claim("email", "user@example.com")
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        request.setMethod("GET");
        request.setRequestURI("/api/k8s_secret/my-secret");

        filter.doFilter(request, response, new MockFilterChain());

        assertTrue(singleEntry().contains("\"user\":\"user@example.com\""));
    }

    @Test
    public void userFallsBackToAuthenticationName() throws ServletException, IOException {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken("basic-user", "n/a", "ROLE_USER"));
        request.setMethod("DELETE");
        request.setRequestURI("/api/k8s_deployment/my-deployment");

        filter.doFilter(request, response, new MockFilterChain());

        assertTrue(singleEntry().contains("\"user\":\"basic-user\""));
    }

    @Test
    public void ipIsTakenFromForwardedForHeader() throws ServletException, IOException {
        request.setMethod("DELETE");
        request.setRequestURI("/api/k8s_deployment/my-deployment");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");

        filter.doFilter(request, response, new MockFilterChain());

        assertTrue(singleEntry().contains("\"ip\":\"203.0.113.7\""));
    }

    @Test
    public void tokensCarriedByTheRequestAreNotDecoded() throws ServletException, IOException {
        // an unverified JWT: its claims must never reach the audit entry
        String claims = "{\"sub\":\"user-from-token\",\"email\":\"leak@example.org\"}";
        String jwt =
            "eyJhbGciOiJSUzI1NiJ9." +
            Base64.getUrlEncoder().withoutPadding().encodeToString(claims.getBytes(StandardCharsets.UTF_8)) +
            ".signature";
        request.setMethod("GET");
        request.setRequestURI("/api/k8s_secret/my-secret");
        request.addHeader("Authorization", "Bearer " + jwt);

        filter.doFilter(request, response, new MockFilterChain());

        String entry = singleEntry();
        assertFalse(entry.contains("user-from-token"));
        assertFalse(entry.contains("leak@example.org"));
        assertTrue(entry.contains("\"user\":\"anonymous\""));
    }

    private static Logger auditLogger() {
        return (Logger) LoggerFactory.getLogger("AUDIT_LOGGER");
    }

    private String singleEntry() {
        assertEquals(1, appender.list.size(), "exactly one audit entry expected");
        return appender.list.get(0).getFormattedMessage();
    }
}
