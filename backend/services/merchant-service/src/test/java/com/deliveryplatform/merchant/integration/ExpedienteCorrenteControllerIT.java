package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.api.dto.ExpedienteCorrenteResponse;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import com.deliveryplatform.merchant.support.IdentityDeMentira;
import com.deliveryplatform.merchant.support.Infraestrutura;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rota que a G-C2 vai chamar, atravessando a cadeia inteira.
 *
 * <h2>O que este arquivo prova, e o que ele deliberadamente não prova</h2>
 *
 * <p>Ele prova a <b>cadeia</b>: token, vínculo, carregamento da loja, os três
 * códigos de resposta e o formato da data no corpo — e, com o relógio fixo, o
 * <b>caso da Marli de ponta a ponta</b>: terça às 10h, a pizzaria de teste
 * fechada, e o carimbo sai terça, que é o dia da abertura das 18h.
 *
 * <p>O relógio é o {@code Clock} do serviço ({@code RelogioConfig}), trocado aqui
 * por um fixo marcado {@code @Primary}. Os casos que decidem a ADR-049 — a
 * madrugada, a loja que só abre às segundas — estão no
 * {@code CarimboDoExpedienteTest}, com instantes literais.
 *
 * <p>O caso do 409 <b>não depende do relógio</b>: uma loja sem horário nunca
 * abre, a qualquer hora. É por isso que ele é o caso mais forte deste arquivo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.outbox.habilitado=false")
class ExpedienteCorrenteControllerIT extends Infraestrutura {

    /** Terça, 29/09/2026, 10:00 em São Paulo. A pizzaria de teste abre terça às 18:00. */
    private static final Instant TERCA_AS_10H = LojaDeTeste.emSaoPaulo("2026-09-29T10:00");

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioFixo() {
            return Clock.fixed(TERCA_AS_10H, ZoneOffset.UTC);
        }
    }

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

    @Autowired
    MembroRepositorio membros;

    @Autowired
    EstabelecimentoRepositorio lojas;

    private UUID comHorario;
    private UUID semHorario;

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @BeforeEach
    void duasLojas() {
        comHorario = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        semHorario = lojas.salvar(LojaDeTeste.semHorario()).getId();
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec pedir(UUID loja, String token) {
        var requisicao = cliente().get()
                .uri("/internal/merchants/" + loja + "/expediente-corrente");
        if (token != null) {
            requisicao = requisicao.header("Authorization", "Bearer " + token);
        }
        return requisicao.exchange();
    }

    private UUID vinculada(UUID loja, EstadoDoMembro estado) {
        UUID pessoa = UUID.randomUUID();
        membros.salvar(Membro.reconstituir(UUID.randomUUID(), pessoa, loja,
                Papel.COLABORADOR, EnumSet.of(Permissao.VER_PRODUTO), estado, agora(), agora()));
        return pessoa;
    }

    // ── o caminho feliz ─────────────────────────────────────────────────────

    @Test
    @DisplayName("terça às 10h, a pizzaria fechada: 200, e o carimbo é terça — o da abertura das 18h")
    void devolve_uma_data() {
        UUID marli = vinculada(comHorario, EstadoDoMembro.ATIVO);

        ExpedienteCorrenteResponse corpo = pedir(comHorario, IDENTITY.tokenDe(marli))
                .expectStatus().isOk()
                .expectBody(ExpedienteCorrenteResponse.class)
                .returnResult().getResponseBody();

        assertThat(corpo).isNotNull();
        assertThat(corpo.estabelecimentoId()).isEqualTo(comHorario);
        assertThat(corpo.expedienteDeReferencia())
                .as("a loja está fechada e a próxima abertura é hoje às 18h — ADR-049. "
                        + "Com o dia da última abertura (sábado), a de hoje reativaria")
                .isEqualTo(LocalDate.parse("2026-09-29"));
    }

    @Test
    @DisplayName("a data sai como texto ISO, que é o formato que o catalog grava")
    void a_data_e_texto_iso() {
        UUID marli = vinculada(comHorario, EstadoDoMembro.ATIVO);

        String json = pedir(comHorario, IDENTITY.tokenDe(marli))
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(json)
                .as("o ProdutoDocumento do catalog grava expedienteDeReferencia como "
                        + "texto ISO de propósito; se esta rota serializasse um "
                        + "instante, o fuso voltaria pela porta que a G-B1 fechou")
                .containsPattern("\"expedienteDeReferencia\"\\s*:\\s*\"\\d{4}-\\d{2}-\\d{2}\"");
    }

    // ── a loja que não abre ─────────────────────────────────────────────────

    @Test
    @DisplayName("loja sem horário: 409 — e este caso não depende do relógio")
    void sem_horario_e_409() {
        UUID dono = vinculada(semHorario, EstadoDoMembro.ATIVO);

        pedir(semHorario, IDENTITY.tokenDe(dono))
                .expectStatus().isEqualTo(409);
    }

    // ── as recusas ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("sem vínculo: 403")
    void sem_vinculo_e_403() {
        pedir(comHorario, IDENTITY.tokenDe(UUID.randomUUID()))
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("vínculo SUSPENSO: 403 — quem foi afastado não pergunta mais nada da loja")
    void suspenso_e_403() {
        UUID afastada = vinculada(comHorario, EstadoDoMembro.SUSPENSO);

        // Terceiro lugar do repositório com filter(ativo) na mão. `.as(...)` é do
        // AssertJ e não existe no ResponseSpec — foi o que não compilou na G-B3.
        pedir(comHorario, IDENTITY.tokenDe(afastada))
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("loja inexistente: 403, e não 404 — a diferença enumeraria lojas")
    void loja_inexistente_e_403() {
        UUID qualquer = UUID.randomUUID();

        // Com 404 aqui e 403 ali, qualquer portador de token descobriria quais
        // identificadores de loja existem neste sistema.
        pedir(UUID.randomUUID(), IDENTITY.tokenDe(qualquer))
                .expectStatus().isForbidden();
    }

    @Test
    void sem_token_e_401() {
        pedir(comHorario, null).expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("sub que não é UUID: 403, pelo SujeitoDoToken")
    void sub_invalido_e_403() {
        pedir(comHorario, IDENTITY.tokenComSujeito("nao-sou-um-uuid"))
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("token de outro emissor: 401 — /internal/ passa pelos mesmos validadores")
    void outro_emissor_e_401() {
        pedir(comHorario, IDENTITY.tokenComEmissor("http://homologacao", UUID.randomUUID()))
                .expectStatus().isUnauthorized();
    }
}
