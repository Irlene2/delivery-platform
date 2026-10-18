package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Disponibilidade;

import java.util.UUID;

/**
 * O horário semanal da loja, para quem o configura (ADR-073, emenda de 17/10/2026). Exige
 * {@code CONFIGURAR_LOJA}, como a escrita: quem lê é quem vai trocá-lo.
 */
public interface ConsultarHorario {

    Disponibilidade daLoja(UUID lojaId, UUID autorUsuarioId);
}
