package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.Pedido;

import java.util.UUID;

/** O cliente acompanha pelo código que recebeu ao criar. Inexistente: 404. */
public interface AcompanharPedido {

    Pedido porCodigo(UUID codigoDeAcompanhamento);
}
