package it.smartcommunitylab.dhub.rm.service;

import com.google.common.cache.LoadingCache;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.model.dto.SecretDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class K8SSecretServiceTest {

    @Mock
    KubernetesClient client;

    @Mock
    K8SAuthorizationService authService;

    @Mock
    MixedOperation<Secret, SecretList, Resource<Secret>> secretOperation;

    @Mock
    SecretDTO secretDTO;

    @Mock
    Resource<Secret> secretResource;

    @Mock
    LoadingCache<String, Map<String, IdAwareResource<Secret>>> resourceCache;

    @Mock
    Resource<Secret> resourceOperation;

    @InjectMocks
    K8SSecretService k8sSecretService;

    String namespace = "test";
    String key = "username";
    String secretId = "my-secret-id";
    String encodedValue = Base64.getEncoder().encodeToString("admin".getBytes());

    Secret secret;
    SecretList secretList;

    @BeforeEach
    public void setup() {
        secret = new SecretBuilder()
                .withNewMetadata().withName(secretId).withNamespace(namespace).endMetadata()
                .addToData(key, encodedValue)
                .build();

        secretDTO = new SecretDTO();
        secretDTO.setName(secretId);

        secretList = new SecretList();
        secretList.setItems(List.of(secret));

        k8sSecretService = new K8SSecretService(client, authService);
        ReflectionTestUtils.setField(k8sSecretService, "labelFilters", "");
        ReflectionTestUtils.setField(k8sSecretService, "annotationFilters", "");
        ReflectionTestUtils.setField(k8sSecretService, "ownerFilters", "");
        ReflectionTestUtils.setField(k8sSecretService, "nameFilters", secretId);
        k8sSecretService.initFilters();
    }

    @Test
    public void testInitFilters() {

        ReflectionTestUtils.setField(k8sSecretService, "annotationFilters", "key1=value1|key2=value2");
        ReflectionTestUtils.setField(k8sSecretService, "ownerFilters", "owner1,owner2");
        ReflectionTestUtils.setField(k8sSecretService, "nameFilters", "name1,name2");

        k8sSecretService.initFilters();

        Map<String, String> annotations = k8sSecretService.annotations;
        assertEquals(2, annotations.size());
        assertEquals("value1", annotations.get("key1"));
        assertEquals("value2", annotations.get("key2"));

        Set<String> owners = k8sSecretService.owners;
        assertEquals(2, owners.size());
        assertTrue(owners.contains("owner1"));
        assertTrue(owners.contains("owner2"));

        Set<String> names = k8sSecretService.names;
        assertEquals(3, names.size());
        assertTrue(names.contains("name1"));
        assertTrue(names.contains("name2"));

    }


    @Test
    public void testGetItems() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        when(secretOperation.list()).thenReturn(secretList);
        List<Secret> items = k8sSecretService.getItems(namespace);
        assertEquals(1, items.size());
        assertEquals("*************", items.get(0).getData().get(key));
    }

    @Test
    public void testDecode() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        when(secretOperation.list()).thenReturn(secretList);
        String decodedValue = k8sSecretService.decode(namespace, secretId, key);
        assertEquals("admin", decodedValue);
    }

    @Test
    public void testDecodeSecretNotFound() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        when(secretOperation.list()).thenReturn(new SecretList());

        IllegalArgumentException result = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            k8sSecretService.decode(namespace, secretId, key);
        });

        assertEquals("No matching secret found", result.getMessage());
    }

    @Test
    public void testDecodeKeyNotFound() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        when(secretOperation.list()).thenReturn(secretList);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            k8sSecretService.decode(namespace, secretId, "invalidKey");
        });
    }

    @Test
    public void testAdd() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        when(secretOperation.resource(any(Secret.class))).thenReturn(secretResource);
        when(secretResource.create()).thenReturn(secret);
        when(secretOperation.list()).thenReturn(secretList);

        IdAwareResource<Secret> result = k8sSecretService.add(namespace, secretDTO);

        assertEquals(secretId, result.getId());
        assertEquals(secret, result.getResource());
    }

    @Test
    public void testAddWithData() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        Map<String, String> data = new HashMap<>();
        data.put(key, "testData");
        secretDTO.setData(data);

        when(secretOperation.resource(any(Secret.class))).thenReturn(secretResource);
        when(secretResource.create()).thenReturn(secret);
        when(secretOperation.list()).thenReturn(secretList);

        IdAwareResource<Secret> result = k8sSecretService.add(namespace, secretDTO);

        assertEquals(secretId, result.getId());
        assertNotNull(result.getResource().getData().get(key));
    }

    @Test
    public void testDelete() {
        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);

        lenient().when(resourceOperation.item()).thenReturn(null);
        lenient().when(resourceOperation.serverSideApply()).thenReturn(null);
        lenient().when(secretOperation.withName(anyString())).thenReturn(resourceOperation);

        K8SSecretService spyService = spy(k8sSecretService);
        doReturn(resourceCache).when(spyService).getResourceCache();

        spyService.delete(namespace, secretId);

        verify(resourceCache, times(1)).invalidate(namespace);
        verify(resourceOperation, times(1)).delete();
    }


    @Test
    public void testFetchWithOwners() throws Exception {
        ReflectionTestUtils.setField(k8sSecretService, "ownerFilters", "v1");
        k8sSecretService.initFilters();

        OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withApiVersion("v1")
                .withKind("Deployment")
                .withName("ownerName")
                .withUid(UUID.randomUUID().toString())
                .build();

        Secret secretWithOwner = new SecretBuilder()
                .withNewMetadata().withName(secretId).withNamespace(namespace)
                .withOwnerReferences(ownerReference).endMetadata()
                .addToData(key, encodedValue)
                .build();

        secretList.setItems(List.of(secretWithOwner));

        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);
        when(secretOperation.list()).thenReturn(secretList);

        Method fetchMethod = K8SSecretService.class.getDeclaredMethod("fetch", String.class);
        fetchMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Secret> secrets = (List<Secret>) fetchMethod.invoke(k8sSecretService, namespace);

        assertEquals(1, secrets.size());
        assertEquals(secretId, secrets.get(0).getMetadata().getName());
    }

    @Test
    public void testFetchWithAnnotations() throws Exception {
        ReflectionTestUtils.setField(k8sSecretService, "annotationFilters", "key1=value1");
        k8sSecretService.initFilters();

        Map<String, String> annotations = new HashMap<>();
        annotations.put("key1", "value1");

        Secret secretWithAnnotations = new SecretBuilder()
                .withNewMetadata().withName(secretId).withNamespace(namespace)
                .withAnnotations(annotations).endMetadata()
                .addToData(key, encodedValue)
                .build();

        secretList.setItems(List.of(secretWithAnnotations));

        when(client.secrets()).thenReturn(secretOperation);
        when(secretOperation.inNamespace(namespace)).thenReturn(secretOperation);
        when(secretOperation.list()).thenReturn(secretList);

        Method fetchMethod = K8SSecretService.class.getDeclaredMethod("fetch", String.class);
        fetchMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Secret> secrets = (List<Secret>) fetchMethod.invoke(k8sSecretService, namespace);

        assertEquals(1, secrets.size());
        assertEquals(secretId, secrets.get(0).getMetadata().getName());
    }
}
