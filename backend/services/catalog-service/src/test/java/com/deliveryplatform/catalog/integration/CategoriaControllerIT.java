package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.support.IdentityDeMentira;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.MerchantDeMentira;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As duas rotas da categoria (ADR-064), contra o Mongo de verdade e o {@code merchant}
 * dublado.
 *
 * <p><b>Cada teste tem loja e pessoa novas</b>: a autorização tem cache em processo
 * (ADR-011), e reusar o par entre testes faria um caso herdar a permissão do outro.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoriaControllerIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();
    private static final MerchantDeMentira MERCHANT = MerchantDeMentira.subir();

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

    @Autowired CategoriaRepositorio categorias;
    @Autowired MongoTemplate mongo;

    private UUID loja;
    private UUID marli;
    private String token;

    @BeforeEach
    void umaLojaEUmaPessoaNovas() {
        mongo.getCollection("categorias").deleteMany(new Document());
        loja = UUID.randomUUID();
        marli = UUID.randomUUID();
        token = IDENTITY.tokenDe(marli);
        MERCHANT.respondeCom(marli, loja, "ADMINISTRADOR", List.of("VER_PRODUTO", "CRIAR_PRODUTO"));
    }

    // ── criar ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cria com os valores do pedido, nasce ativa, e o 201 tem a forma de um item da lista")
    void cria_e_nasce_ativa() {
        var resposta = criar(loja, token, "{\"nome\":\"Pizzas salgadas\",\"ordem\":2}");

        resposta.expectStatus().isCreated();
        resposta.expectHeader().doesNotExist("Location");
        @SuppressWarnings("unchecked")
        Map<String, Object> criada = resposta.expectBody(Map.class).returnResult().getResponseBody();

        assertThat(criada.get("nome")).isEqualTo("Pizzas salgadas");
        assertThat(criada.get("ordem")).isEqualTo(2);
        assertThat(criada.get("ativa"))
                .as("o javadoc diz que categoria nova nasce ativa — salvar não basta")
                .isEqualTo(true);

        List<Map<String, Object>> lista = lista(loja);
        assertThat(lista).containsExactly(criada);
    }

    @Test
    @DisplayName("nome em branco: 400 pela forma do DTO, e nada é gravado")
    void nome_em_branco_e_400() {
        criar(loja, token, "{\"nome\":\"   \",\"ordem\":0}").expectStatus().isBadRequest();
        criar(loja, token, "{\"ordem\":0}").expectStatus().isBadRequest();

        assertThat(categorias.daLoja(loja)).isEmpty();
    }

    @Test
    @DisplayName("sem CRIAR_PRODUTO: 403 com o texto genérico, e nada é gravado")
    void sem_criar_produto_e_403() {
        MERCHANT.respondeCom(marli, loja, "COLABORADOR", List.of("VER_PRODUTO"));

        ProblemDetail recusa = criar(loja, token, "{\"nome\":\"Bebidas\",\"ordem\":0}")
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo("sem acesso a este estabelecimento");
        assertThat(categorias.daLoja(loja)).isEmpty();
    }

    @Test
    @DisplayName("loja de outra pessoa: 403 idêntico ao da loja que não existe (M7)")
    void outra_loja_e_403_igual_a_inexistente() {
        MERCHANT.nega();
        UUID lojaAlheia = UUID.randomUUID();
        String forasteiro = IDENTITY.tokenDe(UUID.randomUUID());

        ProblemDetail alheia = criar(lojaAlheia, forasteiro, "{\"nome\":\"Bebidas\",\"ordem\":0}")
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
        ProblemDetail inexistente = criar(UUID.randomUUID(), forasteiro,
                "{\"nome\":\"Bebidas\",\"ordem\":0}")
                .expectStatus().isForbidden()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(alheia.getDetail()).isEqualTo(inexistente.getDetail());
        assertThat(alheia.getStatus()).isEqualTo(inexistente.getStatus());
        assertThat(categorias.daLoja(lojaAlheia)).isEmpty();
    }

    // ── listar ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("lista só as da loja da URL — com duas lojas no banco")
    void lista_so_as_da_loja() {
        UUID vizinha = UUID.randomUUID();
        categorias.salvar(Categoria.nova(loja, "Pizzas", 0));
        categorias.salvar(Categoria.nova(vizinha, "Lanches", 0));

        List<Map<String, Object>> lista = lista(loja);

        assertThat(lista).extracting(c -> c.get("nome")).containsExactly("Pizzas");
    }

    @Test
    @DisplayName("a ordem é estável: pela ordem, e no empate pelo nome")
    void a_ordem_e_estavel() {
        categorias.salvar(Categoria.nova(loja, "Sobremesas", 2));
        categorias.salvar(Categoria.nova(loja, "Pizzas doces", 1));
        categorias.salvar(Categoria.nova(loja, "Bebidas", 1));
        categorias.salvar(Categoria.nova(loja, "Pizzas salgadas", 0));

        for (int recarga = 0; recarga < 3; recarga++) {
            List<Map<String, Object>> lista = lista(loja);
            assertThat(lista).extracting(c -> c.get("nome"))
                    .containsExactly("Pizzas salgadas", "Bebidas", "Pizzas doces", "Sobremesas");
        }
    }

    @Test
    @DisplayName("loja sem seção: 200 com [] — é o estado de toda loja recém-cadastrada")
    void lista_vazia_e_200() {
        listar(loja, token).expectStatus().isOk().expectBody().json("[]");
    }

    @Test
    @DisplayName("sem VER_PRODUTO: 403")
    void sem_ver_produto_e_403() {
        MERCHANT.respondeCom(marli, loja, "COLABORADOR", List.of("CRIAR_PRODUTO"));

        listar(loja, token).expectStatus().isForbidden();
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec criar(UUID daLoja, String comToken, String corpo) {
        return cliente().post()
                .uri("/api/v1/merchants/" + daLoja + "/catalog/categorias")
                .header("Authorization", "Bearer " + comToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .exchange();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> lista(UUID daLoja) {
        return listar(daLoja, token).expectStatus().isOk()
                .expectBody(List.class).returnResult().getResponseBody();
    }

    private RestTestClient.ResponseSpec listar(UUID daLoja, String comToken) {
        return cliente().get()
                .uri("/api/v1/merchants/" + daLoja + "/catalog/categorias")
                .header("Authorization", "Bearer " + comToken)
                .exchange();
    }
}
