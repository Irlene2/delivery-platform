package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Produto;

import java.util.UUID;

/** Põe ou troca a foto de um produto (ADR-077, emenda de 01/11/2026). */
public interface IlustrarProduto {

    Produto ilustrar(UUID estabelecimentoId, UUID produtoId, String tipo, byte[] conteudo);
}
