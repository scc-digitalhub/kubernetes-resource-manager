package it.smartcommunitylab.dhub.rm.model.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustomResourceSchemaDTOTest {

    @Mock
    private JsonNode mockJsonNode;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    private CustomResourceSchemaDTO customResourceSchemaDTO;

    @BeforeEach
    public void setUp() {
        customResourceSchemaDTO = new CustomResourceSchemaDTO();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGetAndSetId() {
        customResourceSchemaDTO.setId("testId");
        assertEquals("testId", customResourceSchemaDTO.getId());
    }

    @Test
    public void testGetAndSetCrdId() {
        customResourceSchemaDTO.setCrdId("crdId");
        assertEquals("crdId", customResourceSchemaDTO.getCrdId());
    }

    @Test
    public void testGetAndSetVersion() {
        customResourceSchemaDTO.setVersion("1.0");
        assertEquals("1.0", customResourceSchemaDTO.getVersion());
    }

    @Test
    public void testGetAndSetSchema() {
        customResourceSchemaDTO.setSchema(mockJsonNode);
        assertEquals(mockJsonNode, customResourceSchemaDTO.getSchema());
    }

    @Test
    public void testGetSchemaAsString() throws JsonProcessingException {
        customResourceSchemaDTO.setSchema(mockJsonNode);
        assertEquals(objectMapper.writeValueAsString(mockJsonNode), customResourceSchemaDTO.getSchemaAsString());
    }

    @Test
    public void testSetSchemaAsString() throws JsonProcessingException {
        String jsonString = "{\"key\": \"value\"}";
        customResourceSchemaDTO.setSchemaAsString(jsonString);
        JsonNode expectedJsonNode = objectMapper.readTree(jsonString);
        assertEquals(expectedJsonNode, customResourceSchemaDTO.getSchema());
    }

    @Test
    public void testSetSchemaAsString_nullOrEmpty() throws JsonProcessingException {
        customResourceSchemaDTO.setSchemaAsString(null);
        assertNull(customResourceSchemaDTO.getSchema());

        customResourceSchemaDTO.setSchemaAsString("");
        assertNull(customResourceSchemaDTO.getSchema());
    }
}
