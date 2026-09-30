package com.deliveryplatform.merchant.application.exception;

/**
 * A loja não abre por horário, então não há expediente para carimbar.
 *
 * <p>Horário vazio é <b>válido</b> — o {@code estabelecimento.md} §4 diz que
 * significa "nunca abre por horário", e quem opera só por aceite manual
 * cadastra exatamente isso. A ADR-046 já registrava a consequência: <i>"Loja sem
 * horário nunca abre expediente, e portanto nunca reativa nada"</i>.
 *
 * <p><b>Por que 409 e não 404.</b> A loja existe; o que não existe é um
 * expediente. Pelo critério que o {@code catalogo.md} §5 usa para separar as
 * duas famílias, isto é <i>estado do mundo</i>, não erro do chamador — ele
 * pediu uma coisa legítima sobre uma loja legítima, e a resposta é que aquela
 * loja não tem o que ele pediu.
 *
 * <p><b>Por que não devolver uma data assim mesmo.</b> Qualquer data que eu
 * escolhesse aqui seria comparada, amanhã, com a de um evento de abertura que
 * nunca vai chegar — e o produto marcado ficaria esgotado para sempre, sem erro
 * em lugar nenhum. É a mesma razão pela qual o {@code marcarOpcao} do catálogo
 * estoura com id desconhecido em vez de engolir em silêncio.
 *
 * <p>Quem recebe este 409 decide o que fazer. A G-C2 vai ter de decidir, e a
 * resposta provável é que uma loja sem horário só aceite
 * {@code ESGOTADO_INDETERMINADO} — mas isso é decisão daquela rodada, com o caso
 * na frente.
 */
public class SemExpedientePorHorario extends RuntimeException {

    public SemExpedientePorHorario() {
        super("a loja não abre por horário: não há expediente para carimbar");
    }
}
