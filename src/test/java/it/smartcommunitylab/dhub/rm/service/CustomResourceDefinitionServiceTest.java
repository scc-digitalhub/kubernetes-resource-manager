package it.smartcommunitylab.dhub.rm.service;

import io.fabric8.kubernetes.api.model.apiextensions.v1.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ApiextensionsAPIGroupDSL;
import io.fabric8.kubernetes.client.V1ApiextensionAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.model.IdAwareCustomResourceDefinition;
import it.smartcommunitylab.dhub.rm.repository.CustomResourceSchemaRepository;
import org.junit.jupiter.api.Assertions;
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

import java.io.Serializable;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomResourceDefinitionServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private K8SAuthorizationService authorizationService;

    @Mock
    private CustomResourceSchemaRepository customResourceSchemaRepository;

    @InjectMocks
    private CustomResourceDefinitionService customResourceDefinitionService;

    private CustomResourceDefinition createdCrd;
    private final String crdName = "test-crd";

    @BeforeEach
    public void setup() {
        CustomResourceValidation validation = new CustomResourceValidation();
        validation.setOpenAPIV3Schema(new JSONSchemaProps());

        String crdGroup = "example.com";
        createdCrd = new CustomResourceDefinitionBuilder()
                .withNewMetadata()
                .withName(crdName)
                .endMetadata()
                .withNewSpec()
                .withGroup(crdGroup)
                .addNewVersion()
                .withName("v1")
                .withServed(true)
                .withStorage(true)
                .withSchema(validation) // Add schema to version
                .endVersion()
                .withScope("Namespaced")
                .withNewNames()
                .withPlural("tests")
                .withSingular("test")
                .withKind("Test")
                .endNames()
                .endSpec()
                .build();

        authorizationService.isCrdAllowed(crdName);

        ApiextensionsAPIGroupDSL apiextensions = mock(ApiextensionsAPIGroupDSL.class);
        V1ApiextensionAPIGroupDSL v1Apiextensions = mock(V1ApiextensionAPIGroupDSL.class);
        NonNamespaceOperation<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>> crdOperation = mock(NonNamespaceOperation.class);

        lenient().when(kubernetesClient.apiextensions()).thenReturn(apiextensions);
        lenient().when(apiextensions.v1()).thenReturn(v1Apiextensions);
        lenient().when(v1Apiextensions.customResourceDefinitions()).thenReturn(crdOperation);

        CustomResourceDefinitionList crdList = new CustomResourceDefinitionList();
        crdList.setItems(Collections.singletonList(createdCrd));

        lenient().when(crdOperation.list()).thenReturn(crdList);
        lenient().when(authorizationService.isCrdAllowed(anyString())).thenReturn(true);
        lenient().when(customResourceSchemaRepository.findByCrdIdAndVersion(anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    public void testFetchStoredVersionName() {
        String versionName = customResourceDefinitionService.fetchStoredVersionName(crdName);
        assertEquals("v1", versionName);
    }

    @Test
    public void testFetchStoredVersionNameCustomResource() {
        String versionName = customResourceDefinitionService.fetchStoredVersionName(createdCrd);
        assertEquals("v1", versionName);
    }

    @Test
    public void testGetCrdSchema() {
        Map<String, Serializable> crdSchema = customResourceDefinitionService.getCrdSchema(crdName, "v1");
        assertNotNull(crdSchema);
    }

    @Test
    public void testGetCrdSchemaWithCrd() {
        Map<String, Serializable> crdSchema = customResourceDefinitionService.getCrdSchema(createdCrd);
        assertNotNull(crdSchema);
    }

    @Test
    public void testCrdExists() {
        assertTrue(customResourceDefinitionService.crdExists(crdName, "v1"));
    }

    @Test
    public void testFindAll() {
        Collection<String> ids = new ArrayList<>();
        ids.add(crdName);
        boolean onlyWithoutSchema = true;
        Pageable pageable = PageRequest.of(0, 10);

        Page<IdAwareCustomResourceDefinition> result = customResourceDefinitionService.findAll(ids, onlyWithoutSchema, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    public void testFindAllIdsNull() {
        when(customResourceSchemaRepository.findByCrdIdAndVersion(anyString(), anyString()))
                .thenReturn(Optional.empty());

        boolean onlyWithoutSchema = true;
        Pageable pageable = PageRequest.of(0, 10);
        Page<IdAwareCustomResourceDefinition> result = customResourceDefinitionService.findAll(null, onlyWithoutSchema, pageable);
        Assertions.assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test-crd", result.getContent().get(0).getId());


    }

    @Test
    public void testFindById() {
        IdAwareCustomResourceDefinition result = customResourceDefinitionService.findById(crdName);
        assertNotNull(result);
        assertEquals(crdName, result.getId());
    }

    @Test
    public void testFindByIdThrowsAccessDeniedException() {
        String crdId = "test-crd";

        when(authorizationService.isCrdAllowed(crdId)).thenReturn(false);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> customResourceDefinitionService.findById(crdId)
        );

        assertEquals(SystemKeys.ERROR_CRD_NOT_ALLOWED, exception.getMessage());
    }

    @Test
    public void testFindByIdThrowsNoSuchElementException() {

        String crdId = "non-existent-crd";

        when(authorizationService.isCrdAllowed(crdId)).thenReturn(true);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> customResourceDefinitionService.findById(crdId)
        );

        assertEquals(SystemKeys.ERROR_NO_CRD, exception.getMessage());
    }

}
