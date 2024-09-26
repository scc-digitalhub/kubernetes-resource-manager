package it.smartcommunitylab.dhub.rm.model;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdAwareCustomResourceTest {

    @Mock
    GenericKubernetesResource cr;

    @InjectMocks
    IdAwareCustomResource idAwareCr;

    @Test
    public void testConstructor() {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("test-crd");

        cr = new GenericKubernetesResource();
        cr.setMetadata(metadata);

        idAwareCr = new IdAwareCustomResource(cr);

        assertEquals("test-crd", idAwareCr.getId());
        assertEquals(cr, idAwareCr.getCr());
    }

}