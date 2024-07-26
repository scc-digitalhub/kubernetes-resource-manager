package it.smartcommunitylab.dhub.rm.api;

import com.networknt.schema.ValidationMessage;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Status;
import it.smartcommunitylab.dhub.rm.model.IdAwareCustomResource;
import it.smartcommunitylab.dhub.rm.service.AccessControlService;
import it.smartcommunitylab.dhub.rm.service.CustomResourceSchemaService;
import it.smartcommunitylab.dhub.rm.service.CustomResourceService;
import it.smartcommunitylab.dhub.rm.service.K8SPVCService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import it.smartcommunitylab.dhub.rm.exception.ValidationException;
import io.fabric8.kubernetes.client.KubernetesClientException;


import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomResourceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomResourceService customResourceService;

    @MockBean
    private CustomResourceSchemaService customResourceSchemaService;

    @MockBean
    private K8SPVCService k8sPVCService;

    @MockBean
    private UserApi userApi;

    @MockBean
    private AccessControlService accessControlService;

    private final String crdId = "example-crd.id";
    private final String namespace = "default";
    private final String name = "resource1";
    private final String id = "1";

    ObjectMeta meta;
    GenericKubernetesResource genericResource;
    IdAwareCustomResource resource;

    @BeforeEach
    public void setup() {

        meta = new ObjectMeta();
        meta.setName(name);
        genericResource = new GenericKubernetesResource();
        genericResource.setMetadata(meta);

        resource = new IdAwareCustomResource(genericResource);
        resource.setId(id);

        when(accessControlService.canAccess(anyString(), any())).thenReturn(true);

    }

    @Test
    @WithMockUser
    public void testFindAll() throws Exception {

        Page<IdAwareCustomResource> page = new PageImpl<>(
                Arrays.asList(resource),
                PageRequest.of(0, 10),
                1
        );

        when(customResourceService.findAll(ArgumentMatchers.eq(crdId), ArgumentMatchers.eq(namespace), any(Collection.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/" + crdId)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("1"))
                .andExpect(jsonPath("$.content[0].metadata.name").value(name));
    }

    @Test
    @WithMockUser
    public void testFindById() throws Exception {

        when(customResourceService.findById(crdId, id, namespace)).thenReturn(resource);

        mockMvc.perform(get("/api/" + crdId + "/" + id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.metadata.name").value(name));

    }

    @Test
    @WithMockUser
    public void testAdd() throws Exception {

        when(customResourceService.add(ArgumentMatchers.eq(crdId), any(IdAwareCustomResource.class), ArgumentMatchers.eq(namespace)))
                .thenReturn(resource);

        mockMvc.perform(post("/api/" + crdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\", \"metadata\":{\"name\":\"" + name +"\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.metadata.name").value(name));

    }

    @Test
    @WithMockUser
    public void testUpdate() throws Exception {

        when(customResourceService.update(ArgumentMatchers.eq(crdId), ArgumentMatchers.eq(id), any(IdAwareCustomResource.class), ArgumentMatchers.eq(namespace)))
                .thenReturn(resource);

        mockMvc.perform(put("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + id + "\", \"metadata\":{\"name\":\"" + name +"\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.metadata.name").value(name));

    }

    @Test
    @WithMockUser
    public void testDelete() throws Exception {
        doNothing().when(customResourceService).delete(ArgumentMatchers.eq(crdId), ArgumentMatchers.eq(id), ArgumentMatchers.eq(namespace));

        mockMvc.perform(delete("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    //Test Exceptions
    @Test
    @WithMockUser
    public void testFindByIdNotFound() throws Exception {
        when(customResourceService.findById(crdId, id, namespace)).thenThrow(new NoSuchElementException("Resource not found"));

        mockMvc.perform(get("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    @WithMockUser
    public void testAccessDeniedException() throws Exception {
        when(customResourceService.findById(crdId, id, namespace))
                .thenThrow(new AccessDeniedException("Access is denied"));

        mockMvc.perform(get("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access is denied"));
    }

    @Test
    @WithMockUser
    public void testDataIntegrityViolationException() throws Exception {
        when(customResourceService.findById(crdId, id, namespace))
                .thenThrow(new DataIntegrityViolationException("Data integrity violation"));

        mockMvc.perform(get("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Data integrity violation"));
    }

    @Test
    @WithMockUser
    public void testValidationException() throws Exception {
        ValidationMessage validationMessage = new ValidationMessage.Builder()
                .build();

        Set<ValidationMessage> set =  new HashSet<>();
        set.add(validationMessage);

        when(customResourceService.findById(crdId, id, namespace))
                .thenThrow(new ValidationException(set));

        mockMvc.perform(get("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser
    public void testKubernetesClientException() throws Exception {
        Status status = new Status();
        status.setMessage("Error occurred in Kubernetes client");
        status.setCode(400);

        KubernetesClientException kubernetesClientException = new KubernetesClientException(status);

        when(customResourceService.findById(crdId, id, namespace))
                .thenThrow(kubernetesClientException);

        mockMvc.perform(get("/api/" + crdId + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Error occurred in Kubernetes client"));
    }



}
