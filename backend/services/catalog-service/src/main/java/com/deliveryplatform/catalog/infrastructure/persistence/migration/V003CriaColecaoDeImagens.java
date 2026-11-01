package com.deliveryplatform.catalog.infrastructure.persistence.migration;

import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * A coleção das fotos dos produtos (ADR-077, emenda de 01/11/2026), criada por Mongock como a
 * C13 manda. Sem índice além do {@code _id}, que é o do produto: a foto só é lida por ele.
 *
 * <p>Criada em {@code @BeforeExecution} pelo mesmo motivo dos índices da V001: o Mongock abre
 * transação para o {@code @Execution}, e criar coleção é melhor fora dela.
 */
@ChangeUnit(id = "003-cria-colecao-de-imagens", order = "003", author = "delivery-platform")
public class V003CriaColecaoDeImagens {

    static final String COLECAO = "imagens_de_produto";

    @BeforeExecution
    public void criarColecao(MongoTemplate mongo) {
        if (!mongo.collectionExists(COLECAO)) {
            mongo.createCollection(COLECAO);
        }
    }

    @RollbackBeforeExecution
    public void derrubarColecao(MongoTemplate mongo) {
        mongo.dropCollection(COLECAO);
    }

    /** Nenhum dado a transformar — ver o javadoc do {@link V001CriaIndicesDoProduto}. */
    @Execution
    public void executar() {
    }

    @RollbackExecution
    public void reverter() {
    }
}
