package com.deliveryplatform.order.application.port.out;

/** O {@code merchant} não respondeu sobre a autorização. Nega igual, mas não é resposta. */
public class AutorizacaoIndisponivel extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AutorizacaoIndisponivel(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
