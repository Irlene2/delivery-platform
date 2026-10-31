package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.StatusDoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "SAIU_PARA_ENTREGA ou ENTREGUE — os dois passos que são do entregador.")
public record StatusDaEntregaRequest(
        @Schema(example = "ENTREGUE")
        @NotNull(message = "status é obrigatório")
        StatusDoPedido status) {
}
