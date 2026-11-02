package com.deliveryplatform.order.application.exception;

/**
 * O que o entregador não faz pelo app dele (400), e a loja faz pelo painel:
 *
 * <ul>
 *   <li>{@link Motivo#STATUS}: ele só marca "saiu para entrega" e "entregue" — confirmar,
 *       preparar e cancelar são da loja.</li>
 * </ul>
 *
 * <p>O Pix deixou de ser recusa em 02/11/2026: o entregador o informa, e ele fica a conferir
 * pela loja ({@code Pedido#registrarPixInformado}).</p>
 */
public class RecusaAoEntregador extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo { STATUS }

    private final Motivo motivo;

    public RecusaAoEntregador(Motivo motivo) {
        super("entregador: " + motivo);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
