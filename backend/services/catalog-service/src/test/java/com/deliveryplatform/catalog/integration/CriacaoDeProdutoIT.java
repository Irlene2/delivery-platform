package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.EstadoDePublicacao;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.IdentityDeMentira;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.MerchantDeMentira;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code POST .../catalog/produtos} (ADR-065), e a regra da categoria (ADR-066), contra o
 * Mongo de verdade e o {@code merchant} dublado.
 *
 * <p><b>O que foi gravado se afirma pelo repositório</b>, nunca pelo corpo da resposta:
 * um serviço que ignorasse o campo e devolvesse o DTO de entrada passaria pelo corpo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CriacaoDeProdutoIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();
    private static final MerchantDeMentira MERCHANT = MerchantDeMentira.subir();

    private static final String FRASE_DA_SECAO =
            "a seção escolhida não está no cardápio desta loja; escolha uma das seções da lista";

    @DynamicPropertySource
    static void apontarParaOsDubles(DynamicPropertyRegistry registry) {
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
    @Autowired CategoriaRepositorio categorias;
    @Autowired MongoTemplate mongo;

    private UUID loja;
    private UUID marli;
    private String token;
    private Categoria bebidas;

    @BeforeEach
    void umaLojaComUmaSecao() {
        mongo.getCollection("produtos").deleteMany(new Document());
        mongo.getCollection("categorias").deleteMany(new Document());
        loja = UUID.randomUUID();
        marli = UUID.randomUUID();
        token = IDENTITY.tokenDe(marli);
        MERCHANT.respondeCom(marli, loja, "ADMINISTRADOR", List.of("VER_PRODUTO", "CRIAR_PRODUTO"));
        bebidas = categorias.salvar(Categoria.nova(loja, "Bebidas", 0));
    }

    // ── A · criar ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("A1/A2/A6: 201, nasce RASCUNHO, Location para o GET, e o banco tem o categoriaId do pedido")
    void cria_rascunho() {
        var resposta = criar(corpo(bebidas.getId(), "Coca-Cola lata", "6.00"));

        resposta.expectStatus().isCreated();
        Map<String, Object> criado = mapa(resposta);
        String id = (String) criado.get("id");
        resposta.expectHeader().valueEquals("Location",
                "/api/v1/merchants/" + loja + "/catalog/produtos/" + id);
        assertThat(criado.get("estadoDePublicacao")).isEqualTo("RASCUNHO");

        Produto gravado = produtos.buscarPorId(UUID.fromString(id)).orElseThrow();
        assertThat(gravado.getEstadoDePublicacao()).isEqualTo(EstadoDePublicacao.RASCUNHO);
        assertThat(gravado.getCategoriaId())
                .as("é o banco que conta, não o objeto que o serviço devolveu")
                .isEqualTo(bebidas.getId());
        assertThat(gravado.getEstabelecimentoId()).isEqualTo(loja);
        assertThat(gravado.getNome()).isEqualTo("Coca-Cola lata");
        assertThat(gravado.getDescricao()).isEqualTo("350 ml");
        assertThat(gravado.getPrecoBase().valor()).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(gravado.getModoDeControle()).isEqualTo(ModoDeControle.SEM_CONTROLE);
    }

    @Test
    @DisplayName("o Location aponta para uma rota que responde")
    void o_location_responde() {
        String location = criar(corpo(bebidas.getId(), "Guaraná lata", "5.50"))
                .expectStatus().isCreated()
                .returnResult(String.class).getResponseHeaders().getFirst("Location");

        cliente().get().uri(location).header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("A3: preço negativo é recusado pela C1, e a frase não diz C1")
    void preco_negativo_e_400() {
        ProblemDetail recusa = criar(corpo(bebidas.getId(), "Água", "-1.00"))
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail())
                .startsWith("o preço do produto não pode ser negativo")
                .doesNotContain("C1");
        assertThat(mongo.getCollection("produtos").countDocuments()).isZero();
    }

    @Test
    @DisplayName("A3: preço zero é aceito no rascunho — a C1 de zero é cobrada na publicação")
    void preco_zero_no_rascunho() {
        criar(corpo(bebidas.getId(), "Brinde", "0.00")).expectStatus().isCreated();
    }

    @Test
    @DisplayName("A4: nome em branco é 400 pela forma do DTO")
    void nome_em_branco_e_400() {
        criar(corpo(bebidas.getId(), "   ", "6.00")).expectStatus().isBadRequest();
        assertThat(mongo.getCollection("produtos").countDocuments()).isZero();
    }

    @Test
    @DisplayName("A5: sem CRIAR_PRODUTO é 403 com o texto genérico")
    void sem_criar_produto_e_403() {
        MERCHANT.respondeCom(marli, loja, "COLABORADOR", List.of("VER_PRODUTO"));

        ProblemDetail recusa = criar(corpo(bebidas.getId(), "Coca-Cola lata", "6.00"))
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo("sem acesso a este estabelecimento");
    }

    @Test
    @DisplayName("A7: dois produtos com o mesmo nome na mesma loja — o agregado não proíbe, e a rota também não")
    void mesmo_nome_duas_vezes() {
        criar(corpo(bebidas.getId(), "Coca-Cola lata", "6.00")).expectStatus().isCreated();
        criar(corpo(bebidas.getId(), "Coca-Cola lata", "6.50")).expectStatus().isCreated();

        assertThat(mongo.getCollection("produtos").countDocuments()).isEqualTo(2);
    }

    /**
     * O gatilho da ADR-063: com a primeira rota que monta um {@code Produto} a partir de
     * um corpo, quais pontos de {@code INTEGRIDADE} ficaram alcançáveis. Os quatro abaixo
     * eram — e cada um ganhou um {@code @NotNull} no DTO. Nenhum pode sair 500.
     */
    @ParameterizedTest(name = "sem {0}: 400, e não 500")
    @ValueSource(strings = {"categoriaId", "precoBase", "modoDeControle", "ordem", "nome"})
    @DisplayName("§2.4: campo ausente é forma — 400, nunca o 500 de integridade")
    void campo_ausente_e_400(String campo) {
        String completo = corpo(bebidas.getId(), "Coca-Cola lata", "6.00");
        String semOCampo = completo.replaceFirst(",?\"" + campo + "\":(\"[^\"]*\"|[^,}]*)", "")
                .replace("{,", "{");

        assertThat(semOCampo).doesNotContain("\"" + campo + "\"");
        criar(semOCampo).expectStatus().isBadRequest();
        assertThat(mongo.getCollection("produtos").countDocuments()).isZero();
    }

    @Test
    @DisplayName("§2.4: categoriaId que não é UUID e modo desconhecido são 400")
    void forma_errada_e_400() {
        criar(corpo(bebidas.getId(), "Coca", "6.00").replace(bebidas.getId().toString(), "nao-e-uuid"))
                .expectStatus().isBadRequest();
        criar(corpo(bebidas.getId(), "Coca", "6.00").replace("SEM_CONTROLE", "QUANTITATIVO"))
                .expectStatus().isBadRequest();
    }

    // ── B · a referência que não resolve ───────────────────────────────────

    @Test
    @DisplayName("B8: seção inexistente é 400 com a frase, sem id, sem 'entidade', sem 'referência'")
    void secao_inexistente_e_400() {
        UUID inexistente = UUID.randomUUID();

        ProblemDetail recusa = criar(corpo(inexistente, "Coca-Cola lata", "6.00"))
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail())
                .isEqualTo(FRASE_DA_SECAO)
                .doesNotContain(inexistente.toString(), "entidade", "referência", "categoriaId");
    }

    @Test
    @DisplayName("B9/B10: seção de outra loja — duas lojas no banco — é a MESMA frase, e nada é gravado")
    void secao_de_outra_loja_e_a_mesma_recusa() {
        Categoria daVizinha = categorias.salvar(Categoria.nova(UUID.randomUUID(), "Lanches", 0));

        String deOutraLoja = criar(corpo(daVizinha.getId(), "X-Burguer", "20.00"))
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody().getDetail();
        String inexistente = criar(corpo(UUID.randomUUID(), "X-Burguer", "20.00"))
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody().getDetail();

        assertThat(deOutraLoja)
                .as("uma frase que distinguisse as duas contaria quais seções existem no sistema (M7)")
                .isEqualTo(inexistente);
        assertThat(mongo.getCollection("produtos").countDocuments())
                .as("um serviço que gravasse antes de perguntar passaria pelos dois códigos")
                .isZero();
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private String corpo(UUID categoriaId, String nome, String preco) {
        return """
                {"categoriaId":"%s","nome":"%s","descricao":"350 ml","precoBase":%s,\
                "modoDeControle":"SEM_CONTROLE","ordem":0}""".formatted(categoriaId, nome, preco);
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec criar(String corpo) {
        return cliente().post()
                .uri("/api/v1/merchants/" + loja + "/catalog/produtos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .exchange();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(RestTestClient.ResponseSpec resposta) {
        return resposta.expectBody(Map.class).returnResult().getResponseBody();
    }
}
