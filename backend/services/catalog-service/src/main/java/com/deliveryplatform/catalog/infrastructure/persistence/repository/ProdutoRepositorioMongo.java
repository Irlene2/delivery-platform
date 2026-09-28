package com.deliveryplatform.catalog.infrastructure.persistence.repository;

import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.EstadoDePublicacao;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.infrastructure.persistence.mapper.ProdutoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * O adaptador: recebe e devolve agregado, grava e lê documento.
 *
 * <p>Um caminho de mapeamento só, nos dois sentidos, pelo
 * {@link ProdutoMapper}. A regra é a mesma que a rodada F cobrou do
 * {@code merchant}: quem escrever um segundo caminho de mapeamento cria duas
 * verdades sobre o mesmo documento, e a segunda envelhece.
 */
@Repository
public class ProdutoRepositorioMongo implements ProdutoRepositorio {

    private final ProdutoSpringDataRepository documentos;

    public ProdutoRepositorioMongo(ProdutoSpringDataRepository documentos) {
        this.documentos = documentos;
    }

    @Override
    public Produto salvar(Produto produto) {
        return ProdutoMapper.paraDominio(
                documentos.save(ProdutoMapper.paraDocumento(produto)));
    }

    @Override
    public Optional<Produto> buscarPorId(UUID id) {
        return documentos.findById(id).map(ProdutoMapper::paraDominio);
    }

    @Override
    public Page<Produto> publicadosDe(UUID estabelecimentoId, Pageable paginacao) {
        // Page.map preserva o total. Converter para lista e reembrulhar num
        // PageImpl perderia o getTotalElements(), que é justamente o que o
        // cliente não consegue recalcular.
        return documentos
                .findByEstabelecimentoIdAndEstadoDePublicacao(
                        estabelecimentoId, EstadoDePublicacao.ATIVO.name(), paginacao)
                .map(ProdutoMapper::paraDominio);
    }
}
