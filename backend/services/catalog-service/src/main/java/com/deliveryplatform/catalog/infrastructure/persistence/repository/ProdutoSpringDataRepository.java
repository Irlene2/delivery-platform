package com.deliveryplatform.catalog.infrastructure.persistence.repository;

import com.deliveryplatform.catalog.infrastructure.persistence.entity.ProdutoDocumento;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

/**
 * O repositório do Spring Data, que fala documento e não fala domínio.
 *
 * <p>Quem fala domínio é o {@code ProdutoRepositorioMongo}, que embrulha este.
 * A mesma divisão que o {@code merchant} faz entre o {@code …SpringDataRepository}
 * e o adaptador da porta.
 *
 * <p>O nome do método derivado precisa casar com o <b>campo do documento</b>,
 * não com o do agregado: {@code estadoDePublicacao} é {@code String} aqui, e é
 * por isso que o parâmetro é {@code String}.
 */
public interface ProdutoSpringDataRepository extends MongoRepository<ProdutoDocumento, UUID> {

    List<ProdutoDocumento> findByEstabelecimentoIdAndEstadoDePublicacao(
            UUID estabelecimentoId, String estadoDePublicacao);
}
