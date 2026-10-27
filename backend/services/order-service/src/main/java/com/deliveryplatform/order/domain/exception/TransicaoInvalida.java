package com.deliveryplatform.order.domain.exception;

import com.deliveryplatform.order.domain.model.StatusDoPedido;

/**
 * O pedido não está num estado que aceite o que foi pedido (409).
 *
 * <p>Não é corpo malformado: é o pedido ter andado — outra pessoa mudou o status, ou a
 * tela está velha. Por isso é conflito, e não 400.
 */
public class TransicaoInvalida extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        /** A tabela de transições não tem a seta. */
        NAO_PERMITIDA,
        /** {@code SAIU_PARA_ENTREGA} sem entregador informado. */
        FALTA_ENTREGADOR,
        /** {@code ENTREGUE} ou {@code RETIRADO} sem pagamento registrado (R2). */
        FALTA_PAGAMENTO,
        /** Pagamento registrado antes de o pedido ficar pronto, ou depois de encerrado. */
        PAGAMENTO_FORA_DE_HORA,
        /** Entregador informado num pedido encerrado. */
        PEDIDO_ENCERRADO
    }

    private final Motivo motivo;
    private final StatusDoPedido de;
    private final StatusDoPedido para;

    public TransicaoInvalida(Motivo motivo, StatusDoPedido de, StatusDoPedido para) {
        super("transição recusada (" + motivo + "): " + de + " → " + para);
        this.motivo = motivo;
        this.de = de;
        this.para = para;
    }

    public Motivo motivo() {
        return motivo;
    }

    public StatusDoPedido de() {
        return de;
    }

    /** O destino pedido; nulo quando a recusa não é de troca de status. */
    public StatusDoPedido para() {
        return para;
    }
}
