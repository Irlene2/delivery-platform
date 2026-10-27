package com.deliveryplatform.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sem requisito de segurança global, ao contrário do catalog: aqui há rotas públicas (o
 * cardápio pelo link), e cada controlador do painel declara o seu.
 */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI orderOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("order-service")
                        .version("v1")
                        .description("""
                                O pedido da entrega de 15/11 (ADR-077). As rotas /storefront são \
                                públicas — o cliente pede pelo link do cardápio, sem conta. As \
                                rotas /merchants exigem token, e a permissão é resolvida no \
                                merchant-service com o token de quem pediu (ADR-045)."""))
                .components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token do identity-service.")));
    }
}
