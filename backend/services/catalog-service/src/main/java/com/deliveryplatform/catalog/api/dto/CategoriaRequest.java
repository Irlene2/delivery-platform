package com.deliveryplatform.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * O pedido de uma seção nova. <b>A forma é daqui; a regra é do agregado.</b> Nome em
 * branco é forma — nulo e vazio vindos de fora não chegam ao domínio (ADR-063) —, e o
 * {@code Textos.exigirPreenchido} da {@code Categoria} fica como segunda rede.
 *
 * <p>A {@code ordem} é obrigatória e aceita qualquer inteiro: o agregado não tem regra
 * sobre ela (ADR-064), e a listagem desempata por nome. Não nasce ativa ou inativa por
 * escolha do corpo — categoria nova nasce ativa.
 */
@Schema(description = "Uma seção nova do cardápio. Nasce ativa.")
public record CategoriaRequest(

        @Schema(description = "O nome da seção.", example = "Pizzas salgadas")
        @NotBlank(message = "nome é obrigatório")
        String nome,

        @Schema(description = "A posição da seção no cardápio. Empates se resolvem pelo nome.",
                example = "0")
        @NotNull(message = "ordem é obrigatória")
        Integer ordem
) {
}
