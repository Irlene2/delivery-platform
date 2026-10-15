package com.deliveryplatform.merchant.api.dto;

import com.deliveryplatform.merchant.domain.model.Papel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * O convite, para quem recebeu o link (ADR-069 §2). <b>Três campos, e só</b>: um campo novo
 * aqui sai numa rota pública, e o {@code ConviteIT} afirma o conjunto inteiro para que um
 * acréscimo distraído fique vermelho. Sem token, sem telefone, sem identificador de pessoa.
 */
@Schema(description = "O mínimo que convence: a loja, o papel e até quando vale.")
public record ConvitePublicoResponse(
        String nomeDaLoja,
        @Schema(description = "Sempre COLABORADOR: convite nunca concede outro papel.")
        Papel papel,
        Instant expiraEm
) {
}
