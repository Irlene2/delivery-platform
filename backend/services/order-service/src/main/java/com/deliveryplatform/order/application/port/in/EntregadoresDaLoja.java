package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.EntregadorDaLoja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Quem entrega pela loja. A loja lista (VER_PEDIDO); o entregador se apresenta (VER_ENTREGA). */
public interface EntregadoresDaLoja {

    List<EntregadorDaLoja> listar(UUID estabelecimentoId);

    /** Eu, se já me apresentei nesta loja. */
    Optional<EntregadorDaLoja> eu(UUID estabelecimentoId);

    /** Grava (ou troca) o nome com que a loja me reconhece. */
    EntregadorDaLoja apresentar(UUID estabelecimentoId, String nome);
}
