package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.StatusDoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "O próximo status. O motivo só é lido — e é obrigatório — no cancelamento.")
public record StatusRequest(
        @Schema(example = "EM_PREPARO")
        @NotNull(message = "status é obrigatório")
        StatusDoPedido status,

        @Schema(example = "cliente desistiu")
        String motivo
) {
}
