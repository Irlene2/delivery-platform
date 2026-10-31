package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "O que o entregador recebeu na porta: DINHEIRO ou CARTAO. Pix é da loja.")
public record RecebimentoRequest(
        @Schema(example = "DINHEIRO")
        @NotNull(message = "formaEfetiva é obrigatória")
        FormaDePagamento formaEfetiva,
        @Schema(description = "Só em DINHEIRO, e maior ou igual ao total.", example = "110.00")
        BigDecimal valorRecebido) {
}
