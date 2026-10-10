package com.tt.financetracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI financeApi() {
        String scheme = "bearerAuth";
        return new OpenAPI().info(new Info().title("Mintwise Finance API").version("1.0.0")
                .description("Personal finance and investment tracker API"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes(scheme,
                        new SecurityScheme().name(scheme).type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
