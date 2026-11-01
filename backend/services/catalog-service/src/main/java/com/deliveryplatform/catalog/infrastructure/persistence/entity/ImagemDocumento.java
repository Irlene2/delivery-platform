package com.deliveryplatform.catalog.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

/**
 * A foto de um produto, numa coleção própria — nunca dentro do documento do produto
 * ({@code catalogo.md} §9). O {@code _id} é o do produto: uma foto por produto, e gravar de
 * novo substitui.
 */
@Document(collection = "imagens_de_produto")
public record ImagemDocumento(
        @Id UUID produtoId,
        UUID estabelecimentoId,
        String tipo,
        byte[] conteudo) {
}
