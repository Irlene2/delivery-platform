package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.valuetypes.Money;

import java.util.Objects;

/**
 * Como o cliente disse que vai pagar. {@code trocoPara} só existe em dinheiro: é o valor
 * da nota que o entregador vai receber, para levar o troco (planejamento §8.3).
 */
public record PagamentoDeclarado(FormaDePagamento forma, Money trocoPara) {

    public PagamentoDeclarado {
        Objects.requireNonNull(forma, "forma");
    }
}
