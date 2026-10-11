package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Categoria;

import java.util.List;
import java.util.UUID;

public interface ListarCategorias {

    List<Categoria> daLoja(UUID estabelecimentoId);
}
