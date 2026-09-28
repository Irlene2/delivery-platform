package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Equipe;
import com.deliveryplatform.merchant.domain.model.Membro;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A primeira rota interna do repositório, pela borda HTTP, com o mesmo arranjo
 * do {@code EquipeControllerIT}: porta aleatória, {@code RestTestClient}, e o
 * {@code IdentityDeMentira} publicando um JWKS de verdade — para que
 * {@code /internal/} passe pelos mesmos validadores de emissor e audiência que
 * {@code /api/v1/}.
 *
 * <p>Os casos que importam são o 2 e o 5. O 2 prova, na borda, que não há jeito
 * de a rota falar de outra pessoa: o {@code usuarioId} da resposta é o do token,
 * e não o de outro membro da mesma loja. O 5 compara o <b>corpo</b> da loja
 * inexistente com o do vínculo suspenso, e não só o status — é onde M7 vive.
 *
 * <p>Sem limpeza de banco, como no {@code EquipeControllerIT}: cada teste cria
 * a própria loja com identificadores novos, e nenhuma asserção olha a tabela
 * inteira.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "delivery.outbox.habilitado=false",
        "delivery.expediente.varredura-habilitada=false"
})
class ContextoDeAcessoControllerIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();

    private static final Set<Permissao> DUAS = EnumSet.of(Permissao.VER_PEDIDO, Permissao.ALTERAR_STATUS);

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
    @Autowired ObjectMapper json;

    private UUID loja;
    private UUID marli;
    private UUID bia;

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @BeforeEach
    void umaPizzariaComDuasPessoas() {
        loja = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        marli = membros.salvar(Membro.fundador(UUID.randomUUID(), loja, agora())).getUsuarioId();
        bia = membros.salvar(Membro.colaborador(UUID.randomUUID(), loja, DUAS, agora()))
                .getUsuarioId();
    }

    private RestTestClient.ResponseSpec pedirContexto(UUID lojaDaUrl, String token) {
        var requisicao = RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build()
                .get().uri("/internal/merchants/" + lojaDaUrl + "/me/contexto-de-acesso");
        if (token != null) {
            requisicao = requisicao.header("Authorization", "Bearer " + token);
        }
        return requisicao.exchange();
    }

    private Map<String, Object> corpo(RestTestClient.ResponseSpec resposta, int status) {
        String texto = resposta.expectStatus().isEqualTo(status)
                .expectBody(String.class).returnResult().getResponseBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> lido = json.readValue(texto, Map.class);
        return lido;
    }

    private void suspenderBia() {
        Membro autora = membros.buscarPorUsuarioELoja(marli, loja).orElseThrow();
        Membro alvo = membros.buscarPorUsuarioELoja(bia, loja).orElseThrow();
        Equipe.de(loja, List.of(autora, alvo)).suspender(autora, alvo, agora());
        membros.salvar(alvo);
    }

    // ── o caminho feliz ─────────────────────────────────────────────────────

    @Test
    @DisplayName("1 · colaboradora ativa com duas permissões: 200, e só os quatro campos")
    void vinculo_ativo_responde_os_quatro_campos() {
        Map<String, Object> contexto = corpo(pedirContexto(loja, IDENTITY.tokenDe(bia)), 200);

        assertThat(contexto.keySet())
                .as("todo campo a mais vira contrato congelado (ADR-039); estado, membroId e "
                        + "criadoEm ficam de fora de propósito")
                .containsExactlyInAnyOrder("usuarioId", "estabelecimentoId", "papel", "permissoes");
        assertThat(contexto.get("estabelecimentoId")).isEqualTo(loja.toString());
        assertThat(contexto.get("papel")).isEqualTo("COLABORADOR");
        assertThat((List<Object>) contexto.get("permissoes"))
                .containsExactlyInAnyOrder("VER_PEDIDO", "ALTERAR_STATUS");
    }

    @Test
    @DisplayName("2 · o usuarioId da resposta é o do token — a rota não fala de outra pessoa")
    void o_usuario_da_resposta_e_o_do_token() {
        Map<String, Object> daBia = corpo(pedirContexto(loja, IDENTITY.tokenDe(bia)), 200);
        Map<String, Object> daMarli = corpo(pedirContexto(loja, IDENTITY.tokenDe(marli)), 200);

        assertThat(daBia.get("usuarioId")).isEqualTo(bia.toString());
        assertThat(daMarli.get("usuarioId"))
                .as("mesma loja, mesmo caminho, outro token: a resposta muda com o portador, "
                        + "porque não existe outro jeito de dizer de quem se quer saber")
                .isEqualTo(marli.toString());
        assertThat(daMarli.get("papel")).isEqualTo("ADMINISTRADOR");
    }

    // ── as recusas ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("3 · token de A, loja onde só B tem vínculo: 403")
    void sem_vinculo_na_loja_e_403() {
        UUID outraLoja = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        membros.salvar(Membro.fundador(UUID.randomUUID(), outraLoja, agora()));

        pedirContexto(outraLoja, IDENTITY.tokenDe(bia)).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("4 · vínculo SUSPENSO: 403 — o repositório o devolve, e o filtro recusa")
    void vinculo_suspenso_e_403() {
        suspenderBia();

        pedirContexto(loja, IDENTITY.tokenDe(bia)).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("5 · loja que não existe: 403 com o MESMO corpo do vínculo suspenso (M7)")
    void loja_inexistente_devolve_o_mesmo_corpo_do_suspenso() {
        suspenderBia();

        UUID lojaQueNaoExiste = UUID.randomUUID();
        Map<String, Object> suspenso = corpo(pedirContexto(loja, IDENTITY.tokenDe(bia)), 403);
        Map<String, Object> inexistente = corpo(pedirContexto(lojaQueNaoExiste, IDENTITY.tokenDe(marli)), 403);

        // O `instance` do ProblemDetail é o caminho da requisição, e o Spring o
        // preenche sozinho: ele difere entre os dois pedidos porque os dois
        // pediram caminhos diferentes. É eco do que o chamador mandou, não
        // informação sobre a loja — e é por isso que ele é conferido à parte,
        // contra o próprio caminho, em vez de ser simplesmente ignorado.
        assertThat(inexistente.get("instance"))
                .isEqualTo("/internal/merchants/" + lojaQueNaoExiste + "/me/contexto-de-acesso");
        assertThat(suspenso.get("instance"))
                .isEqualTo("/internal/merchants/" + loja + "/me/contexto-de-acesso");

        inexistente.remove("instance");
        suspenso.remove("instance");
        assertThat(inexistente)
                .as("corpos diferentes diriam quais identificadores são lojas de verdade, e em "
                        + "quais o portador já teve acesso — um scanner escrito em JSON")
                .isEqualTo(suspenso);
    }

    // ── a mesma cadeia de filtros de /api/v1/ ───────────────────────────────

    @Test
    @DisplayName("6 · sem Authorization: 401")
    void sem_token_e_401() {
        pedirContexto(loja, null).expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("7 · token de outro emissor: 401 — /internal/ passa pelos mesmos validadores")
    void token_de_outro_emissor_e_401() {
        pedirContexto(loja, IDENTITY.tokenComEmissor("http://homologacao", marli))
                .expectStatus().isUnauthorized();
    }
}
