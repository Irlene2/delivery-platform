package com.deliveryplatform.order.application.port.out;

/**
 * O {@code merchant} não disse se a loja está aberta. Falha fechada: o pedido <b>não</b> nasce
 * (503), porque o risco que a regra existe para evitar é o pedido de madrugada que ninguém vê.
 */
public class SituacaoIndisponivel extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SituacaoIndisponivel(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
