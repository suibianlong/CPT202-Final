package com.cpt202.HerLink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI herLinkOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HerLink API Documentation")
                        .description("API documentation for the HerLink Community Heritage Resource Sharing and Curation Platform")
                        .version("1.0.0"));
    }
}
