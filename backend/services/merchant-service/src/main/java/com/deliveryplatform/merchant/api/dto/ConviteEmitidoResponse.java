package com.deliveryplatform.merchant.api.dto;

import com.deliveryplatform.merchant.domain.model.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

/**
 * O convite recém-emitido — <b>a única resposta deste serviço que carrega o token</b>
 * (ADR-069 §1). Token é credencial: lido aqui, uma vez, e nunca mais. A listagem da equipe
 * não o traz, a leitura pública não o traz, e nenhum log o escreve.
 *
 * <p><b>Vai o token, e não o link montado.</b> O endereço da página do convite é do front,
 * que este serviço não conhece; quem monta o link é a tela que mostra o convite para
 * copiar.
 */
@Schema(description = "O convite emitido. O token só aparece nesta resposta.")
public record ConviteEmitidoResponse(
        @Schema(description = "A credencial do convite. Mostre para copiar; não há como reler.")
        String token,
        @Schema(description = "Até quando o convite vale.")
        Instant expiraEm,
        Set<Permissao> permissoes
) {
}
