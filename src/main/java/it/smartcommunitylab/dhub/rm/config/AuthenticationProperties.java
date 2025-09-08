// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConfigurationProperties(prefix = "auth", ignoreUnknownFields = true)
public class AuthenticationProperties {

    @NestedConfigurationProperty
    private BasicAuthenticationProperties basic;

    @NestedConfigurationProperty
    private OAuth2AuthenticationProperties oauth2;

    public BasicAuthenticationProperties getBasic() {
        return basic;
    }

    public void setBasic(BasicAuthenticationProperties basic) {
        this.basic = basic;
    }

    public OAuth2AuthenticationProperties getOauth2() {
        return oauth2;
    }

    public void setOauth2(OAuth2AuthenticationProperties oauth2) {
        this.oauth2 = oauth2;
    }

    public boolean isBasicAuthEnabled() {
        return basic != null && basic.isEnabled();
    }

    public boolean isOAuth2Enabled() {
        return oauth2 != null && oauth2.isEnabled();
    }

    public boolean isRequired() {
        return isBasicAuthEnabled() || isOAuth2Enabled();
    }

    public static class BasicAuthenticationProperties {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isEnabled() {
            return StringUtils.hasText(username) && StringUtils.hasText(password);
        }
    }

    public static class OAuth2AuthenticationProperties {

        private String issuerUri;
        private String audience;
        private String[] scopes;
        private String roleClaim;
        private String clientId;

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public boolean isEnabled() {
            return StringUtils.hasText(issuerUri) && StringUtils.hasText(audience);
        }

        public String[] getScopes() {
            return scopes;
        }

        public void setScopes(String[] scopes) {
            this.scopes = scopes;
        }

        public String getRoleClaim() {
            return roleClaim;
        }

        public void setRoleClaim(String roleClaim) {
            this.roleClaim = roleClaim;
        }

        public String getClientId() {      
            return clientId;
        }

        public void setClientId(String clientId) {  
            this.clientId = clientId;
        }  

    }
}
