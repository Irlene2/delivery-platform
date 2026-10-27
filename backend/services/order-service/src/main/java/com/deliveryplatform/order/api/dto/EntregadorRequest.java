package com.deliveryplatform.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quem leva o pedido. Só na entrega, e antes de sair.")
public record EntregadorRequest(@Schema(example = "Carlos") String nome) {
}
