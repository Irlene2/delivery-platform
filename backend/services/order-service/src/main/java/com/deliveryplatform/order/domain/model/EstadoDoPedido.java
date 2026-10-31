package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.TransicaoInvalidaException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Estados do pedido e as transições permitidas (docs/dominio/pedido.md, seções 2 e 3). */
public enum EstadoDoPedido {
    RECEBIDO,
    CONFIRMADO,
    PAGO,
    AGUARDANDO_ESTOQUE,
    EM_PREPARO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    AGUARDANDO_CLIENTE,
    NAO_ENTREGUE,
    ENTREGUE,
    RETIRADO,
    CANCELADO;

    private static final Map<EstadoDoPedido, Set<EstadoDoPedido>> SAIDAS =
            new EnumMap<>(EstadoDoPedido.class);

    static {
        SAIDAS.put(RECEBIDO, EnumSet.of(CONFIRMADO, PAGO, AGUARDANDO_ESTOQUE, CANCELADO));
        SAIDAS.put(AGUARDANDO_ESTOQUE, EnumSet.of(CONFIRMADO, CANCELADO));
        SAIDAS.put(CONFIRMADO, EnumSet.of(EM_PREPARO, CANCELADO));
        SAIDAS.put(PAGO, EnumSet.of(EM_PREPARO, CANCELADO));
        SAIDAS.put(EM_PREPARO, EnumSet.of(EM_PREPARO, PRONTO, CANCELADO));
        SAIDAS.put(PRONTO, EnumSet.of(SAIU_PARA_ENTREGA, AGUARDANDO_CLIENTE, CANCELADO));
        SAIDAS.put(SAIU_PARA_ENTREGA, EnumSet.of(ENTREGUE, NAO_ENTREGUE));
        SAIDAS.put(NAO_ENTREGUE, EnumSet.of(SAIU_PARA_ENTREGA, ENTREGUE, CANCELADO));
        SAIDAS.put(AGUARDANDO_CLIENTE, EnumSet.of(RETIRADO, CANCELADO));
        // ENTREGUE, RETIRADO e CANCELADO são terminais: não têm saída.
    }

    public Set<EstadoDoPedido> saidas() {
        return Collections.unmodifiableSet(
                SAIDAS.getOrDefault(this, EnumSet.noneOf(EstadoDoPedido.class)));
    }

    public boolean podeIrPara(EstadoDoPedido destino) {
        return saidas().contains(destino);
    }

    public boolean terminal() {
        return saidas().isEmpty();
    }

    public EstadoDoPedido irPara(EstadoDoPedido destino) {
        if (!podeIrPara(destino)) {
            throw new TransicaoInvalidaException(this, destino);
        }
        return destino;
    }
}
