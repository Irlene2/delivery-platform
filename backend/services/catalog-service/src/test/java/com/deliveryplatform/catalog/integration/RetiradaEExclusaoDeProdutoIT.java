package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.ImagemDoProdutoRepositorio;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.EstadoDePublicacao;
import com.deliveryplatform.catalog.domain.model.ImagemDoProduto;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.IdentityDeMentira;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.MerchantDeMentira;
import com.deliveryplatform.catalog.support.Precos;
import com.deliveryplatform.catalog.support.ProdutoDeTeste;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As escritas do produto de 13/11/2026, pelo HTTP e conferidas no banco: tirar do cardápio,
 * apagar, trocar o "pode acabar" e o selo da vitrine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RetiradaEExclusaoDeProdutoIT extends Infraestrutura {

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

    @Autowired ProdutoRepositorio produtos;
    @Autowired ImagemDoProdutoRepositorio imagens;
    @Autowired MongoTemplate mongo;

    private UUID loja;
    private UUID pessoa;
    private String token;

    @BeforeEach
    void umaLojaEUmaPessoaNovas() {
        mongo.getCollection("produtos").deleteMany(new Document());
        loja = UUID.randomUUID();
        pessoa = UUID.randomUUID();
        token = IDENTITY.tokenDe(pessoa);
        MERCHANT.respondeCom(pessoa, loja, "ADMINISTRADOR",
                List.of("VER_PRODUTO", "ALTERAR_PRODUTO", "DESATIVAR_PRODUTO"));
    }

    @Test
    @DisplayName("o publicado sai do cardápio e volta pela publicação")
    void sai_e_volta() {
        Produto coca = publicado("Coca-Cola lata");

        cliente().delete().uri(caminho(coca, "/publicacao"))
                .header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.estadoDePublicacao").isEqualTo("INATIVO");
        assertThat(estadoNoBanco(coca)).isEqualTo(EstadoDePublicacao.INATIVO);

        cliente().put().uri(caminho(coca, "/publicacao"))
                .header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk();
        assertThat(estadoNoBanco(coca)).isEqualTo(EstadoDePublicacao.ATIVO);
    }

    @Test
    @DisplayName("apagar tira o produto e a foto, e responde 204")
    void apaga_o_produto_e_a_foto() {
        Produto coca = publicado("Coca-Cola lata");
        imagens.salvar(new ImagemDoProduto(coca.getId(), loja, "image/png", new byte[] {1, 2, 3}));

        cliente().delete().uri(caminho(coca, ""))
                .header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isNoContent();

        assertThat(produtos.buscarPorId(coca.getId())).isEmpty();
        assertThat(imagens.buscar(coca.getId())).isEmpty();
    }

    @Test
    @DisplayName("sem DESATIVAR_PRODUTO não apaga, e o produto fica")
    void sem_permissao_nao_apaga() {
        Produto coca = publicado("Coca-Cola lata");
        MERCHANT.respondeCom(pessoa, loja, "COLABORADOR", List.of("VER_PRODUTO", "ALTERAR_PRODUTO"));

        cliente().delete().uri(caminho(coca, ""))
                .header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isForbidden();

        assertThat(produtos.buscarPorId(coca.getId())).isPresent();
    }

    @Test
    @DisplayName("a correção troca o pode acabar e o selo, e os dois aparecem na leitura")
    void troca_controle_e_selo() {
        Produto pizza = produtos.salvar(Produto.rascunho(loja, ProdutoDeTeste.CATEGORIA, "Pizza",
                Precos.reais("40.00"), ModoDeControle.QUALITATIVO, 0));

        cliente().put().uri(caminho(pizza, ""))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"categoriaId\":\"" + ProdutoDeTeste.CATEGORIA + "\",\"nome\":\"Pizza\","
                        + "\"precoBase\":40.00,\"modoDeControle\":\"SEM_CONTROLE\","
                        + "\"selo\":\"Mais pedido\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.modoDeControle").isEqualTo("SEM_CONTROLE")
                .jsonPath("$.selo").isEqualTo("Mais pedido");

        Produto lido = produtos.buscarPorId(pizza.getId()).orElseThrow();
        assertThat(lido.getModoDeControle()).isEqualTo(ModoDeControle.SEM_CONTROLE);
        assertThat(lido.getSelo()).isEqualTo("Mais pedido");
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private Produto publicado(String nome) {
        Produto produto = Produto.rascunho(loja, ProdutoDeTeste.CATEGORIA, nome,
                Precos.reais("6.00"), ModoDeControle.SEM_CONTROLE, 0);
        produto.publicar();
        return produtos.salvar(produto);
    }

    private EstadoDePublicacao estadoNoBanco(Produto produto) {
        return produtos.buscarPorId(produto.getId()).orElseThrow().getEstadoDePublicacao();
    }

    private String caminho(Produto produto, String resto) {
        return "/api/v1/merchants/" + loja + "/catalog/produtos/" + produto.getId() + resto;
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }
}
