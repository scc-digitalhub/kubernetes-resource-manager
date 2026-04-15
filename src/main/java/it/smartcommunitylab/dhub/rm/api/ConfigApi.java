// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.service.ConfigService;

/**
 * API to retrieve a controlled set of application configuration values.
 *
 * <p>Only keys explicitly listed in {@code application.config.exposed}
 * ({@code KRM_CONFIG_EXPOSED}) are returned. All other properties and
 * environment variables remain inaccessible through this endpoint.</p>
 */
@RestController
@PreAuthorize("hasAuthority('ROLE_USER')")
@SecurityRequirement(name = "basicAuth")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping(SystemKeys.API_PATH + "/config")
@Validated
public class ConfigApi {

    @Autowired
    private ConfigService service;

    /**
     * Returns the exposed configuration as a flat key → value map.
     *
     * @return map of exposed configuration entries (may be empty)
     */
    @GetMapping
    public Map<String, String> getConfig() {
        return service.getConfig();
    }
}
