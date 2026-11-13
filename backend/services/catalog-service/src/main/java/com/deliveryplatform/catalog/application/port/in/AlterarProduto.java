package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.valuetypes.Money;

import java.util.UUID;

/**
 * Corrige o cadastro de um produto: seção, nome, preço e descrição (ADR-077, 01/11/2026); e,
 * desde 13/11/2026, o "pode acabar" e o selo da vitrine, quando vierem.
 */
public interface AlterarProduto {

    Produto alterar(UUID estabelecimentoId, UUID produtoId, Alteracao alteracao);

    /**
     * @param modoDeControle nulo mantém o que está
     * @param selo nulo mantém o que está; vazio tira o selo
     */
    record Alteracao(UUID categoriaId, String nome, String descricao, Money precoBase,
                     ModoDeControle modoDeControle, String selo) {

        /** A correção sem controle nem selo — a forma de 01/11/2026. */
        public Alteracao(UUID categoriaId, String nome, String descricao, Money precoBase) {
            this(categoriaId, nome, descricao, precoBase, null, null);
        }
    }
}
