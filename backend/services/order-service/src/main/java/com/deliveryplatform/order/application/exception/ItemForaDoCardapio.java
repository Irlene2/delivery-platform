package com.deliveryplatform.order.application.exception;

import java.util.UUID;

/**
 * Um item do carrinho que o cardápio não vende agora. Recusa de quem pede (400), com a frase
 * que diz o que fazer.
 *
 * <p>{@code nome} é nulo quando o produto não está publicado: aí não há nome confiável para
 * mostrar — o que veio do navegador não é dado do servidor.
 */
public class ItemForaDoCardapio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo { NAO_PUBLICADO, ACABOU }

    private final Motivo motivo;
    private final UUID produtoId;
    private final String nome;

    public ItemForaDoCardapio(Motivo motivo, UUID produtoId, String nome) {
        super(motivo + " " + produtoId);
        this.motivo = motivo;
        this.produtoId = produtoId;
        this.nome = nome;
    }

    public Motivo motivo() {
        return motivo;
    }

    public UUID produtoId() {
        return produtoId;
    }

    public String nome() {
        return nome;
    }
}
