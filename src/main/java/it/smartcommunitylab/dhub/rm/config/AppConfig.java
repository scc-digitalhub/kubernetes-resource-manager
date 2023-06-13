package it.smartcommunitylab.dhub.rm.config;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

@Configuration
@Import(JacksonAutoConfiguration.class)
public class AppConfig {

    @Value("${kubernetes.config}")
    private Resource configPath;

    @Bean
    public KubernetesClient kubernetesClient() throws IOException {
        Assert.notNull(configPath, "Kubernetes config path must be provided");
        return new KubernetesClientBuilder().withConfig(configPath.getInputStream()).build();
    }

    @Bean
    @Qualifier("yamlObjectMapper")
    public YAMLMapper yamlMapper() {
        return new YAMLMapper();
    }
}
