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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A documentação viva com {@code delivery.docs.abertas=true} — ADR-050.
 *
 * <p>É a metade que prova alguma coisa: 200 <b>e</b> um corpo OpenAPI. O
 * {@link DocumentacaoFechadaIT} sozinho seria verde falso, porque sem token o
 * 401 vem antes de o roteamento decidir se a rota existe.
 *
 * <p>Classe própria, e não um caso a mais noutra: a propriedade muda a cadeia
 * de filtros, e o Spring guarda um contexto por configuração.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"delivery.outbox.habilitado=false", "delivery.docs.abertas=true"})
class DocumentacaoAbertaIT extends Infraestrutura {

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
    @DisplayName("o /v3/api-docs responde sem token — e responde OpenAPI")
    void o_documento_responde_sem_token() {
        String corpo = cliente().get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(corpo)
                .as("sem a chave, este teste não distinguiria 'está aberto' de 'a rota "
                        + "devolveu qualquer coisa'")
                .contains("\"openapi\"");
    }

    @Test
    @DisplayName("a página responde sem token — o springdoc-ui está no classpath")
    void a_pagina_responde_sem_token() {
        String corpo = cliente().get().uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(corpo).contains("swagger-ui");
    }

    @Test
    @DisplayName("abrir a documentação não abre o resto — a rota de negócio continua 401")
    void a_rota_de_negocio_continua_fechada() {
        cliente().get().uri("/api/v1/me/estabelecimentos")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
