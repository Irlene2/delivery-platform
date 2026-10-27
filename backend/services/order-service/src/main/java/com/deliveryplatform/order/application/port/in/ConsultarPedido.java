package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.Pedido;

import java.util.UUID;

/** Um pedido da loja, inteiro — com telefone e endereço. Exige {@code VER_PEDIDO}. */
public interface ConsultarPedido {

    Pedido consultar(UUID estabelecimentoId, UUID pedidoId);
}
