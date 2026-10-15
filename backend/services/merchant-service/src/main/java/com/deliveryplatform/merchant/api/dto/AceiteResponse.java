package com.deliveryplatform.merchant.api.dto;

import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;

import java.util.Set;
import java.util.UUID;

/** O vínculo que nasceu do aceite: a loja, o papel e as permissões (ADR-069 §4). */
public record AceiteResponse(UUID estabelecimentoId, Papel papel, Set<Permissao> permissoes) {
}
