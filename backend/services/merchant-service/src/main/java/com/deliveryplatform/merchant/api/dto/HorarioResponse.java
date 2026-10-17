package com.deliveryplatform.merchant.api.dto;

import com.deliveryplatform.merchant.domain.model.Disponibilidade;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * O horário como ficou gravado — normalizado pelo domínio: faixas de cada dia em ordem
 * de início, segundos truncados, dia sem faixa omitido.
 */
public record HorarioResponse(Map<DayOfWeek, List<FaixaDeHorarioResponse>> horarioDeFuncionamento) {

    public record FaixaDeHorarioResponse(LocalTime inicio, LocalTime fim) {
    }

    public static HorarioResponse de(Disponibilidade disponibilidade) {
        Map<DayOfWeek, List<FaixaDeHorarioResponse>> porDia = new EnumMap<>(DayOfWeek.class);
        disponibilidade.horarioDeFuncionamento().forEach((dia, faixas) -> porDia.put(dia,
                faixas.stream().map(f -> new FaixaDeHorarioResponse(f.inicio(), f.fim())).toList()));
        return new HorarioResponse(porDia);
    }
}
