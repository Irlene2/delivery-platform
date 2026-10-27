package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.valuetypes.Money;

import java.util.UUID;

/**
 * O que a loja faz com o pedido depois que ele chega. Exige {@code ALTERAR_STATUS}; pedido de
 * outra loja é 404 (ADR-056).
 */
public interface OperarPedido {

    Pedido avancar(UUID estabelecimentoId, UUID pedidoId, StatusDoPedido destino, String motivo);

    Pedido informarEntregador(UUID estabelecimentoId, UUID pedidoId, String nome);

    Pedido registrarPagamento(UUID estabelecimentoId, UUID pedidoId, FormaDePagamento forma,
                              Money valorRecebido, boolean pixConferido);
}
