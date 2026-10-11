package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Produto;

import java.util.UUID;

public interface PublicarProduto {

    Produto publicar(UUID estabelecimentoId, UUID produtoId);
}
