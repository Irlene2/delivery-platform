package com.deliveryplatform.order.domain.model;

/** Como o pedido chega ao cliente. A modalidade é explícita, nunca deduzida (pedido.md §1). */
public enum Modalidade {
    ENTREGA,
    RETIRADA
}
