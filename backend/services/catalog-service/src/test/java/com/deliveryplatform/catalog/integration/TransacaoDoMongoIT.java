package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.Precos;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O teste de fumaça que a ADR-008 pediu por escrito e nunca teve.
 *
 * <blockquote>
 * "A string de conexão vira ponto único de falha silenciosa. Toda nova
 * configuração precisa do parâmetro. Merece linha nas armadilhas e,
 * idealmente, <b>um teste de fumaça que abra uma transação no startup de cada
 * serviço documental</b>."
 * </blockquote>
 *
 * <p>Ele cobre duas coisas que falham em silêncio e só apareceriam no dia em
 * que o outbox do catálogo existisse:
 *
 * <ol>
 *   <li><b>Não há gerente de transação.</b> O Spring Boot não autoconfigura
 *       {@code MongoTransactionManager}; sem o bean da {@code MongoConfig},
 *       este teste estoura por não achar gerente.</li>
 *   <li><b>O contêiner não está em replica set.</b> A classe atual do
 *       Testcontainers não liga o replica set sozinha — sem
 *       {@code withReplicaSet()}, o Mongo recusa a transação.</li>
 * </ol>
 *
 * <p><b>O que ele <i>não</i> cobre, e foi medido.</b> A ADR-008 diz que sem
 * {@code ?replicaSet=} a transação falha. Aqui ela não falha: o
 * {@code getReplicaSetUrl} da Testcontainers 2 devolve a URI <b>sem</b> o
 * parâmetro, o driver conecta em {@code mode=SINGLE} direto no primário, e
 * primário de replica set faz transação. O que o parâmetro compra é a
 * descoberta da topologia — failover, mais de um nó —, não a transação num nó
 * só. Conferido no log deste teste em 27/09/2026.
 *
 * <p>Nenhuma das três aparece em nenhum outro teste desta rodada: gravar e ler
 * funciona perfeitamente sem transação nenhuma.
 */
@SpringBootTest
class TransacaoDoMongoIT extends Infraestrutura {

    @Autowired ProdutoRepositorio produtos;
    @Autowired MongoTemplate mongo;
    @Autowired TransactionTemplate transacao;

    @BeforeEach
    void limpar() {
        mongo.getCollection("produtos").deleteMany(new Document());
    }

    private static Produto coxinha() {
        return Produto.rascunho(UUID.randomUUID(), UUID.randomUUID(), "Coxinha",
                Precos.reais("8.00"), ModoDeControle.SEM_CONTROLE, 0);
    }

    @Test
    @DisplayName("uma transação abre, grava e confirma")
    void a_transacao_confirma() {
        Produto p = transacao.execute(status -> produtos.salvar(coxinha()));

        assertThat(produtos.buscarPorId(p.getId())).isPresent();
    }

    @Test
    @DisplayName("uma transação que estoura não deixa o documento gravado")
    void a_transacao_desfaz() {
        Produto p = coxinha();

        assertThatThrownBy(() -> transacao.execute(status -> {
            produtos.salvar(p);
            throw new IllegalStateException("desfaz");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(produtos.buscarPorId(p.getId()))
                .as("se este documento existir, não há transação — e o outbox do catálogo, "
                        + "quando existir, publicaria eventos de escritas que não aconteceram. "
                        + "É o defeito que a ADR-008 inteira existe para impedir")
                .isEmpty();
    }
}
