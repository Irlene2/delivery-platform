package com.deliveryplatform.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class ItemDoPedidoJpa {

    @Column(name = "produto_id", nullable = false)
    private UUID produtoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    @Column(length = 140)
    private String observacao;

    protected ItemDoPedidoJpa() {
    }

    public ItemDoPedidoJpa(UUID produtoId, String nome, BigDecimal precoUnitario, int quantidade,
                           String observacao) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
        this.observacao = observacao;
    }

    public UUID getProdutoId() {
        return produtoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getObservacao() {
        return observacao;
    }
}
