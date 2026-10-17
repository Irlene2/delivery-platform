package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Disponibilidade;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Substitui o horário semanal da loja inteiro (ADR-073 §1). Exige
 * {@code CONFIGURAR_LOJA}.
 *
 * <p>As faixas chegam como horas cruas, e não como {@code Faixa}: construir a faixa é
 * onde o domínio pode recusar, e a recusa tem de acontecer dentro do caso de uso, que é
 * o único ponto de tradução.
 */
public interface DefinirHorario {

    Disponibilidade definir(UUID lojaId, UUID autorUsuarioId,
                            Map<DayOfWeek, List<FaixaPedida>> horarioDeFuncionamento);

    record FaixaPedida(LocalTime inicio, LocalTime fim) {
    }
}
