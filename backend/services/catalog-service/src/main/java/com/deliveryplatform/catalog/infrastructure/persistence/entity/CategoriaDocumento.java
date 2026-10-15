package com.deliveryplatform.catalog.infrastructure.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

/**
 * A categoria gravada (ADR-064). Os quatro campos do {@code catalogo.md} §1, o id e a
 * versão.
 *
 * <p><b>Com {@code @Version}</b>, pelo precedente do {@link ProdutoDocumento}
 * (ADR-052) — e o gatilho da ADR-052, <i>"a segunda coleção com mais de um escritor
 * concorrente"</i>, ainda não disparou: hoje a categoria só nasce, e nada a altera. A
 * versão está aqui para que renomear, reordenar e desativar, quando ganharem rota,
 * já encontrem a guarda pronta, e não uma coleção com documentos sem o campo.
 */
@Document(collection = "categorias")
public record CategoriaDocumento(
        @Id UUID id,
        UUID estabelecimentoId,
        String nome,
        int ordem,
        boolean ativa,
        /* Long e não long: nula diz ao Spring Data que o documento é novo (ADR-052). */
        @Version Long versao
) {
}
