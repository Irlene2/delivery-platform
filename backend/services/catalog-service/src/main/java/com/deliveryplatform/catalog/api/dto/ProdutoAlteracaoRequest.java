package com.deliveryplatform.catalog.api.dto;

import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A correção do cadastro de um produto (ADR-077, emenda de 01/11/2026). Os quatro campos que a
 * tela de edição mostra, inteiros: quem não quer mudar um deles manda o valor que já está.
 * Só forma aqui, como no {@link ProdutoRequest}; a regra é do agregado e do caso de uso.
 */
@Schema(description = "O cadastro corrigido: seção, nome, descrição e preço; e, se vierem, o controle e o selo.")
public record ProdutoAlteracaoRequest(

        @Schema(description = "A seção do cardápio, que tem de ser desta loja.")
        @NotNull(message = "categoriaId é obrigatório")
        UUID categoriaId,

        @Schema(description = "O nome como o cliente o lê.", example = "Pizza margherita")
        @NotBlank(message = "nome é obrigatório")
        String nome,

        @Schema(description = "Opcional; vazio apaga a descrição.", example = "Molho, mussarela e manjericão")
        String descricao,

        @Schema(description = "Em reais, duas casas. Num produto publicado, zero é recusado (C1).",
                example = "52.90")
        @NotNull(message = "precoBase é obrigatório")
        BigDecimal precoBase,

        @Schema(description = "Opcional (13/11/2026): se o produto pode acabar. Ausente mantém "
                + "o que está; virar SEM_CONTROLE devolve o produto a DISPONIVEL.",
                nullable = true)
        ModoDeControle modoDeControle,

        @Schema(description = "Opcional (13/11/2026): o selo da vitrine, até 30 letras. Ausente "
                + "mantém o que está; vazio tira o selo.", nullable = true,
                example = "Mais pedido")
        String selo
) {
}
