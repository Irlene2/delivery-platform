package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.ImagemDoProduto;

import java.util.Optional;
import java.util.UUID;

/**
 * A foto, para quem olha o cardápio — sem token. Vazia quando o produto não tem foto ou não é
 * daquela loja: a mesma resposta, como no resto do catálogo (ADR-056).
 */
public interface ConsultarImagemDoProduto {

    Optional<ImagemDoProduto> daLoja(UUID estabelecimentoId, UUID produtoId);
}
