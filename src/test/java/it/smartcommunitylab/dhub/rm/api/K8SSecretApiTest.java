package it.smartcommunitylab.dhub.rm.api;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import it.smartcommunitylab.dhub.rm.model.IdAwareResource;
import it.smartcommunitylab.dhub.rm.service.*;
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

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class K8SSecretApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private K8SSecretService service;

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

    private final String name = "secret";
    private final String namespace = "namespace";

    ObjectMeta meta;
    Secret secret;
    IdAwareResource<Secret> idAwareResource;

    @BeforeEach
    public void setup() {
        meta = new ObjectMeta();
        meta.setName(name);
        meta.setNamespace(namespace);

        secret = new Secret();
        secret.setMetadata(meta);

        idAwareResource = new IdAwareResource<>(secret);

        when(accessControlService.canAccess(anyString(), any())).thenReturn(true);

    }

    @Test
    @WithMockUser
    public void testFindAll() throws Exception {

        Page<IdAwareResource<Secret>> page = new PageImpl<>(
                Arrays.asList(idAwareResource),
                PageRequest.of(0, 10),
                1
        );

        when(service.findAll(anyString(), any(Collection.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/k8s_secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].metadata.name").value(name))
                .andExpect(jsonPath("$.content[0].metadata.namespace").value(namespace));

    }

    @Test
    @WithMockUser
    public void testFindById() throws Exception {

        when(service.findById(anyString(), anyString())).thenReturn(idAwareResource);

        mockMvc.perform(get("/api/k8s_secret/" + name)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.name").value(name));

    }

    @Test
    @WithMockUser
    public void testDecodeSecret() throws Exception {

        String key = "test-key";
        String decodedValue = "test-value";

        when(service.decode(anyString(), anyString(), anyString())).thenReturn(decodedValue);

        mockMvc.perform(get("/api/k8s_secret/" + name + "/decode/" + key)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.test-key").value(decodedValue));

    }

}
