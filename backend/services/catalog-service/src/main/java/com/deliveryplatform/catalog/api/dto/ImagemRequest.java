package com.deliveryplatform.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * A foto do produto, em Base64 dentro do JSON (ADR-077, emenda de 01/11/2026). JSON e não
 * multipart para a rota ter a mesma forma das outras escritas do catálogo; a tela reduz a foto
 * antes (lado maior de 800 px), e o limite de 600 KB é do domínio.
 */
@Schema(description = "A foto do produto: o tipo e o conteúdo em Base64.")
public record ImagemRequest(

        @Schema(description = "image/jpeg, image/png ou image/webp.", example = "image/jpeg")
        @NotBlank(message = "tipo é obrigatório")
        String tipo,

        @Schema(description = "O arquivo em Base64, sem o prefixo data:.")
        @NotBlank(message = "conteudo é obrigatório")
        String conteudo
) {
}
