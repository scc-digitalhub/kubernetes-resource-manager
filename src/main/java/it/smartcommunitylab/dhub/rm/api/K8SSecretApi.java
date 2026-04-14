// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.fabric8.kubernetes.api.model.Secret;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.model.dto.SecretDTO;
import it.smartcommunitylab.dhub.rm.service.K8SSecretService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * API for K8S Secret
 * 
 * <p>
 * This API provides operations to manipulate K8S Secret
 * </p>
 *
 */
@RestController
@PreAuthorize("@authz.canAccess('k8s_secret', 'list')")
@SecurityRequirement(name = "basicAuth")
@SecurityRequirement(name = "jwtAuth")
@RequestMapping(SystemKeys.API_PATH)
@Validated
public class K8SSecretApi {

    @Autowired
    private K8SSecretService service;

    @Value("${kubernetes.namespace}")
    private String namespace;

    /**
     * List K8S Secret
     * 
     * @param id list of secret id to filter. If null, all secrets will be returned
     * @param pageable pagination parameters
     * @return a page of K8S Secret
     */
    @PreAuthorize("@authz.canAccess('k8s_secret', 'list')")
    @GetMapping("/k8s_secret")
    public Page<IdAwareResource<Secret>> findAll(
        @RequestParam(required = false) Collection<String> id,
        Pageable pageable
    ) {
        return service.findAll(namespace, id, pageable);
    }

    /**
     * Get a specific K8S Secret
     * 
     * @param secretId the id of the secret to retrieve
     * @return the K8S Secret
     */
    @PreAuthorize("@authz.canAccess('k8s_secret', 'read')")
    @GetMapping("/k8s_secret/{secretId}")
    public IdAwareResource<Secret> findById(@PathVariable @Pattern(regexp = SystemKeys.REGEX_CR_ID) String secretId) {
        return service.findById(namespace, secretId);
    }

    /**
     * Delete a specific K8S Secret
     * 
      * @param secretId the id of the secret to delete
      * @return the deleted K8S Secret
      */
    @PreAuthorize("@authz.canAccess('k8s_secret', 'write')")
    @DeleteMapping("/k8s_secret/{secretId}")
    public void delete(@PathVariable @Pattern(regexp = SystemKeys.REGEX_CR_ID) String secretId) {
        service.delete(namespace, secretId);
    }

    /**
     * Add a new K8S Secret
     * 
     * @param secret the K8S Secret to add
     * @return the added K8S Secret
     */
    @PreAuthorize("@authz.canAccess('k8s_secret', 'write')")
    @PostMapping("/k8s_secret")
    public IdAwareResource<Secret> add(@RequestBody @Valid SecretDTO secret) {
        return service.add(namespace, secret);
    }

    /**
     * Decode a specific key in a K8S Secret
     * 
     * @param secretId the id of the secret to decode
     * @param key the key to decode
     * @return a map with the decoded value
     */
    @PreAuthorize("@authz.canAccess('k8s_secret', 'read')")
    @GetMapping("/k8s_secret/{secretId}/decode/{key:.*}")
    public Map<String, String> decodeSecret(@PathVariable @Pattern(regexp = SystemKeys.REGEX_CR_ID) String secretId, @PathVariable String key) {
        return Collections.singletonMap(key,  service.decode(namespace, secretId, key));
    }

}
