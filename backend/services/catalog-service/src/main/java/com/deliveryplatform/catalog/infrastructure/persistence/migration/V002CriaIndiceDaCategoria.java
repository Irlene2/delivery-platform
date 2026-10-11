package com.deliveryplatform.catalog.infrastructure.persistence.migration;

import io.mongock.api.annotations.BeforeExecution;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

/**
 * O índice da categoria, {@code (estabelecimentoId, ordem)} — que é como a lista é
 * servida (ADR-064 §5). A §7 do {@code catalogo.md} ganhou a linha na mesma rodada: a
 * C13 manda que toda estrutura e índice nasçam por Mongock, e um índice que existe no
 * código e não no documento é a assimetria que ela impede.
 *
 * <p>O padrão é o do {@link V001CriaIndicesDoProduto}, e o javadoc de lá explica as
 * três escolhas que se repetem aqui: o índice começa por {@code estabelecimentoId}, tem
 * nome, e nasce em {@code @BeforeExecution} porque o Mongo recusa
 * {@code createIndexes} dentro da transação que o Mongock abre para o
 * {@code @Execution} (erro 72).
 *
 * <p>O desempate da listagem — nome e id — não está no índice. Seções com a mesma
 * ordem numa loja são poucas, e ordenar algumas em memória custa menos que manter um
 * índice mais largo em toda gravação.
 */
@ChangeUnit(id = "002-cria-indice-da-categoria", order = "002", author = "delivery-platform")
public class V002CriaIndiceDaCategoria {

    static final String COLECAO = "categorias";
    static final String POR_ORDEM = "categoria_por_estabelecimento_e_ordem";

    @BeforeExecution
    public void criarIndice(MongoTemplate mongo) {
        mongo.indexOps(COLECAO).createIndex(
                new Index()
                        .on("estabelecimentoId", Sort.Direction.ASC)
                        .on("ordem", Sort.Direction.ASC)
                        .named(POR_ORDEM));
    }

    @RollbackBeforeExecution
    public void derrubarIndice(MongoTemplate mongo) {
        mongo.indexOps(COLECAO).dropIndex(POR_ORDEM);
    }

    /** Nenhum dado a transformar — ver o javadoc do {@link V001CriaIndicesDoProduto}. */
    @Execution
    public void executar() {
    }

    @RollbackExecution
    public void reverter() {
    }
}
