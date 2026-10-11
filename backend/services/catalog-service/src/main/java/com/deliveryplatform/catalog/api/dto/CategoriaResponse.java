package com.deliveryplatform.catalog.api.dto;

import com.deliveryplatform.catalog.domain.model.Categoria;

import java.util.UUID;

/**
 * A seção como a lista a mostra — e como o {@code POST} a devolve: a mesma forma nos
 * dois, para que a tela acrescente a criada à lista sem reler (ADR-064 §3).
 *
 * <p>Sem {@code estabelecimentoId}: ele está na URL de quem pediu.
 */
public record CategoriaResponse(
        UUID id,
        String nome,
        int ordem,
        boolean ativa
) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(), categoria.getNome(), categoria.getOrdem(), categoria.isAtiva());
    }
}
