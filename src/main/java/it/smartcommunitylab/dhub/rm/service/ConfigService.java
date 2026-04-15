// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service that exposes a controlled subset of environment variables / application properties
 * as a key-value configuration map.
 *
 * <p>The set of admitted variable names is driven by the {@code application.config.exposed}
 * property (comma-separated list). Only those names are resolved and returned; any name not
 * present in that allow-list is silently ignored, preventing accidental exposure of secrets.</p>
 */
@Service
public class ConfigService {

    private final Environment environment;

    /** Comma-separated list of property/env-var names that may be exposed. */
    @Value("${application.config.exposed:}")
    private String exposedNames;

    public ConfigService(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns a map containing one entry for each admitted name whose value is resolvable
     * in the current {@link Environment}. Names with no value present are omitted.
     *
     * @return key → value map of exposed configuration entries
     */
    public Map<String, String> getConfig() {
        Map<String, String> result = new LinkedHashMap<>();

        if (!StringUtils.hasText(exposedNames)) {
            return result;
        }

        List<String> names = StringUtils.commaDelimitedListToSet(exposedNames)
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        for (String name : names) {
            String value = environment.getProperty(name);
            if (value != null) {
                result.put(name, value);
            }
        }

        return result;
    }
}
