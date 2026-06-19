package it.smartcommunitylab.dhub.rm.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/*
This is a custom configuration for Thymeleaf to be used in the testConsole() function inside the MainControllerTest.
The purpose of this configuration is to make Thymeleaf to ignore missing templates during the test.
This solution is used because in the console.html template there is a th:replace that asks for an index.html, that during the test is not possible to have
and so it creates an error. Since Thymeleaf processing isn't critical for my test and I only need to ensure that the model is correctly populated and the view name is correct,
I just ignore the missing template with this configuration.
Line 3: <head th:replace="~{index.html :: head}">
 */

@TestConfiguration
public class TestThymeleafConfig {

    @Bean
    @Primary
    public ITemplateResolver defaultTemplateResolver() {

        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);
        return resolver;

    }
}
