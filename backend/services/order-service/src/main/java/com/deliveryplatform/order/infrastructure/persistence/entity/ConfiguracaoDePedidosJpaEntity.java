package com.deliveryplatform.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma linha por loja, com a loja como chave. Sem versão: quem altera é o administrador, numa
 * tela de três campos, e o último a gravar vence.
 */
@Entity
@Table(name = "configuracao_de_pedidos")
public class ConfiguracaoDePedidosJpaEntity {

    @Id
    @Column(name = "estabelecimento_id")
    private UUID estabelecimentoId;

    @Column(name = "taxa_de_entrega", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxaDeEntrega;

    @Column(name = "aceita_entrega", nullable = false)
    private boolean aceitaEntrega;

    @Column(name = "aceita_retirada", nullable = false)
    private boolean aceitaRetirada;

    protected ConfiguracaoDePedidosJpaEntity() {
    }

    public ConfiguracaoDePedidosJpaEntity(UUID estabelecimentoId, BigDecimal taxaDeEntrega,
                                          boolean aceitaEntrega, boolean aceitaRetirada) {
        this.estabelecimentoId = estabelecimentoId;
        this.taxaDeEntrega = taxaDeEntrega;
        this.aceitaEntrega = aceitaEntrega;
        this.aceitaRetirada = aceitaRetirada;
    }

    public UUID getEstabelecimentoId() {
        return estabelecimentoId;
    }

    public BigDecimal getTaxaDeEntrega() {
        return taxaDeEntrega;
    }

    public boolean isAceitaEntrega() {
        return aceitaEntrega;
    }

    public boolean isAceitaRetirada() {
        return aceitaRetirada;
    }
}
