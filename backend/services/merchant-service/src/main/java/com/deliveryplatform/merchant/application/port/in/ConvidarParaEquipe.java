package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Convite;
import com.deliveryplatform.merchant.domain.model.Permissao;

import java.util.Set;
import java.util.UUID;

/** Emitir um convite para a equipe de uma loja (ADR-069). */
public interface ConvidarParaEquipe {

    Convite convidar(UUID lojaId, UUID autorUsuarioId, String telefone, Set<Permissao> oferecidas);
}
