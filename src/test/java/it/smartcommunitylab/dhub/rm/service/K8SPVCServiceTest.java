package it.smartcommunitylab.dhub.rm.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Map;

import com.google.common.cache.LoadingCache;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.BaseOperation;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.model.dto.PersistentVolumeClaimDTO;
import it.smartcommunitylab.dhub.rm.model.dto.PersistentVolumeClaimDTO.PVC_VOLUME_MODE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

@ExtendWith(MockitoExtension.class)
public class K8SPVCServiceTest {

    @Mock
    private KubernetesClient kubernetesClient;

    @Mock
    private K8SAuthorizationService authorizationService;

    @Mock
    private MixedOperation<PersistentVolumeClaim, PersistentVolumeClaimList, Resource<PersistentVolumeClaim>> persistentVolumeClaimOperation;

    @Mock
    private BaseOperation<PersistentVolumeClaim, PersistentVolumeClaimList, Resource<PersistentVolumeClaim>> namespaceOperation;

    @Mock
    private Resource<PersistentVolumeClaim> resourceOperation;

    @Mock
    private LoadingCache<String, Map<String, IdAwareResource<PersistentVolumeClaim>>> resourceCache;

    @InjectMocks
    private K8SPVCService k8sPVCService;

    @Value("${kubernetes.pvc.managed-by}")
    private String managedByLabel = "test-managed-by";

    @BeforeEach
    void setup() {
        k8sPVCService = new K8SPVCService(kubernetesClient, authorizationService);

        when(kubernetesClient.persistentVolumeClaims()).thenReturn(persistentVolumeClaimOperation);
        when(persistentVolumeClaimOperation.inNamespace(anyString())).thenReturn(namespaceOperation);
        lenient().when(namespaceOperation.resource(any(PersistentVolumeClaim.class))).thenReturn(resourceOperation);
        lenient().when(resourceOperation.item()).thenReturn(null);
        lenient().when(resourceOperation.serverSideApply()).thenReturn(null);

        lenient().when(namespaceOperation.withName(anyString())).thenReturn(resourceOperation);
    }

    @Test
    public void testAdd() {
        String namespace = "test-namespace";
        PersistentVolumeClaimDTO dto = new PersistentVolumeClaimDTO();
        dto.setName("test-pvc");
        dto.setStorageClassName("standard");
        dto.setVolumeMode(PVC_VOLUME_MODE.Filesystem);
        dto.setVolumeName("test-volume");
        dto.setAccessModes(Arrays.asList(PersistentVolumeClaimDTO.PVC_ACCESS_MODE.ReadWriteOnce));
        dto.setResourceAmount(10);

        PersistentVolumeClaim expectedPvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .addToLabels("app.kubernetes.io/managed-by", managedByLabel)
                .endMetadata()
                .withNewSpec()
                .withStorageClassName(dto.getStorageClassName())
                .withVolumeMode(dto.getVolumeMode().name())
                .withVolumeName(dto.getVolumeName())
                .withAccessModes(dto.getAccessModes().stream().map(am -> am.name()).toList())
                .withNewResources()
                .addToRequests("storage", new Quantity(dto.getResourceAmount() + "Gi"))
                .endResources()
                .endSpec()
                .build();

        IdAwareResource<PersistentVolumeClaim> idAwareResource = new IdAwareResource<>(expectedPvc);
        K8SPVCService spyService = spy(k8sPVCService);
        doReturn(idAwareResource).when(spyService).findById(namespace, dto.getName());

        IdAwareResource<PersistentVolumeClaim> result = spyService.add(namespace, dto);

        verify(resourceOperation, times(1)).item();
        verify(resourceOperation, times(1)).serverSideApply();
        Assertions.assertEquals(expectedPvc, result.getResource());
    }

    @Test
    public void testDelete() {
        String namespace = "test-namespace";
        String pvcId = "test-pvc";

        K8SPVCService spyService = spy(k8sPVCService);
        doReturn(resourceCache).when(spyService).getResourceCache();

        spyService.delete(namespace, pvcId);

        verify(resourceCache, times(1)).invalidate(namespace);
        verify(resourceOperation, times(1)).delete();
    }
}
