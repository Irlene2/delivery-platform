package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.EntregadorDaLoja;

import java.util.UUID;

/** Um entregador da equipe: a conta e o nome com que a loja o reconhece. */
public record EntregadorResponse(UUID usuarioId, String nome) {

    public static EntregadorResponse de(EntregadorDaLoja entregador) {
        return new EntregadorResponse(entregador.usuarioId(), entregador.nome());
    }
}
