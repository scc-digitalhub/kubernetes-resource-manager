package it.smartcommunitylab.dhub.rm.model;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdAwareCustomResourceDefinitionTest {

    @Test
    public void testConstructor() {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("test-crd");

        CustomResourceDefinition crd = new CustomResourceDefinition();
        crd.setMetadata(metadata);

        IdAwareCustomResourceDefinition idAwareCrd = new IdAwareCustomResourceDefinition(crd);

        assertEquals("test-crd", idAwareCrd.getId());
        assertEquals(crd, idAwareCrd.getCrd());
    }

    @Test
    public void testConstructorNullArgument() {
        Executable executable = () -> new IdAwareCustomResourceDefinition(null);
        assertThrows(IllegalArgumentException.class, executable);
    }

}

