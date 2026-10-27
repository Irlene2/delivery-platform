package com.deliveryplatform.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** O merchant que responde a autorização, e os 300 ms da tabela da ADR-011. */
@ConfigurationProperties(prefix = "delivery.autorizacao")
public record AutorizacaoProperties(
        @DefaultValue("http://localhost:8082") String merchantUri,
        @DefaultValue("300ms") Duration tempoDeConexao,
        @DefaultValue("300ms") Duration tempoDeLeitura
) {
}
