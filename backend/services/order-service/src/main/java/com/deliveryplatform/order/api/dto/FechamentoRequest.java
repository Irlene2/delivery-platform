package com.deliveryplatform.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fechar o caixa (ADR-077, emenda de 01/11/2026). <b>Nenhum total vem daqui</b>: o servidor
 * apura o dia a partir dos pedidos. O corpo traz só o que é de quem fecha.
 */
@Schema(description = "O dia a fechar e, se quiser, a conferência do caixa.")
public record FechamentoRequest(
        @Schema(description = "Nulo = hoje, no fuso da loja.", example = "2026-11-05", nullable = true)
        LocalDate dia,
        @Schema(description = "Opcional: o dinheiro contado no caixa, para conferir com as vendas "
                + "em dinheiro.", example = "320.00", nullable = true)
        BigDecimal dinheiroContado,
        @Schema(description = "Opcional.", example = "Faltou troco às 21h", nullable = true)
        @Size(max = 500, message = "a observação tem no máximo 500 caracteres")
        String observacao) {
}
