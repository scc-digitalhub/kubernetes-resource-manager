package it.smartcommunitylab.dhub.rm.service;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.model.CustomResourceSchema;
import it.smartcommunitylab.dhub.rm.model.IdAwareCustomResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CustomResourceServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private K8SAuthorizationService authorizationService;

    @Mock
    private CustomResourceDefinitionService crdService;

    @Mock
    private CustomResourceSchemaService schemaService;

    @Mock
    GenericKubernetesResourceList resourceList;

    @Mock
    CustomResourceSchema schema;

    @Mock
    private CustomResourceDefinitionContext context;

    @Mock
    private GenericKubernetesResource genericKubeResource;

    @Mock
    IdAwareCustomResource request;

    @Mock
    MixedOperation<GenericKubernetesResource, GenericKubernetesResourceList, Resource<GenericKubernetesResource>> mixedOperation;

    @Mock
    NamespaceableResource<GenericKubernetesResource> namespaceableResource;

    @Mock
    GenericKubernetesResourceList genericKubernetesResourceList;

    @InjectMocks
    private CustomResourceService customResourceService;

    private final String crdId = "example.com";
    private final String id = "resource1";
    private final String namespace = "default";
    private final String storedVersion = "v1/1";
    private final String version = storedVersion.split("/")[1];

    @BeforeEach
    void setup() {
        context = new CustomResourceDefinitionContext.Builder()
                .withScope("Namespaced")
                .withGroup("com")
                .withName(crdId)
                .withPlural("example")
                .withVersion(version)
                .build();

        lenient().when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);

        genericKubeResource = new GenericKubernetesResource();
        genericKubeResource.setMetadata(new ObjectMeta());
        genericKubeResource.getMetadata().setName(id);
        genericKubeResource.setApiVersion(storedVersion);
        genericKubeResource.setKind("GenericKubernetesResource");

        request = new IdAwareCustomResource(genericKubeResource);

        schema = new CustomResourceSchema();
        schema.setSchema(Collections.singletonMap("$schema", "https://json-schema.org/draft/2020-12/schema"));

        genericKubernetesResourceList = new GenericKubernetesResourceList();
        genericKubernetesResourceList.setItems(Collections.singletonList(genericKubeResource));
    }

    @Test
    public void testFindAll() {

        Pageable pageable = PageRequest.of(0, 10);

        when(crdService.fetchStoredVersionName(crdId)).thenReturn(storedVersion);
        when(schemaService.findCRDByCrdIdAndVersion(crdId, storedVersion)).thenReturn(schema);

        List<GenericKubernetesResource> mockResources = List.of(genericKubeResource);

        when(kubernetesClient.genericKubernetesResources(any())).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(namespace)).thenReturn(mixedOperation);
        when(mixedOperation.list()).thenReturn(resourceList);
        when(resourceList.getItems()).thenReturn(mockResources);

        Page<IdAwareCustomResource> result = customResourceService.findAll(crdId, namespace, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        verify(authorizationService).isCrdAllowed(crdId);
        verify(crdService).fetchStoredVersionName(crdId);
        verify(schemaService).findCRDByCrdIdAndVersion(crdId, storedVersion);
        verify(kubernetesClient).genericKubernetesResources(any());
        verify(mixedOperation).inNamespace(namespace);
        verify(mixedOperation).list();
    }


    @Test
    public void testFindById() {

        lenient().when(crdService.fetchStoredVersionName(crdId)).thenReturn(storedVersion);
        lenient().when(schemaService.findCRDByCrdIdAndVersion(crdId, storedVersion)).thenReturn(schema);

        List<GenericKubernetesResource> mockResources = List.of(genericKubeResource);

        when(kubernetesClient.genericKubernetesResources(any())).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(namespace)).thenReturn(mixedOperation);
        when(mixedOperation.list()).thenReturn(resourceList);
        lenient().when(kubernetesClient.genericKubernetesResources(context).inNamespace(namespace).list()).thenReturn(resourceList);
        lenient().when(resourceList.getItems()).thenReturn(mockResources);
        when(kubernetesClient.resource(genericKubeResource)).thenReturn(namespaceableResource);
        when(namespaceableResource.get()).thenReturn(genericKubeResource);

        IdAwareCustomResource result = customResourceService.findById(crdId, id, namespace);

        assertNotNull(result);
        assertEquals(id, result.getId());

    }

    @Test
    public void testAdd(){

        when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);
        when(crdService.fetchStoredVersionName(crdId)).thenReturn(version);
        when(schemaService.findCRDByCrdIdAndVersion(crdId, version)).thenReturn(schema);

        when(kubernetesClient.resource(any(GenericKubernetesResource.class))).thenReturn(namespaceableResource);
        when(namespaceableResource.inNamespace(namespace)).thenReturn(namespaceableResource);
        when(namespaceableResource.create()).thenReturn(request.getCr());

        IdAwareCustomResource result = customResourceService.add(crdId, request, namespace);

        assertNotNull(result);
        assertEquals(request.getCr(), result.getCr());

    }

    @Test
    public void testUpdate() {

        when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);
        when(crdService.fetchStoredVersionName(crdId)).thenReturn(version);
        when(schemaService.findCRDByCrdIdAndVersion(crdId, version)).thenReturn(schema);

        when(kubernetesClient.genericKubernetesResources(any())).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(namespace)).thenReturn(mixedOperation);
        when(mixedOperation.list()).thenReturn(resourceList);
        when(resourceList.getItems()).thenReturn(Collections.singletonList(genericKubeResource));
        lenient().when(kubernetesClient.resource(genericKubeResource)).thenReturn(namespaceableResource);
        when(namespaceableResource.get()).thenReturn(genericKubeResource);

        when(kubernetesClient.resource(any(GenericKubernetesResource.class))).thenReturn(namespaceableResource);
        when(namespaceableResource.edit((UnaryOperator<GenericKubernetesResource>) any())).thenReturn(genericKubeResource);
        lenient().when(namespaceableResource.inNamespace(namespace)).thenReturn(namespaceableResource);

        IdAwareCustomResource updatedResource = customResourceService.update(crdId, id, request, namespace);

        assertNotNull(updatedResource);
        assertEquals(request.getCr(), updatedResource.getCr());

        //

        CustomResourceDefinitionContext contextNotAllowed = new CustomResourceDefinitionContext.Builder()
                .withScope("Namespaced")
                .withGroup("com")
                .withName("crd-not-allowed")
                .withPlural("example")
                .withVersion(version)
                .build();

        when(authorizationService.isCrdAllowed("crd-not-allowed")).thenReturn(false);


        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> customResourceService.update("crd-not-allowed", id, request, namespace)
        );

        assertEquals(SystemKeys.ERROR_CRD_NOT_ALLOWED, exception.getMessage());
    }

    @Test
    public void testUpdateVersionMismatch() {
        when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);
        when(crdService.fetchStoredVersionName(crdId)).thenReturn("different-version");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customResourceService.update(crdId, id, request, namespace)
        );

        assertEquals("Version 1 is not stored", exception.getMessage());
    }

    @Test
    public void testUpdateCustomResourceNotFound() {
        when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);
        when(crdService.fetchStoredVersionName(crdId)).thenReturn(version);
        when(schemaService.findCRDByCrdIdAndVersion(crdId, version)).thenReturn(schema);

        when(kubernetesClient.genericKubernetesResources(any())).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(namespace)).thenReturn(mixedOperation);
        when(mixedOperation.list()).thenReturn(resourceList);
        when(resourceList.getItems()).thenReturn(Collections.emptyList());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> customResourceService.update(crdId, id, request, namespace)
        );

        assertEquals(SystemKeys.ERROR_NO_CR_WITH_VERSION, exception.getMessage());
    }

    @Test
    public void testDelete() {

        when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);
        when(crdService.fetchStoredVersionName(crdId)).thenReturn(version);
        lenient().when(schemaService.findCRDByCrdIdAndVersion(crdId, version)).thenReturn(schema);

        when(kubernetesClient.genericKubernetesResources(any())).thenReturn(mixedOperation);
        when(mixedOperation.inNamespace(namespace)).thenReturn(mixedOperation);
        when(mixedOperation.list()).thenReturn(resourceList);
        when(resourceList.getItems()).thenReturn(Collections.singletonList(genericKubeResource));
        when(kubernetesClient.resource(genericKubeResource)).thenReturn(namespaceableResource);

        customResourceService.delete(crdId, id, namespace);

        verify(namespaceableResource).delete();

        //

        CustomResourceDefinitionContext contextNotAllowed = new CustomResourceDefinitionContext.Builder()
                .withScope("Namespaced")
                .withGroup("com")
                .withName("crd-not-allowed")
                .withPlural("example")
                .withVersion(version)
                .build();

        when(authorizationService.isCrdAllowed("crd-not-allowed")).thenReturn(false);


        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> customResourceService.delete("crd-not-allowed", id, namespace)
        );

        assertEquals(SystemKeys.ERROR_CRD_NOT_ALLOWED, exception.getMessage());


    }



}
