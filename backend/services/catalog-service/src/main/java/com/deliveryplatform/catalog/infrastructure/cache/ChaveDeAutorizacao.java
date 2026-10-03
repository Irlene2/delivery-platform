package com.deliveryplatform.catalog.infrastructure.cache;

import java.util.UUID;

/**
 * A chave do cache: <b>o par</b> que a ADR-011 nomeia — <i>"Chave:
 * {@code (usuarioId, estabelecimentoId)}"</i>.
 *
 * <h2>O portador é texto, e não {@code UUID}</h2>
 *
 * <p>Quem lê o cache tira o portador do {@code sub} do token, e <b>o {@code sub}
 * pode não ser um {@code UUID}</b> — o teste
 * {@code token_com_sub_que_nao_e_uuid_atravessa_a_cadeia}, da G-B3, passa
 * exatamente um assim, e ele atravessa a cadeia de filtros porque quem converte
 * o {@code sub} é o {@code merchant}, não este serviço (ADR-038).
 *
 * <p>Converter aqui criaria um caminho que estoura antes de negar, e negar é o
 * que se quer. Como texto, um {@code sub} malformado vira uma chave que nenhum
 * evento vai invalidar — e não precisa: o {@code merchant} responde 403 para
 * ele, o negativo dura 10 s, e nenhum {@code VinculoAlteradoV1} jamais falará
 * dessa pessoa, porque ela não existe.
 *
 * <p>Do outro lado, quem invalida tem o {@code usuarioId} como {@code UUID} no
 * payload do evento, e usa {@link #doEvento}. As duas formas coincidem quando o
 * {@code sub} é um {@code UUID} — que é o caso de todo usuário de verdade,
 * porque é o {@code identity} que o emite (ADR-037).
 */
public record ChaveDeAutorizacao(String portador, UUID estabelecimentoId) {

    public ChaveDeAutorizacao {
        if (portador == null || portador.isBlank()) {
            throw new IllegalArgumentException("portador vazio na chave do cache");
        }
        if (estabelecimentoId == null) {
            throw new IllegalArgumentException("estabelecimento nulo na chave do cache");
        }
    }

    /** A chave vista pelo lado de quem invalida, onde o usuário é {@code UUID}. */
    public static ChaveDeAutorizacao doEvento(UUID usuarioId, UUID estabelecimentoId) {
        return new ChaveDeAutorizacao(usuarioId.toString(), estabelecimentoId);
    }
}
