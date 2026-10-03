package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.GrupoDeOpcoes;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Opcao;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.IdentityDeMentira;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.MerchantDeMentira;
import com.deliveryplatform.catalog.support.Precos;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A primeira requisição autenticada e autorizada do {@code catalog-service} —
 * e a primeira do repositório cuja autorização atravessa <b>dois</b> serviços.
 *
 * <p>Ela percorre: a cadeia de filtros do {@code catalog}, a busca do JWKS pela
 * rede, a validação de assinatura, emissor, audiência e tempo, o
 * encaminhamento do token ao {@code merchant}, a tradução da resposta, a
 * conferência de {@code VER_PRODUTO}, a consulta paginada no MongoDB e a
 * montagem do resumo. <b>Nada disso tinha sido percorrido por uma requisição
 * de verdade neste serviço.</b>
 *
 * <p>Os quatro casos de recusa devolvem <b>a mesma coisa</b> — sem vínculo, sem
 * permissão, {@code merchant} com erro e {@code merchant} calado. É M7 levado a
 * sério: pela borda, um scanner não distingue "a loja não é sua" de "o sistema
 * está com problema".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProdutoControllerIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();
    private static final MerchantDeMentira MERCHANT = MerchantDeMentira.subir();

    @DynamicPropertySource
    static void apontarParaOsDublês(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", IDENTITY::jwksUri);
        registry.add("delivery.jwt.issuer", () -> IdentityDeMentira.EMISSOR);
        registry.add("delivery.jwt.audience", () -> IdentityDeMentira.AUDIENCIA);
        registry.add("delivery.autorizacao.merchant-uri", MERCHANT::uri);
        registry.add("delivery.autorizacao.tempo-de-leitura", () -> "300ms");
    }

    @AfterAll
    static void derrubar() {
        IDENTITY.close();
        MERCHANT.close();
    }

    @LocalServerPort
    int porta;

    @Autowired ProdutoRepositorio produtos;
    @Autowired MongoTemplate mongo;

    private UUID loja;
    private UUID marli;

    @BeforeEach
    void umaPizzariaComDoisProdutosPublicadosEUmRascunho() {
        mongo.getCollection("produtos").deleteMany(new Document());
        MERCHANT.esquecerOQueRecebeu();

        loja = UUID.randomUUID();
        marli = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();

        Produto margherita = Produto.rascunho(loja, categoria, "Pizza margherita",
                Precos.reais("49.90"), ModoDeControle.QUALITATIVO, 0);
        margherita.acrescentarGrupo(GrupoDeOpcoes.novo("Tamanho", 1, 2, 0, List.of(
                Opcao.nova("Pequena", Precos.reais("0.00"), 0),
                Opcao.nova("Grande", Precos.reais("16.00"), 1))));
        margherita.publicar();
        produtos.salvar(margherita);

        Produto refrigerante = Produto.rascunho(loja, categoria, "Refrigerante lata",
                Precos.reais("7.00"), ModoDeControle.SEM_CONTROLE, 1);
        refrigerante.publicar();
        produtos.salvar(refrigerante);

        // Rascunho: existe no banco e não aparece na lista.
        produtos.salvar(Produto.rascunho(loja, categoria, "Pizza em teste",
                Precos.reais("1.00"), ModoDeControle.QUALITATIVO, 2));

        MERCHANT.respondeCom(marli, loja, "ADMINISTRADOR", List.of("VER_PRODUTO"));
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec pedirProdutos(UUID lojaDaUrl, String token, String query) {
        var requisicao = cliente().get()
                .uri("/api/v1/merchants/" + lojaDaUrl + "/catalog/produtos" + query);
        if (token != null) {
            requisicao = requisicao.header("Authorization", "Bearer " + token);
        }
        return requisicao.exchange();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> corpoDe(RestTestClient.ResponseSpec resposta) {
        return resposta.expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
    }

    // ── o caminho feliz ─────────────────────────────────────────────────────

    @Test
    @DisplayName("com VER_PRODUTO, a lista traz os publicados — e não o rascunho")
    void a_lista_traz_os_publicados() {
        Map<String, Object> pagina = corpoDe(pedirProdutos(loja, IDENTITY.tokenDe(marli), ""));

        assertThat(pagina.get("total")).isEqualTo(2);
        assertThat((List<Map<String, Object>>) pagina.get("conteudo"))
                .extracting(item -> item.get("nome"))
                .containsExactlyInAnyOrder("Pizza margherita", "Refrigerante lata");
    }

    @Test
    @DisplayName("o resumo traz vendavel — o cliente não conseguiria recalculá-lo sem os grupos")
    void o_resumo_traz_vendavel() {
        Map<String, Object> pagina = corpoDe(pedirProdutos(loja, IDENTITY.tokenDe(marli), ""));

        assertThat((List<Map<String, Object>>) pagina.get("conteudo"))
                .allSatisfy(item -> {
                    assertThat(item).containsKeys("id", "nome", "precoBase", "disponibilidade",
                            "vendavel");
                    assertThat(item.get("vendavel")).isEqualTo(true);
                });
    }

    @Test
    @DisplayName("a paginação é de verdade: size=1 traz um item e o total continua dois")
    void a_paginacao_funciona() {
        Map<String, Object> pagina = corpoDe(pedirProdutos(loja, IDENTITY.tokenDe(marli),
                "?page=0&size=1"));

        assertThat((List<?>) pagina.get("conteudo")).hasSize(1);
        assertThat(pagina.get("total"))
                .as("o total é o que permite desenhar a paginação sem uma segunda consulta")
                .isEqualTo(2);
        assertThat(pagina.get("pagina")).isEqualTo(0);
        assertThat(pagina.get("tamanho")).isEqualTo(1);
    }

    @Test
    @DisplayName("o token do portador chega ao merchant — não um token qualquer, nem nenhum")
    void o_token_atravessa() {
        String token = IDENTITY.tokenDe(marli);

        pedirProdutos(loja, token, "").expectStatus().isOk();

        assertThat(MERCHANT.autorizacoesRecebidas()).containsExactly("Bearer " + token);
    }

    // ── as quatro recusas, todas iguais ─────────────────────────────────────

    @Test
    @DisplayName("sem vínculo: 403")
    void sem_vinculo_e_403() {
        MERCHANT.nega();

        pedirProdutos(loja, IDENTITY.tokenDe(marli), "").expectStatus().isForbidden();
    }

    @Test
    @DisplayName("vínculo ativo sem VER_PRODUTO: 403 — e o merchant respondeu 200")
    void sem_a_permissao_e_403() {
        MERCHANT.respondeCom(marli, loja, "COLABORADOR", List.of("VER_PEDIDO", "ALTERAR_STATUS"));

        pedirProdutos(loja, IDENTITY.tokenDe(marli), "").expectStatus().isForbidden();
    }

    @Test
    @DisplayName("permissão desconhecida não concede — descartar nunca é deixar passar")
    void permissao_desconhecida_nao_concede() {
        MERCHANT.respondeCom(marli, loja, "COLABORADOR", List.of("VER_PRODUTOS"));

        // Um s a mais no nome de uma permissão não pode abrir o cardápio. (O
        // ResponseSpec do RestTestClient não tem .as(...) — é do AssertJ —, então
        // a mensagem mora neste comentário.)
        pedirProdutos(loja, IDENTITY.tokenDe(marli), "")
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("merchant com erro e merchant calado devolvem o MESMO corpo de sem vínculo")
    void as_quatro_recusas_sao_indistinguiveis() {
        MERCHANT.nega();
        ProblemDetail semVinculo = recusa(IDENTITY.tokenDe(marli));

        MERCHANT.estoura();
        ProblemDetail comErro = recusa(IDENTITY.tokenDe(marli));

        MERCHANT.emSilencio();
        ProblemDetail calado = recusa(IDENTITY.tokenDe(marli));

        assertThat(comErro.getStatus()).isEqualTo(semVinculo.getStatus());
        assertThat(comErro.getDetail())
                .as("pela borda, o cliente não pode distinguir 'a loja não é sua' de 'o "
                        + "sistema está com problema' — a diferença interessa a quem opera, "
                        + "e vive no registro e na porta")
                .isEqualTo(semVinculo.getDetail());
        assertThat(calado.getStatus()).isEqualTo(semVinculo.getStatus());
        assertThat(calado.getDetail()).isEqualTo(semVinculo.getDetail());
    }

    private ProblemDetail recusa(String token) {
        return pedirProdutos(loja, token, "")
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
    }

    // ── 401: o token, antes de qualquer regra ───────────────────────────────

    @Test
    void sem_token_e_401_e_nao_403() {
        pedirProdutos(loja, null, "").expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("token de outro emissor é 401 — e o merchant nem chega a ser chamado")
    void token_de_outro_emissor_e_401() {
        pedirProdutos(loja, IDENTITY.tokenComEmissor("http://homologacao", marli), "")
                .expectStatus().isUnauthorized();

        assertThat(MERCHANT.autorizacoesRecebidas())
                .as("recusar cedo é o que impede este serviço de repassar lixo adiante")
                .isEmpty();
    }

    @Test
    void token_expirado_e_401() {
        pedirProdutos(loja, IDENTITY.tokenExpirado(marli), "").expectStatus().isUnauthorized();
    }

    @Test
    void token_com_sub_que_nao_e_uuid_atravessa_a_cadeia_e_e_recusado_depois() {
        // O catalog não converte o sub: quem o usa é o merchant. O token é
        // válido para a cadeia de filtros, e a recusa vem da autorização.
        MERCHANT.nega();

        pedirProdutos(loja, IDENTITY.tokenComSujeito("nao-sou-um-uuid"), "")
                .expectStatus().isForbidden();
    }

    // ── o actuator ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("o health responde sem token, e responde 200 — com o indicador do AMQP ligado")
    void o_health_responde_sem_token() {
        cliente().get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
