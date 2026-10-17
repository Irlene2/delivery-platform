package com.deliveryplatform.merchant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * O horário semanal inteiro, que substitui o que havia (ADR-073 §1). O dia é o nome do
 * {@code DayOfWeek} em inglês — {@code MONDAY} a {@code SUNDAY} —, e dia desconhecido
 * não chega ao domínio: falha na leitura do corpo, com 400.
 */
public record HorarioRequest(

        @Schema(description = "Por dia da semana, as faixas daquele dia. Mapa vazio volta a "
                + "loja ao estado sem horário — \"nunca abre por horário\". Faixas que se "
                + "sobrepõem são aceitas; fim antes do início é a faixa que cruza a meia-noite.")
        @NotNull(message = "horarioDeFuncionamento é obrigatório")
        Map<DayOfWeek, @NotNull List<@NotNull @Valid FaixaDeHorarioRequest>> horarioDeFuncionamento
) {
}
