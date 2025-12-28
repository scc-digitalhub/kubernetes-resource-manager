package it.smartcommunitylab.dhub.rm.model;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdAwareResourceTest {

    private HasMetadata mockHasMetadata;

    private final String name = "test-crd";

    @BeforeEach
    public void setUp() {

        mockHasMetadata = mock(HasMetadata.class);
        ObjectMeta mockObjectMeta = mock(ObjectMeta.class);

        when(mockHasMetadata.getMetadata()).thenReturn(mockObjectMeta);
        when(mockObjectMeta.getName()).thenReturn(name);
    }

    @Test
    public void testConstructor() {
        IdAwareResource<HasMetadata> idAwareResource = new IdAwareResource<>(mockHasMetadata);

        assertNotNull(idAwareResource);
        assertEquals(name, idAwareResource.getId());
        assertEquals(mockHasMetadata, idAwareResource.getResource());
    }

    @Test
    public void testSetIdAndSetResource() {
        IdAwareResource<HasMetadata> idAwareResource = new IdAwareResource<>(mockHasMetadata);
        String newName = "new-crd";
        idAwareResource.setId(newName);

        assertEquals(newName, idAwareResource.getId());

        HasMetadata newMockHasMetadata = mock(HasMetadata.class);
        ObjectMeta newMockObjectMeta = mock(ObjectMeta.class);
        when(newMockHasMetadata.getMetadata()).thenReturn(newMockObjectMeta);
        when(newMockObjectMeta.getName()).thenReturn(newName);

        idAwareResource.setResource(newMockHasMetadata);

        assertEquals(newMockHasMetadata, idAwareResource.getResource());
        assertEquals(newName, idAwareResource.getId());

    }
}
