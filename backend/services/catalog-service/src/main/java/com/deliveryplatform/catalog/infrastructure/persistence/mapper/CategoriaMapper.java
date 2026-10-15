package com.deliveryplatform.catalog.infrastructure.persistence.mapper;

import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.infrastructure.persistence.entity.CategoriaDocumento;

/**
 * Um caminho de mapeamento só, nos dois sentidos — a regra do {@link ProdutoMapper}.
 *
 * <p>Sem a conversão em {@code DocumentoIlegivel} que o produto tem: a categoria só é
 * lida pela listagem, e um documento corrompido aqui é defeito nosso, que sai 500 pelo
 * motivo {@code INTEGRIDADE} (ADR-063) — que é o que o {@code DocumentoIlegivel} também
 * daria.
 */
public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static CategoriaDocumento paraDocumento(Categoria categoria) {
        return new CategoriaDocumento(
                categoria.getId(),
                categoria.getEstabelecimentoId(),
                categoria.getNome(),
                categoria.getOrdem(),
                categoria.isAtiva(),
                categoria.getVersao());
    }

    public static Categoria paraDominio(CategoriaDocumento doc) {
        return Categoria.reconstituir(
                doc.id(), doc.estabelecimentoId(), doc.nome(), doc.ordem(), doc.ativa(),
                doc.versao());
    }
}
