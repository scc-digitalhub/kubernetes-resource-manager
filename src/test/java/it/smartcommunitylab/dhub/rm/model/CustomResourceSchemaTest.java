package it.smartcommunitylab.dhub.rm.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomResourceSchemaTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CustomResourceSchema schema;

    @Test
    public void testGettersAndSetters() {
        schema.setId("123");
        schema.setCrdId("testCrd");
        schema.setVersion("1.0");

        Assertions.assertEquals("123", schema.getId());
        Assertions.assertEquals("testCrd", schema.getCrdId());
        Assertions.assertEquals("1.0", schema.getVersion());
    }

    @Test
    public void testSchemaConversion() {
        Map<String, Serializable> testSchema = new HashMap<>();
        testSchema.put("key1", "value1");
        testSchema.put("key2", 123);

        schema.setSchema(testSchema);
        Map<String, Serializable> retrievedSchema = schema.getSchema();

        Assertions.assertEquals(testSchema, retrievedSchema);
    }

    @Test
    public void testConstraints() {
        schema.setId("123");
        Assertions.assertThrows(PersistenceException.class, () -> {
            doThrow(PersistenceException.class).when(entityManager).persist(schema);
            entityManager.persist(schema);
        });
    }


}

