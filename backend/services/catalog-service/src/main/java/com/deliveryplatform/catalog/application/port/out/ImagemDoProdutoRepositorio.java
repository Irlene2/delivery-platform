package com.deliveryplatform.catalog.application.port.out;

import com.deliveryplatform.catalog.domain.model.ImagemDoProduto;

import java.util.Optional;
import java.util.UUID;

/** Uma foto por produto, guardada à parte do documento do produto. Gravar de novo substitui. */
public interface ImagemDoProdutoRepositorio {

    void salvar(ImagemDoProduto imagem);

    Optional<ImagemDoProduto> buscar(UUID produtoId);
}
