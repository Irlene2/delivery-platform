package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.valuetypes.Money;

import java.time.Instant;

/**
 * O pagamento como aconteceu — registrado por quem entregou ou atendeu no balcão.
 *
 * <p>A forma efetiva pode ser diferente da declarada: o cliente disse dinheiro e pagou
 * no cartão porque não tinha troco. As duas ficam (planejamento §1.3).
 *
 * @param valorRecebido em dinheiro, a nota recebida; nulo nas outras formas
 * @param trocoDevolvido calculado aqui, nunca informado de fora
 * @param pixConferido o operador conferiu no extrato. Comprovante na tela não vale
 */
public record PagamentoRegistrado(
        FormaDePagamento formaEfetiva,
        Money valorRecebido,
        Money trocoDevolvido,
        boolean pixConferido,
        Instant registradoEm) {
}
