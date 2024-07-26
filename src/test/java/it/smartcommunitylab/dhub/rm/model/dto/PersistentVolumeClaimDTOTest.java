package it.smartcommunitylab.dhub.rm.model.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class PersistentVolumeClaimDTOTest {

    @InjectMocks
    PersistentVolumeClaimDTO persistentVolumeClaimDTO;

    @BeforeEach
    public void setUp() {
        persistentVolumeClaimDTO = new PersistentVolumeClaimDTO();
    }

    @Test
    public void testGetAndSetName() {
        persistentVolumeClaimDTO.setName("testName");
        assertEquals("testName", persistentVolumeClaimDTO.getName());
    }

    @Test
    public void testGetAndSetAccessModes() {
        List<PersistentVolumeClaimDTO.PVC_ACCESS_MODE> accessModes = Arrays.asList(
                PersistentVolumeClaimDTO.PVC_ACCESS_MODE.ReadWriteOnce,
                PersistentVolumeClaimDTO.PVC_ACCESS_MODE.ReadOnlyMany
        );
        persistentVolumeClaimDTO.setAccessModes(accessModes);
        assertEquals(accessModes, persistentVolumeClaimDTO.getAccessModes());
    }

    @Test
    public void testGetAndSetResourceAmount() {
        persistentVolumeClaimDTO.setResourceAmount(1024);
        assertEquals(1024, persistentVolumeClaimDTO.getResourceAmount());
    }

    @Test
    public void testGetAndSetStorageClassName() {
        persistentVolumeClaimDTO.setStorageClassName("fast-storage");
        assertEquals("fast-storage", persistentVolumeClaimDTO.getStorageClassName());
    }

    @Test
    public void testGetAndSetVolumeMode() {
        persistentVolumeClaimDTO.setVolumeMode(PersistentVolumeClaimDTO.PVC_VOLUME_MODE.Block);
        assertEquals(PersistentVolumeClaimDTO.PVC_VOLUME_MODE.Block, persistentVolumeClaimDTO.getVolumeMode());
    }

    @Test
    public void testGetAndSetVolumeName() {
        persistentVolumeClaimDTO.setVolumeName("volume-1");
        assertEquals("volume-1", persistentVolumeClaimDTO.getVolumeName());
    }
}
