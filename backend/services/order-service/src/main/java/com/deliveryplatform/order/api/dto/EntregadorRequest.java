package com.deliveryplatform.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Quem leva o pedido. Só na entrega. Da equipe, pelo usuarioId; sem conta, "
        + "pelo nome.")
public record EntregadorRequest(
        @Schema(description = "Um entregador da equipe (GET .../orders/couriers). Com ele, o nome "
                + "é o que ele cadastrou, e o pedido aparece no app dele.")
        UUID usuarioId,
        @Schema(description = "Só quando não há usuarioId: o entregador avulso.", example = "Carlos")
        String nome) {
}
