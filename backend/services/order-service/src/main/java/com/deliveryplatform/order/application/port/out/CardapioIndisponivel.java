package com.deliveryplatform.order.application.port.out;

/**
 * O {@code catalog} não respondeu. O pedido <b>não</b> é criado: sem o preço do servidor, a
 * única alternativa seria confiar no preço do navegador, e essa é a que não existe (503).
 */
public class CardapioIndisponivel extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CardapioIndisponivel(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
