package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.support.IdentityDeMentira;
import com.deliveryplatform.merchant.support.Infraestrutura;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * A documentação viva <b>sem</b> a propriedade — o padrão, e o que sobe em
 * qualquer lugar que não seja o computador de quem desenvolve (ADR-050).
 *
 * <p>Sozinha, esta classe é verde falso: sem token o 401 vem antes de o
 * roteamento decidir se a rota existe, e ela passaria mesmo sem o springdoc no
 * classpath. Vale junto do {@link DocumentacaoAbertaIT}, que prova que a rota
 * existe e responde OpenAPI quando aberta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.outbox.habilitado=false")
class DocumentacaoFechadaIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();

    @DynamicPropertySource
    static void apontarParaOIdentityDeMentira(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", IDENTITY::jwksUri);
        registry.add("delivery.jwt.issuer", () -> IdentityDeMentira.EMISSOR);
        registry.add("delivery.jwt.audience", () -> IdentityDeMentira.AUDIENCIA);
    }

    @AfterAll
    static void derrubar() {
        IDENTITY.close();
    }

    @LocalServerPort
    int porta;

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    @Test
    @DisplayName("sem a propriedade, o /v3/api-docs é 401 — o padrão fecha")
    void o_documento_sem_token_e_401() {
        cliente().get().uri("/v3/api-docs").exchange().expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("sem a propriedade, a página é 401")
    void a_pagina_sem_token_e_401() {
        cliente().get().uri("/swagger-ui/index.html").exchange().expectStatus().isUnauthorized();
    }
}
