package com.deliveryplatform.identity.integration;

import com.deliveryplatform.identity.api.dto.CodigoDeDemonstracaoResponse;
import com.deliveryplatform.identity.api.dto.VerificationCodeRequest;
import com.deliveryplatform.identity.application.port.out.CodigoDeVerificacaoRepositorio;
import com.deliveryplatform.identity.domain.model.Telefone;
import com.deliveryplatform.identity.support.GeradorDeChaveDeTeste;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com a bandeira de desenvolvimento ligada, o 202 traz o código (emenda de 01/11/2026 à
 * ADR-042) — o mesmo que está na tabela, e nenhum para telefone que já tem conta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.cadastro.codigo-na-resposta=true")
@Testcontainers
class CodigoNaRespostaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void chaveDeAssinatura(DynamicPropertyRegistry registry) {
        registry.add("delivery.jwt.private-key-path", GeradorDeChaveDeTeste::caminhoDaChaveUnica);
    }

    @LocalServerPort
    int porta;

    @Autowired
    CodigoDeVerificacaoRepositorio codigos;

    @Test
    void o_codigo_da_resposta_e_o_da_tabela() {
        String telefone = "11922220001";

        CodigoDeDemonstracaoResponse resposta = RestTestClient
                .bindToServer().baseUrl("http://localhost:" + porta).build()
                .post().uri("/api/v1/auth/verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VerificationCodeRequest(telefone))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CodigoDeDemonstracaoResponse.class).returnResult().getResponseBody();

        assertThat(resposta).isNotNull();
        assertThat(resposta.codigoDeDemonstracao())
                .isEqualTo(codigos.buscarPorTelefone(Telefone.de(telefone)).orElseThrow().getCodigo());
    }
}
