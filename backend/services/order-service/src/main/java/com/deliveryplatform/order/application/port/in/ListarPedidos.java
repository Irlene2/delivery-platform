package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Os pedidos de um dia da loja, do mais novo para o mais velho. Exige {@code VER_PEDIDO}. */
public interface ListarPedidos {

    /**
     * @param status nulo = todos
     * @param dia    nulo = hoje, no fuso da loja
     */
    List<Pedido> daLoja(UUID estabelecimentoId, StatusDoPedido status, LocalDate dia);
}
