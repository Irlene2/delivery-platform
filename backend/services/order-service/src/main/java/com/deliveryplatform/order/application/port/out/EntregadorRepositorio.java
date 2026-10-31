package com.deliveryplatform.order.application.port.out;

import com.deliveryplatform.order.domain.model.EntregadorDaLoja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntregadorRepositorio {

    Optional<EntregadorDaLoja> buscar(UUID estabelecimentoId, UUID usuarioId);

    /** Os entregadores que já se apresentaram nesta loja, pelo nome. */
    List<EntregadorDaLoja> daLoja(UUID estabelecimentoId);

    EntregadorDaLoja salvar(EntregadorDaLoja entregador);
}
