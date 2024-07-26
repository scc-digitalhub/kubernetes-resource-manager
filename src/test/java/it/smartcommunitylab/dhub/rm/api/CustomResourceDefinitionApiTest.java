package it.smartcommunitylab.dhub.rm.api;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import it.smartcommunitylab.dhub.rm.model.IdAwareCustomResourceDefinition;
import it.smartcommunitylab.dhub.rm.model.dto.CustomResourceSchemaDTO;
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

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomResourceDefinitionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomResourceDefinitionService customResourceDefinitionService;

    @MockBean
    private CustomResourceSchemaService schemaService;

    @MockBean
    private CustomResourceService customResourceService;

    @MockBean
    private K8SPVCService k8SPVCService;

    @MockBean
    private UserApi userApi;

    @MockBean
    private AccessControlService accessControlService;

    private final String name = "example.com";
    private final String crdId = "crs-dto.test";
    private final String version = "v1";

    ObjectMeta meta;
    CustomResourceDefinition customResource;
    IdAwareCustomResourceDefinition resource;
    CustomResourceSchemaDTO customResourceSchemaDTO;

    @BeforeEach
    public void setup() {
        meta = new ObjectMeta();
        meta.setName(name);
        customResource = new CustomResourceDefinition();
        customResource.setMetadata(meta);

        resource = new IdAwareCustomResourceDefinition(customResource);

        customResourceSchemaDTO = new CustomResourceSchemaDTO();
        customResourceSchemaDTO.setCrdId(crdId);
        customResourceSchemaDTO.setVersion(version);

        when(accessControlService.canAccess(anyString(), any())).thenReturn(true);


    }


    @Test
    @WithMockUser
    public void testFindAll() throws Exception {

        Page<IdAwareCustomResourceDefinition> page = new PageImpl<>(
                Arrays.asList(resource),
                PageRequest.of(0, 10),
                1
        );

        when(customResourceDefinitionService.findAll(any(Collection.class), anyBoolean(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/crd")
                        .param("id", name)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(name));

    }

    @Test
    @WithMockUser
    public void testFindById() throws Exception {

        when(customResourceDefinitionService.findById(name)).thenReturn(resource);

        System.out.println(customResourceDefinitionService.findById(name));

        mockMvc.perform(get("/api/crd/" + name)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.name").value(name));

    }

    @Test
    @WithMockUser
    public void testFindSchemaForId() throws Exception {

        when(customResourceDefinitionService.fetchStoredVersionName(crdId)).thenReturn(version);
        when(schemaService.findByCrdIdAndVersion(crdId, customResourceDefinitionService.fetchStoredVersionName(crdId))).thenReturn(customResourceSchemaDTO);

        mockMvc.perform(get("/api/crd/" + crdId + "/schema")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crdId").value(crdId));


    }

    @Test
    @WithMockUser
    public void testFindSchemasForId() throws Exception {

        Page<CustomResourceSchemaDTO> page = new PageImpl<>(
                Arrays.asList(customResourceSchemaDTO),
                PageRequest.of(0, 10),
                1
        );

        when(schemaService.findByCrdId(ArgumentMatchers.eq(crdId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/crd/" + crdId + "/schemas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].crdId").value(crdId));
    }

}
