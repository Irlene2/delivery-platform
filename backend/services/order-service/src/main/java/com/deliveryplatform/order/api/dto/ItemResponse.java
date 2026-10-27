package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.ItemDoPedido;

import java.math.BigDecimal;
import java.util.UUID;

/** O item com o nome e o preço congelados no instante do pedido. */
public record ItemResponse(UUID produtoId, String nome, BigDecimal precoUnitario, int quantidade,
                           String observacao, BigDecimal subtotal) {

    public static ItemResponse de(ItemDoPedido item) {
        return new ItemResponse(item.produtoId(), item.nome(), item.precoUnitario().valor(),
                item.quantidade(), item.observacao(), item.subtotal().valor());
    }
}
