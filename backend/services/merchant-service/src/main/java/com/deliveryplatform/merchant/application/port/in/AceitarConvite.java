package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Membro;

import java.util.UUID;

/** Aceitar um convite: o vínculo nasce, {@code COLABORADOR} (ADR-069 §4, B2). */
public interface AceitarConvite {

    Membro aceitar(String token, UUID usuarioId);
}
