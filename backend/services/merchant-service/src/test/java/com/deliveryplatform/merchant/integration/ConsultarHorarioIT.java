package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.support.EquipeDeTeste;
import com.deliveryplatform.merchant.support.IdentityDeMentira;
import com.deliveryplatform.merchant.support.Infraestrutura;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** {@code GET .../settings/horario} (ADR-073, emenda de 17/10/2026), de ponta a ponta no {@code merchant}. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "delivery.outbox.habilitado=false",
        "delivery.expediente.varredura-habilitada=false"})
class ConsultarHorarioIT extends Infraestrutura {

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

    @Autowired MembroRepositorio membros;
    @Autowired EstabelecimentoRepositorio lojas;

    private UUID loja;
    private UUID marli;
    private UUID bia;

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @BeforeEach
    void umaPizzariaSemHorario() {
        loja = lojas.salvar(LojaDeTeste.semHorario()).getId();
        marli = membros.salvar(Membro.fundador(UUID.randomUUID(), loja, agora())).getUsuarioId();
        bia = membros.salvar(Membro.colaborador(
                UUID.randomUUID(), loja, EquipeDeTeste.DA_BIA, agora())).getUsuarioId();
    }

    @Test
    @DisplayName("3 · loja de outra pessoa dá o mesmo que loja inexistente — corpo e status")
    void outra_loja_e_igual_a_inexistente() {
        UUID vizinha = lojas.salvar(LojaDeTeste.semHorario()).getId();
        membros.salvar(Membro.fundador(UUID.randomUUID(), vizinha, agora()));

        String deOutra = ler(vizinha, marli).expectStatus().isForbidden()
                .expectBody(String.class).returnResult().getResponseBody();
        String inexistente = ler(UUID.randomUUID(), marli).expectStatus().isForbidden()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(semInstance(deOutra)).isEqualTo(semInstance(inexistente));
    }

    /** Vazio é um horário, e não a ausência de um: a tela distingue "não abre" de "não consegui ler". */
    @Test
    @DisplayName("5 · loja que nunca abre por horário: 200 com o mapa vazio")
    void sem_horario_e_200_com_mapa_vazio() {
        String corpo = ler(loja, marli).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(corpo).isEqualTo("{\"horarioDeFuncionamento\":{}}");
    }

    @Test
    @DisplayName("4b · depois do PUT, o GET lê o que o PUT gravou")
    void le_o_que_o_put_gravou() {
        cliente().put().uri(caminho(loja))
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(marli))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(DefinirHorarioIT.SEXTA_A_NOITE)
                .exchange().expectStatus().isOk();

        String corpo = ler(loja, marli).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(corpo).contains("\"FRIDAY\"").contains("18:00").contains("23:00")
                .doesNotContain("MONDAY");
    }

    @Test
    @DisplayName("6 · sem token é 401; com token e sem CONFIGURAR_LOJA é 403, com o texto genérico")
    void sem_token_e_sem_permissao() {
        cliente().get().uri(caminho(loja)).exchange().expectStatus().isUnauthorized();

        ProblemDetail recusa = ler(loja, bia).expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
        assertThat(recusa.getDetail()).isEqualTo("sem acesso a este estabelecimento");
    }

    @Test
    @DisplayName("7 · o instance do ProblemDetail é o caminho do pedido, e nada mais")
    void instance_e_so_o_caminho() {
        ProblemDetail recusa = ler(loja, bia).expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getInstance()).isEqualTo(URI.create(caminho(loja)));
    }

    private static String caminho(UUID estabelecimento) {
        return "/api/v1/merchants/" + estabelecimento + "/settings/horario";
    }

    private RestTestClient.ResponseSpec ler(UUID estabelecimento, UUID quem) {
        return cliente().get().uri(caminho(estabelecimento))
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(quem))
                .exchange();
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private static String semInstance(String corpo) {
        return corpo.replaceAll("\"instance\":\"[^\"]*\"", "");
    }
}
