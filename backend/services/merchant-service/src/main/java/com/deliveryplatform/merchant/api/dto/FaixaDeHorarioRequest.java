package com.deliveryplatform.merchant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/** Um turno, em hora civil no fuso da loja. Início inclusivo, fim exclusivo. */
public record FaixaDeHorarioRequest(

        @Schema(description = "Hora de abrir, no fuso da loja.", example = "18:00")
        @NotNull(message = "inicio é obrigatório")
        LocalTime inicio,

        @Schema(description = "Hora de fechar. Antes do início, a faixa cruza a meia-noite.",
                example = "02:00")
        @NotNull(message = "fim é obrigatório")
        LocalTime fim
) {
}
