package com.deliveryplatform.order.application.exception;

import java.time.Instant;
import java.util.Optional;

/**
 * Pedido feito fora do horário da loja (ADR-077, emenda de 01/11/2026). Carrega a próxima
 * abertura, quando há, para a frase dizer quando voltar; a frase é do {@code TratadorDeErros}.
 */
public class LojaFechada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Optional<Instant> proximaAbertura;

    public LojaFechada(Optional<Instant> proximaAbertura) {
        super("a loja está fechada" + proximaAbertura.map(p -> "; abre em " + p).orElse(""));
        this.proximaAbertura = proximaAbertura;
    }

    public Optional<Instant> proximaAbertura() {
        return proximaAbertura;
    }
}
