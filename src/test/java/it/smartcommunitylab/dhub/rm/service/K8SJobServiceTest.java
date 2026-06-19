package it.smartcommunitylab.dhub.rm.service;

import java.util.Arrays;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class K8SJobServiceTest {

    @Mock
    KubernetesClient client;

    @Mock
    K8SAuthorizationService service;

    @Mock
    BatchAPIGroupDSL batchAPIGroupDSL;

    @Mock
    V1BatchAPIGroupDSL v1BatchAPIGroupDSL;

    @Mock
    MixedOperation<Job, JobList, ScalableResource<Job>> mixedOperation;

    @Mock
    ScalableResource<Job> scalableResource;

    @InjectMocks
    K8SJobService k8sJobService;

    @BeforeEach
    public void setup() {
        k8sJobService = new K8SJobService(client, service);
        MockitoAnnotations.openMocks(this);
        when(v1BatchAPIGroupDSL.jobs()).thenReturn(mixedOperation);
    }

    @Test
    public void testGetLog() {

        String namespace = "test-namespace";
        String deploymentName = "test-deployment";
        String expectedLog = "Log line 1\nLog line 2";

        when(mixedOperation.inNamespace(namespace)).thenReturn(mixedOperation);
        when(mixedOperation.withName(deploymentName)).thenReturn(scalableResource);
        when(scalableResource.getLog()).thenReturn(expectedLog);

        when(k8sJobService.getKubernetesClient().batch()).thenReturn(batchAPIGroupDSL);
        when(batchAPIGroupDSL.v1()).thenReturn(v1BatchAPIGroupDSL);

        List<String> result = k8sJobService.getLog(namespace, deploymentName);

        assertEquals(Arrays.asList("Log line 1", "Log line 2"), result);

    }

}