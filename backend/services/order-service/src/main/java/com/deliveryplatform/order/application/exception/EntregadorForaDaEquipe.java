package com.deliveryplatform.order.application.exception;

/**
 * O pedido foi entregue a um {@code usuarioId} que nunca se apresentou como entregador desta
 * loja (400). Ou o identificador veio errado, ou a pessoa ainda não abriu o app.
 */
public class EntregadorForaDaEquipe extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntregadorForaDaEquipe() {
        super("entregador fora da equipe");
    }
}
