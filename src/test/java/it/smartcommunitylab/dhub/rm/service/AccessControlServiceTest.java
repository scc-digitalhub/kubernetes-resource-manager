package it.smartcommunitylab.dhub.rm.service;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import it.smartcommunitylab.dhub.rm.config.AuthenticationProperties;
import it.smartcommunitylab.dhub.rm.config.RoleProperties;
import it.smartcommunitylab.dhub.rm.model.IdAwareCustomResourceDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccessControlServiceTest {

    @Mock
    private AuthenticationProperties authenticationProperties;

    @Mock
    private RoleProperties roleProperties;

    @Mock
    private CustomResourceDefinitionService service;

    @InjectMocks
    private AccessControlService accessControlService;

    RoleProperties.RoleConfig role;
    List<RoleProperties.RoleConfig> roles;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        role = new RoleProperties.RoleConfig();
        role.setRole("default");
        role.setResources(Arrays.asList("k8s_job::read", "k8s_service::write"));

        roles = new ArrayList<>();
        roles.add(role);

        lenient().when(roleProperties.getRoles()).thenReturn(roles);

        ObjectMeta metadata = new ObjectMeta();
        metadata.setName("custom_resource");

        CustomResourceDefinition crd = new CustomResourceDefinition();
        crd.setMetadata(metadata);

        IdAwareCustomResourceDefinition resource = new IdAwareCustomResourceDefinition(crd);

        Page<IdAwareCustomResourceDefinition> page = new PageImpl<>(Collections.singletonList(resource));

        lenient().when(service.findAll(null, false, PageRequest.ofSize(1000))).thenReturn(page);
    }

    @Test
    public void testInitRoles() {
        accessControlService.initRoles();

        Map<String, Set<AccessControlService.RESOURCE_OP>> roleMap = accessControlService.getUserPermissions();
        assertTrue(roleMap.containsKey("k8s_job"));
        assertTrue(roleMap.get("k8s_job").contains(AccessControlService.RESOURCE_OP.read));
        assertTrue(roleMap.containsKey("k8s_service"));
        assertTrue(roleMap.get("k8s_service").contains(AccessControlService.RESOURCE_OP.write));

    }

    @Test
    public void testCanAccess_NoAuthScenario() {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(false);
        when(authenticationProperties.isOAuth2Enabled()).thenReturn(false);

        boolean canAccess = accessControlService.canAccess("some_resource", AccessControlService.RESOURCE_OP.read);
        assertTrue(canAccess);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanAccess_HasAccess() throws NoSuchFieldException, IllegalAccessException {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(true);
        lenient().when(authenticationProperties.isOAuth2Enabled()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((List) authorities);

        Field roleMapField = AccessControlService.class.getDeclaredField("roleMap");
        roleMapField.setAccessible(true);
        Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>> roleMap = (Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>>) roleMapField.get(accessControlService);

        Map<String, Set<AccessControlService.RESOURCE_OP>> userRoleMap = new HashMap<>();
        userRoleMap.put("some_resource", Collections.singleton(AccessControlService.RESOURCE_OP.read));
        roleMap.put("ROLE_USER", userRoleMap);

        boolean canAccess = accessControlService.canAccess("some_resource", AccessControlService.RESOURCE_OP.read);
        assertTrue(canAccess);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCanAccess_NoAccess() throws NoSuchFieldException, IllegalAccessException {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(true);
        lenient().when(authenticationProperties.isOAuth2Enabled()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((List) authorities);

        Field roleMapField = AccessControlService.class.getDeclaredField("roleMap");
        roleMapField.setAccessible(true);
        Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>> roleMap = (Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>>) roleMapField.get(accessControlService);

        Map<String, Set<AccessControlService.RESOURCE_OP>> userRoleMap = new HashMap<>();
        userRoleMap.put("some_resource", Collections.singleton(AccessControlService.RESOURCE_OP.read));
        roleMap.put("ROLE_USER", userRoleMap);

        boolean canAccess = accessControlService.canAccess("other_resource", AccessControlService.RESOURCE_OP.read);
        assertFalse(canAccess);
    }

    @SuppressWarnings("unchecked")
    private void setRoleMap(Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>> roleMap) throws NoSuchFieldException, IllegalAccessException {
        Field roleMapField = AccessControlService.class.getDeclaredField("roleMap");
        roleMapField.setAccessible(true);
        Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>> actualRoleMap = (Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>>) roleMapField.get(accessControlService);
        actualRoleMap.clear();
        actualRoleMap.putAll(roleMap);
    }

    @Test
    public void testGetUserPermissions_NoAuthScenario() {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(false);
        when(authenticationProperties.isOAuth2Enabled()).thenReturn(false);

        Map<String, Set<AccessControlService.RESOURCE_OP>> permissions = accessControlService.getUserPermissions();
        assertTrue(permissions.values().stream().allMatch(ops -> ops.contains(AccessControlService.RESOURCE_OP.all)));
    }

    @Test
    public void testGetUserPermissions_NoSecurityContext() {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(true);
        lenient().when(authenticationProperties.isOAuth2Enabled()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Map<String, Set<AccessControlService.RESOURCE_OP>> permissions = accessControlService.getUserPermissions();
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testGetUserPermissions_NoAuthentication() {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(true);
        lenient().when(authenticationProperties.isOAuth2Enabled()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        Map<String, Set<AccessControlService.RESOURCE_OP>> permissions = accessControlService.getUserPermissions();
        assertTrue(permissions.isEmpty());
    }

    @Test
    public void testGetUserPermissions_NoAuthorities() {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(true);
        lenient().when(authenticationProperties.isOAuth2Enabled()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

        Map<String, Set<AccessControlService.RESOURCE_OP>> permissions = accessControlService.getUserPermissions();
        assertTrue(permissions.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserPermissions_HasAuthorities() throws NoSuchFieldException, IllegalAccessException {
        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(true);
        lenient().when(authenticationProperties.isOAuth2Enabled()).thenReturn(true);

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getAuthorities()).thenReturn((List) authorities);

        Map<String, Map<String, Set<AccessControlService.RESOURCE_OP>>> roleMap = new HashMap<>();
        Map<String, Set<AccessControlService.RESOURCE_OP>> userRoleMap = new HashMap<>();
        userRoleMap.put("some_resource", Collections.singleton(AccessControlService.RESOURCE_OP.read));
        roleMap.put("ROLE_USER", userRoleMap);
        setRoleMap(roleMap);

        Map<String, Set<AccessControlService.RESOURCE_OP>> permissions = accessControlService.getUserPermissions();
        assertTrue(permissions.containsKey("some_resource"));
        assertTrue(permissions.get("some_resource").contains(AccessControlService.RESOURCE_OP.read));
    }



}
