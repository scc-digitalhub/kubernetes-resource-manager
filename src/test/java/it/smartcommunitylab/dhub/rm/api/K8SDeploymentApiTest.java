package it.smartcommunitylab.dhub.rm.api;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.service.AccessControlService;
import it.smartcommunitylab.dhub.rm.service.CustomResourceSchemaService;
import it.smartcommunitylab.dhub.rm.service.K8SDeploymentService;
import it.smartcommunitylab.dhub.rm.service.K8SPVCService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class K8SDeploymentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private K8SDeploymentService service;

    @MockBean
    private CustomResourceApi customResourceApi;

    @MockBean
    private CustomResourceDefinitionApi customResourceDefinitionApi;

    @MockBean
    private CustomResourceSchemaService customResourceSchemaService;

    @MockBean
    private K8SPVCApi k8SPVCApi;

    @MockBean
    private K8SPVCService k8SPVCService;

    @MockBean
    private UserApi userApi;

    @MockBean
    private AccessControlService accessControlService;

    private final String name = "deployment";
    private final String namespace = "namespace";

    Deployment deployment;
    IdAwareResource<Deployment> idAwareResource;

    @BeforeEach
    public void setup() {

        deployment = new DeploymentBuilder()
                .withNewMetadata().withName(name).withNamespace(namespace).endMetadata()
                .build();

        idAwareResource = new IdAwareResource<>(deployment);

        when(accessControlService.canAccess(anyString(), any())).thenReturn(true);


    }

    @Test
    @WithMockUser
    public void testFindAll() throws Exception {

        Page<IdAwareResource<Deployment>> page = new PageImpl<>(
                Arrays.asList(idAwareResource),
                PageRequest.of(0, 10),
                1
        );

        when(service.findAll(anyString(), any(Collection.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/k8s_deployment")
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].metadata.name").value(name))
                .andExpect(jsonPath("$.content[0].metadata.namespace").value(namespace));

    }

    @Test
    @WithMockUser
    public void testFindById() throws Exception {

        when(service.findById(anyString(), anyString())).thenReturn(idAwareResource);

        mockMvc.perform(get("/api/k8s_deployment/" + name)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.name").value(name));

    }

    @Test
    @WithMockUser
    public void testGetLog() throws Exception {

        List<String> list = new ArrayList<>();
        list.add("log1");
        list.add("log2");

        when(service.getLog(anyString(), anyString())).thenReturn(list);

        mockMvc.perform(get("/api/k8s_deployment/" + name + "/log")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

}
