package it.smartcommunitylab.dhub.rm.service;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class K8SResourceServiceTest {

    @Mock
    KubernetesClient client;

    @Mock
    K8SAuthorizationService authService;

    private K8SResourceService<HasMetadata> k8SResourceService;

    private final String crdId = "example.com";
    private final String namespace = "default";

    IdAwareResource<HasMetadata> resource1;
    List< IdAwareResource<HasMetadata>> resources;

    @BeforeEach
    public void setup() {

        k8SResourceService = spy(new K8SResourceService<HasMetadata>(client, authService, 60) {
            @Override
            protected List<HasMetadata> getItems(String namespace) {
                return new ArrayList<>();
            }
        });

        resources = new ArrayList<>();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(crdId);

        HasMetadata hasMetadata = new HasMetadata() {
            @Override
            public ObjectMeta getMetadata() {
                return meta;
            }

            @Override
            public void setMetadata(ObjectMeta objectMeta) {

            }

            @Override
            public void setApiVersion(String s) {

            }
        };

        hasMetadata.setMetadata(meta);

        resource1 = new IdAwareResource<>(hasMetadata);
        resource1.setId("resource1");
        resources.add(resource1);

    }

    @Test
    public void testFindAll() {
        doReturn(resources).when(k8SResourceService).readResources(namespace);

        Pageable pageable = PageRequest.of(0, 10);

        Page<IdAwareResource<HasMetadata>> result = k8SResourceService.findAll(namespace, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("resource1", result.getContent().get(0).getId());
    }

    @Test
    public void testFindById() {
        String resourceId = "resource1";

        when(k8SResourceService.readResource(resourceId, namespace)).thenReturn(resource1);

        IdAwareResource<HasMetadata> result = k8SResourceService.findById(namespace, resourceId);

        assertEquals(resourceId, result.getId());
        assertEquals(crdId, result.getResource().getMetadata().getName());
    }

    @Test
    public void testFindById_NotFound() {
        String resourceId = "nonexistent";

        // Expect NoSuchElementException when the resource is not found
        assertThrows(NoSuchElementException.class, () -> {
            k8SResourceService.findById(namespace, resourceId);
        });
    }
}
