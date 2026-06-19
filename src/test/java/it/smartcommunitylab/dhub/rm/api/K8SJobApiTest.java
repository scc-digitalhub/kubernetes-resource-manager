package it.smartcommunitylab.dhub.rm.api;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class K8SJobApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private K8SJobService service;

    @MockBean
    CustomResourceApi customResourceApi;

    @MockBean
    CustomResourceService customResourceService;

    @MockBean
    CustomResourceSchemaService customResourceSchemaService;

    @MockBean
    private K8SPVCApi k8SPVCApi;

    @MockBean
    private K8SPVCService k8SPVCService;

    @MockBean
    private UserApi userApi;

    @MockBean
    private AccessControlService accessControlService;

    private final String name = "job";
    private final String namespace = "namespace";

    ObjectMeta meta;
    Job job;
    IdAwareResource<Job> idAwareResource;

    @BeforeEach
    public void setup() {
        meta = new ObjectMeta();
        meta.setName(name);
        meta.setNamespace(namespace);

        job = new Job();
        job.setMetadata(meta);

        idAwareResource = new IdAwareResource<>(job);

        when(accessControlService.canAccess(anyString(), any())).thenReturn(true);

    }

    @Test
    @WithMockUser
    public void testFindAll() throws Exception {

        Page<IdAwareResource<Job>> page = new PageImpl<>(
                Arrays.asList(idAwareResource),
                PageRequest.of(0, 10),
                1
        );

        when(service.findAll(anyString(), any(Collection.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/k8s_job")
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

        mockMvc.perform(get("/api/k8s_job/" + name)
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

        mockMvc.perform(get("/api/k8s_job/" + name + "/log")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    public void testDelete() throws Exception {
        doNothing().when(service).delete(ArgumentMatchers.eq(namespace), ArgumentMatchers.eq(name));

        mockMvc.perform(delete("/api/k8s_job/" + name)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }


}
