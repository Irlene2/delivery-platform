package com.deliveryplatform.order.domain.exception;

/**
 * O fechamento do dia que não pode acontecer (ADR-077, emenda de 01/11/2026). Carrega o motivo
 * e o dado; a frase é do {@code TratadorDeErros} (ADR-063).
 */
public class FechamentoRecusado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        /** Há pedido do dia que não terminou. dado: quantos. */
        EM_ANDAMENTO,
        /** O dia já foi fechado. */
        JA_FECHADO,
        /** O dia ainda não chegou. */
        DIA_FUTURO,
        /** Contagem de caixa negativa. */
        CONTAGEM_NEGATIVA,
        /** O pedido é de um dia já fechado e não muda mais. */
        DIA_FECHADO
    }

    private final Motivo motivo;
    private final int quantos;

    public FechamentoRecusado(Motivo motivo, int quantos) {
        super(motivo + (quantos > 0 ? " (" + quantos + ")" : ""));
        this.motivo = motivo;
        this.quantos = quantos;
    }

    public Motivo motivo() {
        return motivo;
    }

    public int quantos() {
        return quantos;
    }
}
