package com.deliveryplatform.order.application.port.out;

import com.deliveryplatform.valuetypes.Money;

import java.util.UUID;

/**
 * Um produto publicado, no recorte que o pedido usa: o nome e o preço que vão ser congelados
 * no item, e se ele pode ser vendido agora.
 */
public record ProdutoDoCardapio(UUID id, String nome, Money preco, boolean vendavel) {
}
