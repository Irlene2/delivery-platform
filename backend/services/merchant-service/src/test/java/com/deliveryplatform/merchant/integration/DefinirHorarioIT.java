package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Disponibilidade;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.Faixa;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Pausa;
import com.deliveryplatform.merchant.infrastructure.outbox.OutboxSpringDataRepository;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.net.URI;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code PUT /api/v1/merchants/{id}/settings/horario} (ADR-073), de ponta a ponta no
 * {@code merchant}: autorização, gravação, recusa traduzida — e a ausência de evento.
 *
 * <p>A varredura da ADR-046 está <b>desligada</b> aqui: ela publicaria a abertura sozinha
 * durante o teste, e o caso 15 — a rota não publica — ficaria verde ou vermelho conforme
 * o relógio da passada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "delivery.outbox.habilitado=false",
        "delivery.expediente.varredura-habilitada=false"})
class DefinirHorarioIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();

    static final String SEXTA_A_NOITE = """
            {"horarioDeFuncionamento":{"FRIDAY":[{"inicio":"18:00","fim":"23:00"}]}}""";

    /** Duas faixas por dia, a segunda cruzando a meia-noite: aberta a qualquer instante. */
    private static final String O_DIA_INTEIRO;

    static {
        StringBuilder corpo = new StringBuilder("{\"horarioDeFuncionamento\":{");
        for (DayOfWeek dia : DayOfWeek.values()) {
            corpo.append('"').append(dia).append("\":[{\"inicio\":\"00:00\",\"fim\":\"12:00\"},"
                    + "{\"inicio\":\"12:00\",\"fim\":\"00:00\"}]");
            corpo.append(dia == DayOfWeek.SUNDAY ? "" : ",");
        }
        O_DIA_INTEIRO = corpo.append("}}").toString();
    }

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
    @Autowired OutboxSpringDataRepository outbox;

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

    // ── B · o caso de uso, com o banco de verdade ───────────────────────────

    @Test
    @DisplayName("12 · loja de outra pessoa dá o mesmo que loja inexistente — corpo e status")
    void outra_loja_e_igual_a_inexistente() {
        UUID vizinha = lojas.salvar(LojaDeTeste.semHorario()).getId();
        membros.salvar(Membro.fundador(UUID.randomUUID(), vizinha, agora()));

        String deOutra = definir(vizinha, marli, SEXTA_A_NOITE)
                .expectStatus().isForbidden()
                .expectBody(String.class).returnResult().getResponseBody();
        String inexistente = definir(UUID.randomUUID(), marli, SEXTA_A_NOITE)
                .expectStatus().isForbidden()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(semInstance(deOutra)).isEqualTo(semInstance(inexistente));
    }

    @Test
    @DisplayName("13 e 16 · grava pelo repositório de verdade, e o que ficou é o corpo inteiro")
    void grava_o_corpo_inteiro() {
        definir(loja, marli, SEXTA_A_NOITE).expectStatus().isOk();

        Disponibilidade gravada = lojas.buscarPorId(loja).orElseThrow().getDisponibilidade();
        assertThat(gravada).isEqualTo(new Disponibilidade(
                Map.of(DayOfWeek.FRIDAY, List.of(Faixa.de("18:00", "23:00"))), Pausa.nenhuma()));
    }

    @Test
    @DisplayName("13b · substitui no banco: o dia que saiu não volta na leitura, e o vazio é semHorario")
    void substitui_no_banco() {
        definir(loja, marli, O_DIA_INTEIRO).expectStatus().isOk();
        definir(loja, marli, SEXTA_A_NOITE).expectStatus().isOk();
        assertThat(lojas.buscarPorId(loja).orElseThrow().getDisponibilidade().horarioDeFuncionamento())
                .containsOnlyKeys(DayOfWeek.FRIDAY);

        definir(loja, marli, "{\"horarioDeFuncionamento\":{}}").expectStatus().isOk();
        assertThat(lojas.buscarPorId(loja).orElseThrow().getDisponibilidade())
                .isEqualTo(Disponibilidade.semHorario());
    }

    /**
     * A decisão da ADR-073 §3, fixada: a abertura é da varredura. Alguém que "melhore" a
     * rota publicando o evento — com uma razão que soa boa — bate aqui.
     */
    @Test
    @DisplayName("15 · com a loja aberta neste instante, a rota não escreve nada no outbox")
    void nao_escreve_no_outbox() {
        assertThat(outbox.findAll()).noneMatch(linha -> loja.equals(linha.getAgregadoId()));

        definir(loja, marli, O_DIA_INTEIRO).expectStatus().isOk();

        Estabelecimento gravada = lojas.buscarPorId(loja).orElseThrow();
        assertThat(gravada.estaAberta(Instant.now()))
                .as("a premissa do caso: a loja está aberta agora")
                .isTrue();
        assertThat(outbox.findAll()).noneMatch(linha -> loja.equals(linha.getAgregadoId()));
    }

    // ── C · a rota ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("17 · 200, e o corpo devolve o horário como ficou — ordenado pelo domínio")
    void devolve_o_horario_como_ficou() {
        String fora_de_ordem = """
                {"horarioDeFuncionamento":{"MONDAY":[{"inicio":"18:00","fim":"23:00"},{"inicio":"11:00","fim":"14:00"}]}}""";

        Map<String, Object> resposta = definir(loja, marli, fora_de_ordem)
                .expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> porDia =
                (Map<String, List<Map<String, String>>>) resposta.get("horarioDeFuncionamento");
        assertThat(porDia).containsOnlyKeys("MONDAY");
        assertThat(porDia.get("MONDAY")).extracting(f -> LocalTime.parse(f.get("inicio")))
                .containsExactly(LocalTime.of(11, 0), LocalTime.of(18, 0));
        assertThat(porDia.get("MONDAY")).extracting(f -> LocalTime.parse(f.get("fim")))
                .containsExactly(LocalTime.of(14, 0), LocalTime.of(23, 0));
    }

    @Test
    @DisplayName("18 · sem token é 401; com token e sem CONFIGURAR_LOJA é 403, com o texto genérico")
    void sem_token_e_sem_permissao() {
        cliente().put().uri(caminho(loja))
                .contentType(MediaType.APPLICATION_JSON).body(SEXTA_A_NOITE)
                .exchange().expectStatus().isUnauthorized();

        ProblemDetail recusa = definir(loja, bia, SEXTA_A_NOITE)
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
        assertThat(recusa.getDetail()).isEqualTo("sem acesso a este estabelecimento");
        assertThat(lojas.buscarPorId(loja).orElseThrow().getDisponibilidade())
                .isEqualTo(Disponibilidade.semHorario());
    }

    @Test
    @DisplayName("19 · dia que não é dia da semana dá 400 na leitura do corpo, e nada é gravado")
    void dia_invalido_e_400() {
        definir(loja, marli, """
                {"horarioDeFuncionamento":{"SEGUNDA":[{"inicio":"18:00","fim":"23:00"}]}}""")
                .expectStatus().isBadRequest();

        assertThat(lojas.buscarPorId(loja).orElseThrow().getDisponibilidade())
                .isEqualTo(Disponibilidade.semHorario());
    }

    @Test
    @DisplayName("20 · a recusa vai à tela com a frase do tratador, igual — as duas recusas")
    void recusa_com_a_frase_do_tratador() {
        ProblemDetail ambigua = definir(loja, marli, """
                {"horarioDeFuncionamento":{"MONDAY":[{"inicio":"10:00","fim":"10:00"}]}}""")
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
        assertThat(ambigua.getDetail())
                .isEqualTo("uma faixa de horário não pode terminar na mesma hora em que começa");

        ProblemDetail repetida = definir(loja, marli, """
                {"horarioDeFuncionamento":{"MONDAY":[{"inicio":"11:00","fim":"14:00"},{"inicio":"11:00","fim":"14:00"}]}}""")
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
        assertThat(repetida.getDetail())
                .isEqualTo("a mesma faixa de horário aparece duas vezes no mesmo dia");
    }

    @Test
    @DisplayName("21 · o instance do ProblemDetail é o caminho do pedido, e nada do corpo")
    void instance_e_so_o_caminho() {
        ProblemDetail recusa = definir(loja, marli, """
                {"horarioDeFuncionamento":{"MONDAY":[{"inicio":"10:00","fim":"10:00"}]}}""")
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getInstance()).isEqualTo(URI.create(caminho(loja)));
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private static String caminho(UUID estabelecimento) {
        return "/api/v1/merchants/" + estabelecimento + "/settings/horario";
    }

    private RestTestClient.ResponseSpec definir(UUID estabelecimento, UUID quem, String corpo) {
        return cliente().put().uri(caminho(estabelecimento))
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .exchange();
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private static String semInstance(String corpo) {
        return corpo.replaceAll("\"instance\":\"[^\"]*\"", "");
    }
}
