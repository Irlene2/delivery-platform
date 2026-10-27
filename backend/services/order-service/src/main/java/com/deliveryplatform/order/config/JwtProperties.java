package com.deliveryplatform.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Emissor e audiência que o token tem de ter — os mesmos do catalog. */
@ConfigurationProperties(prefix = "delivery.jwt")
public record JwtProperties(String issuer, String audience) {
}
