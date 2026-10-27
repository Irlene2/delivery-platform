package com.deliveryplatform.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Swagger UI e /v3/api-docs sem token (ADR-050). Falso por padrão: esquecer fecha. */
@ConfigurationProperties(prefix = "delivery.docs")
public record DocsProperties(boolean abertas) {
}
