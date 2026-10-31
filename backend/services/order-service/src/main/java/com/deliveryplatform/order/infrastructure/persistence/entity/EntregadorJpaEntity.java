package com.deliveryplatform.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "entregador")
public class EntregadorJpaEntity {

    @EmbeddedId
    private Chave chave;

    @Column(nullable = false, length = 80)
    private String nome;

    protected EntregadorJpaEntity() {
    }

    public EntregadorJpaEntity(UUID estabelecimentoId, UUID usuarioId, String nome) {
        this.chave = new Chave(estabelecimentoId, usuarioId);
        this.nome = nome;
    }

    public UUID getEstabelecimentoId() {
        return chave.estabelecimentoId;
    }

    public UUID getUsuarioId() {
        return chave.usuarioId;
    }

    public String getNome() {
        return nome;
    }

    /** A chave composta (loja, usuário). Classe, e não record: o JPA a instancia vazia. */
    @Embeddable
    public static class Chave implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "estabelecimento_id")
        private UUID estabelecimentoId;

        @Column(name = "usuario_id")
        private UUID usuarioId;

        protected Chave() {
        }

        public Chave(UUID estabelecimentoId, UUID usuarioId) {
            this.estabelecimentoId = estabelecimentoId;
            this.usuarioId = usuarioId;
        }

        @Override
        public boolean equals(Object outro) {
            return outro instanceof Chave c
                    && java.util.Objects.equals(estabelecimentoId, c.estabelecimentoId)
                    && java.util.Objects.equals(usuarioId, c.usuarioId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(estabelecimentoId, usuarioId);
        }
    }
}
