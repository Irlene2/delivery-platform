package com.deliveryplatform.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * O catalog que dá o preço na hora do pedido. Mais folga que a autorização: é uma chamada por
 * pedido, não por requisição, e o cardápio inteiro vem no corpo.
 */
@ConfigurationProperties(prefix = "delivery.catalogo")
public record CatalogoProperties(
        @DefaultValue("http://localhost:8083") String catalogUri,
        @DefaultValue("500ms") Duration tempoDeConexao,
        @DefaultValue("2s") Duration tempoDeLeitura
) {
}
