package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Papel;

import java.time.Instant;

/**
 * O convite, para quem recebeu o link e ainda não tem sessão (ADR-069 §2): o mínimo que
 * convence, e nada que identifique pessoa.
 */
public interface ConsultarConvite {

    ConviteParaQuemRecebe porToken(String token);

    /** O nome da loja, o papel com que se entra, e até quando vale. Nada mais. */
    record ConviteParaQuemRecebe(String nomeDaLoja, Papel papel, Instant expiraEm) {
    }
}
