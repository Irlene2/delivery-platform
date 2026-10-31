package com.deliveryplatform.order.domain.exception;

import com.deliveryplatform.order.domain.model.EstadoDoPedido;

/** Transição fora da tabela do pedido. Na API vira HTTP 409. */
public class TransicaoInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransicaoInvalidaException(EstadoDoPedido de, EstadoDoPedido para) {
        super("Transição inválida do pedido: " + de + " -> " + para);
    }
}
