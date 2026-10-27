package com.deliveryplatform.order.application.exception;

/**
 * 404 — o pedido não existe, ou existe e é de outra loja. A mesma resposta para os dois
 * (ADR-056), e a mensagem é fixa para não dizer qual.
 */
public class PedidoNaoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PedidoNaoEncontrado() {
        super("pedido não encontrado");
    }
}
