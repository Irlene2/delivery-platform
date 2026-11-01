package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.valuetypes.Money;

import java.util.UUID;

/** Corrige o cadastro de um produto: seção, nome, preço e descrição (ADR-077, 01/11/2026). */
public interface AlterarProduto {

    Produto alterar(UUID estabelecimentoId, UUID produtoId, Alteracao alteracao);

    record Alteracao(UUID categoriaId, String nome, String descricao, Money precoBase) {
    }
}
