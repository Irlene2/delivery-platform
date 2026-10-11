package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.EstadoDePublicacao;
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
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code PUT .../produtos/{produtoId}/publicacao} (ADR-065), contra o Mongo de verdade.
 * O estado se afirma <b>pelo banco</b>.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PublicacaoDeProdutoIT extends Infraestrutura {

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
    @Autowired MongoTemplate mongo;

    private UUID loja;
    private UUID marli;
    private String token;

    @BeforeEach
    void umaLojaEUmaPessoaNovas() {
        mongo.getCollection("produtos").deleteMany(new Document());
        loja = UUID.randomUUID();
        marli = UUID.randomUUID();
        token = IDENTITY.tokenDe(marli);
        MERCHANT.respondeCom(marli, loja, "ADMINISTRADOR", List.of("VER_PRODUTO", "ALTERAR_PRODUTO"));
    }

    @Test
    @DisplayName("C11: um rascunho simples vai ao cardápio — pelo banco")
    void rascunho_vai_ao_cardapio() {
        Produto coca = rascunho("Coca-Cola lata", "6.00");

        publicar(loja, coca.getId()).expectStatus().isOk();

        assertThat(estadoNoBanco(coca)).isEqualTo(EstadoDePublicacao.ATIVO);
    }

    @Test
    @DisplayName("C12: publicar duas vezes seguidas — 200 nas duas, e o estado é o mesmo")
    void duas_vezes_e_200_nas_duas() {
        Produto coca = rascunho("Coca-Cola lata", "6.00");

        publicar(loja, coca.getId()).expectStatus().isOk();
        publicar(loja, coca.getId()).expectStatus().isOk()
                .expectBody().jsonPath("$.estadoDePublicacao").isEqualTo("ATIVO");

        assertThat(estadoNoBanco(coca)).isEqualTo(EstadoDePublicacao.ATIVO);
    }

    /**
     * {@code INATIVO → ATIVO} é transição válida ({@code PublicacaoDoProdutoTest}), e é
     * o mesmo ato. Um "só publica se for rascunho" no caso de uso passaria pelo C12 — o
     * produto já ativo continuaria ativo — e cairia aqui.
     */
    @Test
    @DisplayName("um INATIVO volta ao cardápio pela mesma rota")
    void inativo_volta() {
        Produto coca = rascunho("Coca-Cola lata", "6.00");
        coca.publicar();
        coca.inativar();
        coca = produtos.salvar(coca);

        publicar(loja, coca.getId()).expectStatus().isOk();

        assertThat(estadoNoBanco(coca)).isEqualTo(EstadoDePublicacao.ATIVO);
    }

    @Test
    @DisplayName("C13: produto de outra loja responde igual a produto que não existe (M7)")
    void outra_loja_e_igual_a_inexistente() {
        UUID vizinha = UUID.randomUUID();
        Produto daVizinha = produtos.salvar(Produto.rascunho(vizinha, ProdutoDeTeste.CATEGORIA,
                "X-Burguer", Precos.reais("20.00"), ModoDeControle.QUALITATIVO, 0));

        ProblemDetail deOutraLoja = publicar(loja, daVizinha.getId())
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();
        ProblemDetail inexistente = publicar(loja, UUID.randomUUID())
                .expectStatus().isNotFound()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(deOutraLoja.getDetail()).isEqualTo(inexistente.getDetail());
        assertThat(estadoNoBanco(daVizinha))
                .as("quem tem ALTERAR_PRODUTO aqui não publica o cardápio de lá")
                .isEqualTo(EstadoDePublicacao.RASCUNHO);
    }

    @Test
    @DisplayName("C14: sem ALTERAR_PRODUTO é 403, e nada muda")
    void sem_alterar_produto_e_403() {
        Produto coca = rascunho("Coca-Cola lata", "6.00");
        MERCHANT.respondeCom(marli, loja, "COLABORADOR", List.of("VER_PRODUTO", "CRIAR_PRODUTO"));

        publicar(loja, coca.getId()).expectStatus().isForbidden();

        assertThat(estadoNoBanco(coca)).isEqualTo(EstadoDePublicacao.RASCUNHO);
    }

    /**
     * Alcançável pela rota, sem grupos: o rascunho aceita preço zero, e a C1 de zero é
     * cobrada na publicação. As recusas de C5, C4 e C2 precisam de grupos, que não
     * entram pelo corpo nesta rodada — seguem cobertas pelo {@code ErroDoCatalogoTest}.
     */
    @Test
    @DisplayName("C15/D17: a recusa traz o nome do produto e não o código da invariante, no corpo da ADR-055")
    void recusa_traz_o_dado() {
        Produto brinde = rascunho("Pizza doce", "0.00");

        ProblemDetail recusa = publicar(loja, brinde.getId())
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getStatus()).isEqualTo(400);
        assertThat(recusa.getDetail())
                .isEqualTo("«Pizza doce» está com preço zero e não vai ao cardápio; "
                        + "ponha o preço e publique de novo")
                .doesNotContain("C1");
        assertThat(estadoNoBanco(brinde)).isEqualTo(EstadoDePublicacao.RASCUNHO);
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private Produto rascunho(String nome, String preco) {
        return produtos.salvar(Produto.rascunho(loja, ProdutoDeTeste.CATEGORIA, nome,
                Precos.reais(preco), ModoDeControle.SEM_CONTROLE, 0));
    }

    private EstadoDePublicacao estadoNoBanco(Produto produto) {
        return produtos.buscarPorId(produto.getId()).orElseThrow().getEstadoDePublicacao();
    }

    private RestTestClient.ResponseSpec publicar(UUID daLoja, UUID produtoId) {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build()
                .put()
                .uri("/api/v1/merchants/" + daLoja + "/catalog/produtos/" + produtoId + "/publicacao")
                .header("Authorization", "Bearer " + token)
                .exchange();
    }
}
