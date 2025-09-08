// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.controller;

import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.config.ApplicationProperties;
import it.smartcommunitylab.dhub.rm.config.AuthenticationProperties;
import it.smartcommunitylab.dhub.rm.service.AccessControlService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * This controller handles the root URL and redirects to the console.
 * <p>
 * The controller also handles the console URL and adds the configuration
 * to the model that is needed by the React application.
 * <p>
 * The configuration includes the application URL, the API URL, the context
 * path, the authentication method and the OAuth2 configuration.
 */
@Controller
public class MainController {

    @Autowired
    AuthenticationProperties authenticationProperties;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    AccessControlService acService;

    private static final String CONSOLE_CONTEXT = SystemKeys.CONSOLE_PATH;

    @GetMapping("/")
    public ModelAndView root() {
        return new ModelAndView("redirect:" + CONSOLE_CONTEXT + "/");
    }

    /**
     * Handles the console URL and adds the configuration to the model.
     * 
     * @param model
     *            the model to add the configuration to
     * @param request
     *            the HTTP request
     * @return the name of the view
     */
    @GetMapping(value = { CONSOLE_CONTEXT, CONSOLE_CONTEXT + "/**" })
    public String console(Model model, HttpServletRequest request) {
        String requestUrl = ServletUriComponentsBuilder
            .fromRequestUri(request)
            .replacePath(request.getContextPath())
            .build()
            .toUriString();

        String applicationUrl = StringUtils.hasText(applicationProperties.getUrl())
            ? applicationProperties.getUrl()
            : requestUrl;

        // build config
        Map<String, String> config = new HashMap<>();
        config.put("REACT_APP_APPLICATION_URL", applicationUrl);
        config.put("REACT_APP_API_URL", "");

        config.put("REACT_APP_CONTEXT_PATH", CONSOLE_CONTEXT);
        config.put("REACT_APP_AUTH", "none");
        if (authenticationProperties.isBasicAuthEnabled()) {
            config.put("REACT_APP_AUTH", "basic");
        }

        if (authenticationProperties.isOAuth2Enabled()) {
            config.put("REACT_APP_AUTH", "oauth2");
            config.put("REACT_APP_AUTHORITY", authenticationProperties.getOauth2().getIssuerUri());
            config.put("REACT_APP_CLIENT_ID", authenticationProperties.getOauth2().getClientId());
            if (authenticationProperties.getOauth2().getScopes() != null) {
                config.put("REACT_APP_SCOPE", String.join(" ", authenticationProperties.getOauth2().getScopes()));
            }
        }

        config.put("REACT_APP_CORE_NAME", applicationProperties.getCoreName());

        model.addAttribute("config", config);
        return "console.html";
    }
}
