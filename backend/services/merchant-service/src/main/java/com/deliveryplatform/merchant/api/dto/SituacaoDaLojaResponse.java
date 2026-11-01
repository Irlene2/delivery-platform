package com.deliveryplatform.merchant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** O que a vitrine mostra e o {@code order} confere: o nome, se está aberta, e quando abre. */
@Schema(description = "Se a loja está recebendo pedidos agora.")
public record SituacaoDaLojaResponse(

        @Schema(description = "O nome da loja, para o topo do cardápio.", example = "Pizzaria da Marli")
        String nome,

        @Schema(description = "Dentro do horário e sem pausa: aceita pedido agora.")
        boolean aberta,

        @Schema(description = "Com a loja fechada, o próximo instante em que ela abre. Nulo quando "
                + "está aberta, ou quando a loja não abre por horário (ainda não definiu o horário).",
                example = "2026-11-01T21:00:00Z", nullable = true)
        Instant proximaAbertura
) {
}
