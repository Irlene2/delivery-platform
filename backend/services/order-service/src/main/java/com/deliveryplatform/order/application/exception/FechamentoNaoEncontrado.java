package com.deliveryplatform.order.application.exception;

/** O dia pedido não foi fechado (404). */
public class FechamentoNaoEncontrado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FechamentoNaoEncontrado() {
        super("este dia ainda não foi fechado");
    }
}
