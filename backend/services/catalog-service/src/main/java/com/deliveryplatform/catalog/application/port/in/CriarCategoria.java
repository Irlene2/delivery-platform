package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Categoria;

import java.util.UUID;

public interface CriarCategoria {

    Categoria criar(UUID estabelecimentoId, String nome, int ordem);
}
