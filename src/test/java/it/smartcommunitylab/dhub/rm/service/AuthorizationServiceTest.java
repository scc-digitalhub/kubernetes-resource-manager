package it.smartcommunitylab.dhub.rm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuthorizationServiceTest {

    @InjectMocks
    private K8SAuthorizationService authorizationService;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        setPrivateField(authorizationService, "allowedCrds", Arrays.asList("crd1", "crd2"));
        setPrivateField(authorizationService, "deniedCrds", Arrays.asList("crd3", "crd4"));
        setPrivateField(authorizationService, "serviceSelector", "serviceSelector");
        setPrivateField(authorizationService, "deploymentSelector", "deploymentSelector");
        setPrivateField(authorizationService, "jobSelector", "jobSelector");
        setPrivateField(authorizationService, "pvcSelector", "pvcSelector");
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testIsCrdAllowed() {
        assertTrue(authorizationService.isCrdAllowed("crd1"));
        assertFalse(authorizationService.isCrdAllowed("crd3"));
        assertFalse(authorizationService.isCrdAllowed("crd5"));
    }

    @Test
    public void testSelectors() {
        assertEquals("serviceSelector", authorizationService.getServiceSelector());
        assertEquals("deploymentSelector", authorizationService.getDeploymentSelector());
        assertEquals("jobSelector", authorizationService.getJobSelector());
        assertEquals("pvcSelector", authorizationService.getPVCSelector());
    }
}
