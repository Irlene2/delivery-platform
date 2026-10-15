package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.out.ConviteRepositorio;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Convite;
import com.deliveryplatform.merchant.domain.model.EstadoDoConvite;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Permissao;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code POST .../team/convites} (ADR-069): o 201 devolve o token uma vez, e ele não
 * aparece em mais lugar nenhum.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.outbox.habilitado=false")
class ConvidarIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();

    static final String CORPO = """
            {"telefone":"%s","permissoes":[%s]}""";

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
    @Autowired ConviteRepositorio convites;

    private UUID loja;
    private UUID marli;
    private UUID junior;
    private UUID bia;

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @BeforeEach
    void umaPizzariaComTresPessoas() {
        loja = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        marli = membros.salvar(Membro.fundador(UUID.randomUUID(), loja, agora())).getUsuarioId();
        junior = membros.salvar(Membro.colaborador(
                UUID.randomUUID(), loja, EquipeDeTeste.DO_JUNIOR, agora())).getUsuarioId();
        bia = membros.salvar(Membro.colaborador(
                UUID.randomUUID(), loja, EquipeDeTeste.DA_BIA, agora())).getUsuarioId();
    }

    @Test
    @DisplayName("A1 · o 201 devolve o token, e o convite existe no banco, pendente")
    void convidar_devolve_o_token() {
        Map<String, Object> emitido = convidar(loja, marli, "(81) 98765-4321", "\"VER_PEDIDO\"")
                .expectStatus().isCreated()
                .expectBody(Map.class).returnResult().getResponseBody();

        String token = (String) emitido.get("token");
        assertThat(token).isNotBlank();
        Convite gravado = convites.buscarPorToken(token).orElseThrow();
        assertThat(gravado.getEstabelecimentoId()).isEqualTo(loja);
        assertThat(gravado.getEstado()).isEqualTo(EstadoDoConvite.PENDENTE);
        assertThat(gravado.getPermissoesOferecidas()).containsExactly(Permissao.VER_PEDIDO);
    }

    /**
     * O token é credencial, e se lê uma vez. A listagem de convites não existe; o
     * {@code GET .../team} existe, e não pode trazê-lo — nem hoje, nem "por simetria"
     * daqui a três rodadas.
     */
    @Test
    @DisplayName("A2 · o token aparece num lugar só: o GET da equipe não o contém")
    void o_token_nao_reaparece() {
        String token = tokenEmitido(marli);

        String equipe = cliente().get().uri("/api/v1/merchants/" + loja + "/team")
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(marli))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(equipe).doesNotContain(token);
    }

    @Test
    @DisplayName("A3 · sem GERENCIAR_EQUIPE é 403, com o texto genérico")
    void sem_a_permissao_e_403() {
        ProblemDetail recusa = convidar(loja, bia, "(81) 98765-4321", "\"VER_PEDIDO\"")
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo("sem acesso a este estabelecimento");
    }

    @Test
    @DisplayName("A4 · de outra loja — duas lojas no banco — é 403 idêntico a 'não existe' (M7)")
    void outra_loja_e_igual_a_inexistente() {
        UUID vizinha = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        membros.salvar(Membro.fundador(UUID.randomUUID(), vizinha, agora()));

        String deOutra = convidar(vizinha, marli, "(81) 98765-4321", "\"VER_PEDIDO\"")
                .expectStatus().isForbidden()
                .expectBody(String.class).returnResult().getResponseBody();
        String inexistente = convidar(UUID.randomUUID(), marli, "(81) 98765-4321", "\"VER_PEDIDO\"")
                .expectStatus().isForbidden()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(semInstante(deOutra)).isEqualTo(semInstante(inexistente));
        assertThat(convites.pendentesDe(vizinha)).isEmpty();
    }

    @Test
    @DisplayName("A5 · a validade é a do agregado, e não um prazo que o caso de uso inventou")
    void a_validade_e_do_agregado() {
        Convite gravado = convites.buscarPorToken(tokenEmitido(marli)).orElseThrow();

        assertThat(Duration.between(gravado.getCriadoEm(), gravado.getExpiraEm()))
                .isEqualTo(Convite.VALIDADE);
    }

    @Test
    @DisplayName("A2 na emissão: oferecer o que não tem é 400 com a frase, e nada é gravado")
    void oferecer_o_que_nao_tem_e_400() {
        ProblemDetail recusa = convidar(loja, junior, "(81) 98765-4321", "\"CRIAR_PRODUTO\"")
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo("você só pode oferecer permissões que você mesmo tem");
        assertThat(convites.pendentesDe(loja)).isEmpty();
    }

    @Test
    @DisplayName("telefone que não é telefone: 400 com a frase")
    void telefone_invalido_e_400() {
        ProblemDetail recusa = convidar(loja, marli, "abc", "\"VER_PEDIDO\"")
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo("telefone inválido");
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec convidar(UUID lojaDaUrl, UUID quem, String telefone,
                                                 String permissoes) {
        return cliente().post().uri("/api/v1/merchants/" + lojaDaUrl + "/team/convites")
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .body(CORPO.formatted(telefone, permissoes))
                .exchange();
    }

    @SuppressWarnings("unchecked")
    private String tokenEmitido(UUID quem) {
        Map<String, Object> emitido = convidar(loja, quem, "(81) 98765-4321", "\"VER_PEDIDO\"")
                .expectStatus().isCreated()
                .expectBody(Map.class).returnResult().getResponseBody();
        return (String) emitido.get("token");
    }

    /** O {@code instance} é o caminho de cada pedido; o resto do corpo tem de ser igual. */
    private static String semInstante(String corpo) {
        return corpo.replaceAll("\"instance\":\"[^\"]*\"", "");
    }
}
