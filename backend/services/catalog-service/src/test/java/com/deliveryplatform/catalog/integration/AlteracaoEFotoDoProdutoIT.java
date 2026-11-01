package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corrigir o cadastro e pôr a foto (ADR-077, emenda de 01/11/2026), contra o Mongo de verdade.
 * O estado se afirma pelo banco; a foto, pela rota pública que o cardápio usa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AlteracaoEFotoDoProdutoIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();
    private static final MerchantDeMentira MERCHANT = MerchantDeMentira.subir();

    /** Os 8 bytes da assinatura PNG: basta para o teste, a rota não decodifica a imagem. */
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

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
    private String token;
    private Categoria pizzas;
    private Categoria doces;

    @BeforeEach
    void umaLojaNova() {
        mongo.getCollection("produtos").deleteMany(new Document());
        mongo.getCollection("categorias").deleteMany(new Document());
        loja = UUID.randomUUID();
        UUID marli = UUID.randomUUID();
        token = IDENTITY.tokenDe(marli);
        MERCHANT.respondeCom(marli, loja, "ADMINISTRADOR", List.of("VER_PRODUTO", "ALTERAR_PRODUTO"));
        pizzas = categorias.salvar(Categoria.nova(loja, "Pizzas", 0));
        doces = categorias.salvar(Categoria.nova(loja, "Pizzas doces", 1));
    }

    @Test
    @DisplayName("publicado: nome, preço, descrição e seção mudam, e ele continua no cardápio")
    void corrige_o_cadastro() {
        Produto pizza = Produto.rascunho(loja, pizzas.getId(), "Pizza margherita",
                Precos.reais("49.90"), ModoDeControle.QUALITATIVO, 0);
        pizza.publicar();
        pizza = produtos.salvar(pizza);

        cliente().put().uri("/api/v1/merchants/" + loja + "/catalog/produtos/" + pizza.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"categoriaId\":\"" + doces.getId() + "\",\"nome\":\"Pizza de banana\","
                        + "\"descricao\":\"Banana e canela\",\"precoBase\":42.50}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.nome").isEqualTo("Pizza de banana");

        Produto gravado = produtos.buscarPorId(pizza.getId()).orElseThrow();
        assertThat(gravado.getNome()).isEqualTo("Pizza de banana");
        assertThat(gravado.getPrecoBase()).isEqualTo(Precos.reais("42.50"));
        assertThat(gravado.getDescricao()).isEqualTo("Banana e canela");
        assertThat(gravado.getCategoriaId()).isEqualTo(doces.getId());
        assertThat(gravado.getEstadoDePublicacao().name()).isEqualTo("ATIVO");
    }

    @Test
    @DisplayName("a foto: gravada à parte, o produto aponta para ela, e a rota pública a devolve")
    void foto() {
        Produto pizza = Produto.rascunho(loja, pizzas.getId(), "Pizza margherita",
                Precos.reais("49.90"), ModoDeControle.QUALITATIVO, 0);
        pizza.publicar();
        pizza = produtos.salvar(pizza);

        cliente().put().uri("/api/v1/merchants/" + loja + "/catalog/produtos/" + pizza.getId() + "/imagem")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipo\":\"image/png\",\"conteudo\":\""
                        + Base64.getEncoder().encodeToString(PNG) + "\"}")
                .exchange()
                .expectStatus().isOk();

        String referencia = produtos.buscarPorId(pizza.getId()).orElseThrow().getImagemRef();
        assertThat(referencia).startsWith("/api/v1/storefront/" + loja + "/produtos/" + pizza.getId()
                + "/imagem?v=");

        byte[] lida = cliente().get().uri(referencia)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectBody(byte[].class).returnResult().getResponseBody();
        assertThat(lida).isEqualTo(PNG);

        assertThat(mongo.getCollection("produtos").find().first().toJson())
                .as("o binário nunca entra no documento do produto (catalogo.md §9)")
                .doesNotContain(Base64.getEncoder().encodeToString(PNG));

        cliente().get().uri("/api/v1/storefront/" + loja + "/menu")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.produtos[0].imagem").isEqualTo(referencia);
    }

    @Test
    @DisplayName("a foto de um produto não sai pela vitrine de outra loja")
    void foto_de_outra_loja() {
        Produto pizza = produtos.salvar(Produto.rascunho(loja, pizzas.getId(), "Pizza",
                Precos.reais("49.90"), ModoDeControle.QUALITATIVO, 0));
        cliente().put().uri("/api/v1/merchants/" + loja + "/catalog/produtos/" + pizza.getId() + "/imagem")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"tipo\":\"image/png\",\"conteudo\":\""
                        + Base64.getEncoder().encodeToString(PNG) + "\"}")
                .exchange()
                .expectStatus().isOk();

        cliente().get().uri("/api/v1/storefront/" + UUID.randomUUID() + "/produtos/" + pizza.getId()
                        + "/imagem")
                .exchange()
                .expectStatus().isNotFound();
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }
}
