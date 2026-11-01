package com.deliveryplatform.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O código do cadastro, devolvido <b>só com a bandeira de desenvolvimento ligada</b>
 * ({@code delivery.cadastro.codigo-na-resposta}, falsa por padrão) — emenda de 01/11/2026 à
 * ADR-042. Fora dela, o 202 sai sem corpo, como sempre.
 */
@Schema(description = "Só no ambiente de desenvolvimento e na demonstração: o código que o "
        + "cadastro pede. Em qualquer outro lugar o 202 vem sem corpo.")
public record CodigoDeDemonstracaoResponse(
        @Schema(description = "Vazio quando o telefone já tem conta.", example = "482913", nullable = true)
        String codigoDeDemonstracao) {
}
