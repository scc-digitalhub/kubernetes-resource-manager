// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.service;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.model.dto.SecretDTO;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

/**
 * Service for K8S Secret resource
 */
@Service
@Validated
public class K8SSecretService extends K8SResourceService<Secret> {
    
    @Value("${kubernetes.secret.managed-by:krm}")
    private String managedByLabel;
    @Value("${kubernetes.secret.labels}")
    private String labelFilters;
    @Value("${kubernetes.secret.owners}")
    private String ownerFilters;
    @Value("${kubernetes.secret.annotations}")
    private String annotationFilters;
    @Value("${kubernetes.secret.names}")
    private String nameFilters;

    Map<String, String> annotations = new HashMap<>();
    Set<String> owners = new HashSet<>();
    Set<String> names = new HashSet<>();

    public K8SSecretService(KubernetesClient client, K8SAuthorizationService authService) {
        super(client, authService, 60);
    }

    public static final Logger logger = LoggerFactory.getLogger(K8SSvcService.class);

    @PostConstruct
    public void initFilters() {
            
            if (StringUtils.hasText(annotationFilters)) {
                 Arrays.asList(annotationFilters.split("\\|")).forEach(a -> {
                    String[] arr = a.split("=");
                    annotations.put(arr[0].trim(), arr[1].trim());
                 });
            }
            if (StringUtils.hasText(ownerFilters)) {
                owners.addAll(StringUtils.commaDelimitedListToSet(ownerFilters));
            }
            if (StringUtils.hasText(nameFilters)) {
                names.addAll(StringUtils.commaDelimitedListToSet(nameFilters));
            }

    }

    @Override
    protected List<Secret> getItems(String namespace) {
        return fetch(namespace).stream().map(s -> secureSecret(s)).toList();
    }

    /**
     * Decode the specified secret.
     * @param namespace
     * @param secretId
     * @param key
     * @return
     */
    public String decode(String namespace, @Pattern(regexp = SystemKeys.REGEX_CR_ID) String secretId, String key) {
        List<Secret> secrets = fetch(namespace);
        
        Optional<Secret> secret = secrets.stream().filter(s -> s.getMetadata().getName().equals(secretId)).findAny();
        if (secret.isEmpty()) throw new IllegalArgumentException("No matching secret found");
        String encoded = secret.get().getData().get(key);
        if (encoded == null) throw new IllegalArgumentException("No matching secret key found");
        return new String(Base64.getDecoder().decode(encoded));
    }

    /**
     * Add new Secret to the namespace
     * @param namespace
     * @param dto
     * @return
     */
    public IdAwareResource<Secret> add(@Nonnull String namespace, @Valid SecretDTO dto) {
        // check if secret with the same name already exists
        List<Secret> secrets = fetch(namespace);
        Optional<Secret> secretOpt = secrets.stream().filter(s -> s.getMetadata().getName().equals(dto.getName())).findAny();
        if (secretOpt.isPresent()) throw new IllegalArgumentException("Secret with the same name already exists");


        if (dto.getData() == null) dto.setData(Collections.emptyMap());
        dto.getData().keySet().forEach(key -> dto.getData().put(key, dto.getData().containsKey(key) ? Base64.getEncoder().encodeToString((dto.getData().get(key) == null ? "" : dto.getData().get(key)).getBytes()): ""));
        Secret secret = new SecretBuilder()
        .withNewMetadata()
            .withName(dto.getName())
            .addToLabels("app.kubernetes.io/managed-by", managedByLabel)
        .endMetadata()
        .withData(dto.getData())
        .build();

        getKubernetesClient().secrets().inNamespace(namespace).resource(secret).create();
        getResourceCache().invalidate(namespace);
        return findById(namespace, dto.getName());
    }

    /**
     * Deleted the Secret with the specified name and namespace
     * @param namespace
     * @param secretId
     */
    public void delete(@Nonnull String namespace, @Pattern(regexp = SystemKeys.REGEX_CR_ID) String secretId) {
        // can delete only if the secret exists and matches the filters
        List<Secret> secrets = fetch(namespace);        
        Optional<Secret> secretOpt = secrets.stream().filter(s -> s.getMetadata().getName().equals(secretId)).findAny();
        if (secretOpt.isEmpty()) throw new IllegalArgumentException("No matching secret found");

        getResourceCache().invalidate(namespace);
        getKubernetesClient().secrets().inNamespace(namespace).withName(secretId).delete();
    }

    private List<Secret> fetch(String namespace) {
        Map<String, Secret> secrets = new HashMap<>();

        if (StringUtils.hasText(labelFilters))
                Arrays.asList(labelFilters.split("\\|")).stream()
                .map(s -> getKubernetesClient().secrets().inNamespace(namespace) .withLabelSelector(s).list().getItems())
                .flatMap(Collection::stream).forEach(s -> secrets.put(s.getMetadata().getName(), s));
        if (!annotations.isEmpty() || !owners.isEmpty() || !names.isEmpty()) {
            List<Secret> all = getKubernetesClient().secrets().inNamespace(namespace).list().getItems();
            all.forEach(s -> {
                // check owners: apiVersion in the list of owners
                if (!owners.isEmpty() && s.getMetadata().getOwnerReferences() != null && s.getMetadata().getOwnerReferences().stream().filter(o -> owners.contains(o.getApiVersion())).findAny().isPresent()) {
                    secrets.put(s.getMetadata().getName(), s);
                } 
                // check annotations: should match at least one
                if (!annotations.isEmpty() && s.getMetadata().getAnnotations() != null && s.getMetadata().getAnnotations().keySet().stream().filter(a -> annotations.containsKey(a) && annotations.get(a).equals(s.getMetadata().getAnnotations().get(a))).findAny().isPresent()) {
                    secrets.put(s.getMetadata().getName(), s);
                }
                // check names: regexp
                if (names.stream().anyMatch(n -> s.getMetadata().getName().matches(n))) {
                    secrets.put(s.getMetadata().getName(), s);
                }
            });
        }
                
        return new LinkedList<>(secrets.values());
    }

    private Secret secureSecret(Secret s) {
        Map<String, String> newData = new HashMap<>();
        for (Entry<String,String> entry : s.getData().entrySet()) {
            newData.put(entry.getKey(), "*************");
        }
        s.setData(newData);
        return s;
    }

}
