package com.deliveryplatform.order.application.exception;

/**
 * O que o entregador não faz pelo app dele (400), e a loja faz pelo painel:
 *
 * <ul>
 *   <li>{@link Motivo#STATUS}: ele só marca "saiu para entrega" e "entregue" — confirmar,
 *       preparar e cancelar são da loja;</li>
 *   <li>{@link Motivo#PIX}: o Pix cai na conta da loja, e só quem vê o extrato o confere
 *       (pedido.md §5).</li>
 * </ul>
 */
public class RecusaAoEntregador extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo { STATUS, PIX }

    private final Motivo motivo;

    public RecusaAoEntregador(Motivo motivo) {
        super("entregador: " + motivo);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
