package com.deliveryplatform.order.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Os estados do pedido nesta entrega — o recorte do {@code docs/dominio/pedido.md} §2
 * descrito em {@code docs/entrega-15-11/pedido.md} §4.
 *
 * <p>Fora, de propósito: {@code PAGO} (marco 8), {@code AGUARDANDO_ESTOQUE} (marco 10),
 * {@code NAO_ENTREGUE} e os subestados de {@code EM_PREPARO}. Valor sem emissor não entra
 * no enum (a regra que o {@code ModoDeControle} do catalog escreveu).
 */
public enum StatusDoPedido {
    RECEBIDO,
    CONFIRMADO,
    EM_PREPARO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    AGUARDANDO_CLIENTE,
    ENTREGUE,
    RETIRADO,
    CANCELADO;

    private static final Set<StatusDoPedido> FINAIS = EnumSet.of(ENTREGUE, RETIRADO, CANCELADO);
    private static final Set<StatusDoPedido> CANCELAVEIS = EnumSet.of(RECEBIDO, CONFIRMADO, EM_PREPARO);
    private static final Set<StatusDoPedido> ACEITAM_PAGAMENTO =
            EnumSet.of(PRONTO, SAIU_PARA_ENTREGA, AGUARDANDO_CLIENTE);

    public boolean encerrado() {
        return FINAIS.contains(this);
    }

    public boolean cancelavel() {
        return CANCELAVEIS.contains(this);
    }

    /** O pagamento é registrado na entrega ou no balcão: do pronto até antes de encerrar. */
    public boolean aceitaPagamento() {
        return ACEITAM_PAGAMENTO.contains(this);
    }
}
