package it.smartcommunitylab.dhub.rm.controller;

import it.smartcommunitylab.dhub.rm.SystemKeys;
import it.smartcommunitylab.dhub.rm.api.CustomResourceApi;
import it.smartcommunitylab.dhub.rm.api.K8SPVCApi;
import it.smartcommunitylab.dhub.rm.api.UserApi;
import it.smartcommunitylab.dhub.rm.config.ApplicationProperties;
import it.smartcommunitylab.dhub.rm.config.AuthenticationProperties;
import it.smartcommunitylab.dhub.rm.service.AccessControlService;
import it.smartcommunitylab.dhub.rm.service.CustomResourceSchemaService;
import it.smartcommunitylab.dhub.rm.service.CustomResourceService;
import it.smartcommunitylab.dhub.rm.service.K8SPVCService;
import it.smartcommunitylab.dhub.rm.controller.TestThymeleafConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestThymeleafConfig.class})
public class MainControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    CustomResourceApi customResourceApi;

    @MockBean
    CustomResourceService customResourceService;

    @MockBean
    CustomResourceSchemaService customResourceSchemaService;

    @MockBean
    private K8SPVCApi k8SPVCApi;

    @MockBean
    private K8SPVCService k8SPVCService;

    @MockBean
    AuthenticationProperties authenticationProperties;

    @MockBean
    ApplicationProperties applicationProperties;

    @MockBean
    private UserApi userApi;

    @MockBean
    private AccessControlService accessControlService;

    private static final String CONSOLE_CONTEXT = SystemKeys.CONSOLE_PATH;

    @Test
    public void testRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SystemKeys.CONSOLE_PATH + "/"));
    }

    @Test
    public void testConsole() throws Exception {

        String[] scopes = {"scope1", "scope2"};

        when(applicationProperties.getUrl()).thenReturn("http://example.com");

        AuthenticationProperties.OAuth2AuthenticationProperties oauth2 = new AuthenticationProperties.OAuth2AuthenticationProperties();
        oauth2.setIssuerUri("http://auth.example.com");
        oauth2.setAudience("client-id");
        oauth2.setScopes(scopes);

        when(authenticationProperties.isBasicAuthEnabled()).thenReturn(false);
        when(authenticationProperties.isOAuth2Enabled()).thenReturn(true);
        when(authenticationProperties.getOauth2()).thenReturn(oauth2);

        mockMvc.perform(get(CONSOLE_CONTEXT + "/**"))
                .andExpect(status().isOk())
                .andExpect(view().name("console.html"))
                .andExpect(model().attributeExists("config"))
                .andExpect(model().attribute("config", new HashMap<String, String>() {{
                    put("REACT_APP_APPLICATION_URL", "http://example.com");
                    put("REACT_APP_API_URL", "");
                    put("REACT_APP_CONTEXT_PATH", CONSOLE_CONTEXT);
                    put("REACT_APP_AUTH", "oauth2");
                    put("REACT_APP_AUTHORITY", "http://auth.example.com");
                    put("REACT_APP_CLIENT_ID", "client-id");
                    put("REACT_APP_SCOPE", "scope1 scope2");
                    put("REACT_APP_CORE_NAME", null);
                }}));
    }
}
