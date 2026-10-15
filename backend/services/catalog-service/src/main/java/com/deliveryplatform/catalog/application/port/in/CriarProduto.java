package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.valuetypes.Money;

import java.util.UUID;

public interface CriarProduto {

    Produto criar(UUID estabelecimentoId, Pedido pedido);

    /** O que o corpo trouxe, já na forma do domínio. A {@code descricao} é opcional. */
    record Pedido(UUID categoriaId, String nome, String descricao, Money precoBase,
                  ModoDeControle modoDeControle, int ordem) {
    }
}
