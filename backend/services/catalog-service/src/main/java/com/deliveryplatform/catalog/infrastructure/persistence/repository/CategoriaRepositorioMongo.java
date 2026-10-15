package com.deliveryplatform.catalog.infrastructure.persistence.repository;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.infrastructure.persistence.entity.CategoriaDocumento;
import com.deliveryplatform.catalog.infrastructure.persistence.mapper.CategoriaMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O adaptador da categoria. Grava pelo {@code MongoTemplate}, que respeita o
 * {@code @Version} do documento como o repositório do Spring Data respeitaria — nulo
 * insere, preenchido atualiza com a guarda.
 *
 * <p><b>Sem a tradução do {@code WriteConflict}</b> que o {@code ProdutoRepositorioMongo}
 * faz: ela existe porque a marcação e a reativação gravam o mesmo produto em
 * transações concorrentes. A categoria só nasce, e ninguém a grava duas vezes. Quando
 * renomear ganhar rota, a tradução vem junto.
 */
@Repository
public class CategoriaRepositorioMongo implements CategoriaRepositorio {

    /** A ordem que o índice {@code (estabelecimentoId, ordem)} serve, e o desempate. */
    private static final Sort ORDEM_DA_LISTA = Sort.by("ordem", "nome", "id");

    private final MongoTemplate mongo;

    public CategoriaRepositorioMongo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        return CategoriaMapper.paraDominio(mongo.save(CategoriaMapper.paraDocumento(categoria)));
    }

    @Override
    public Optional<Categoria> buscarPorId(UUID id) {
        return Optional.ofNullable(mongo.findById(id, CategoriaDocumento.class))
                .map(CategoriaMapper::paraDominio);
    }

    @Override
    public List<Categoria> daLoja(UUID estabelecimentoId) {
        Query daLoja = Query.query(Criteria.where("estabelecimentoId").is(estabelecimentoId))
                .with(ORDEM_DA_LISTA);
        return mongo.find(daLoja, CategoriaDocumento.class).stream()
                .map(CategoriaMapper::paraDominio)
                .toList();
    }
}
