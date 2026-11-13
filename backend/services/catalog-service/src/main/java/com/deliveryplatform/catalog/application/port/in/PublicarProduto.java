package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Produto;

import java.util.UUID;

public interface PublicarProduto {

    Produto publicar(UUID estabelecimentoId, UUID produtoId);

    /**
     * Tira do cardápio o que estava publicado (13/11/2026): vira {@code INATIVO}, e volta pelo
     * {@link #publicar}. Exige {@code DESATIVAR_PRODUTO}. Rascunho recusa — nunca apareceu.
     */
    Produto retirar(UUID estabelecimentoId, UUID produtoId);
}
