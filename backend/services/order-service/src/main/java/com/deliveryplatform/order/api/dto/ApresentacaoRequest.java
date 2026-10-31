package com.deliveryplatform.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "O nome com que a loja vai reconhecer o entregador.")
public record ApresentacaoRequest(@Schema(example = "Carlos Moto") String nome) {
}
