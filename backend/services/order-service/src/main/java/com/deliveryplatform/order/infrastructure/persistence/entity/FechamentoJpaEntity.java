package com.deliveryplatform.order.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** O fechamento do dia como a tabela o guarda (V3). Sem regra: a regra é do agregado. */
@Entity
@Table(name = "fechamento_do_dia")
public class FechamentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "estabelecimento_id", nullable = false)
    private UUID estabelecimentoId;

    @Column(nullable = false)
    private LocalDate dia;

    @Column(name = "fechado_em", nullable = false)
    private Instant fechadoEm;

    @Column(name = "fechado_por", nullable = false)
    private UUID fechadoPor;

    @Column(nullable = false)
    private int pedidos;

    @Column(nullable = false)
    private int concluidos;

    @Column(nullable = false)
    private int cancelados;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal vendas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal taxas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dinheiro;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cartao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pix;

    @Column(name = "dinheiro_contado", precision = 19, scale = 2)
    private BigDecimal dinheiroContado;

    @Column(length = 500)
    private String observacao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "acerto_do_fechamento", joinColumns = @JoinColumn(name = "fechamento_id"))
    @OrderColumn(name = "ordem")
    private List<Acerto> acertos = new ArrayList<>();

    protected FechamentoJpaEntity() {
    }

    @SuppressWarnings("java:S107")
    public FechamentoJpaEntity(UUID id, UUID estabelecimentoId, LocalDate dia, Instant fechadoEm,
                               UUID fechadoPor, int pedidos, int concluidos, int cancelados,
                               BigDecimal vendas, BigDecimal taxas, BigDecimal dinheiro,
                               BigDecimal cartao, BigDecimal pix, BigDecimal dinheiroContado,
                               String observacao, List<Acerto> acertos) {
        this.id = id;
        this.estabelecimentoId = estabelecimentoId;
        this.dia = dia;
        this.fechadoEm = fechadoEm;
        this.fechadoPor = fechadoPor;
        this.pedidos = pedidos;
        this.concluidos = concluidos;
        this.cancelados = cancelados;
        this.vendas = vendas;
        this.taxas = taxas;
        this.dinheiro = dinheiro;
        this.cartao = cartao;
        this.pix = pix;
        this.dinheiroContado = dinheiroContado;
        this.observacao = observacao;
        this.acertos = new ArrayList<>(acertos);
    }

    public UUID getId() { return id; }
    public UUID getEstabelecimentoId() { return estabelecimentoId; }
    public LocalDate getDia() { return dia; }
    public Instant getFechadoEm() { return fechadoEm; }
    public UUID getFechadoPor() { return fechadoPor; }
    public int getPedidos() { return pedidos; }
    public int getConcluidos() { return concluidos; }
    public int getCancelados() { return cancelados; }
    public BigDecimal getVendas() { return vendas; }
    public BigDecimal getTaxas() { return taxas; }
    public BigDecimal getDinheiro() { return dinheiro; }
    public BigDecimal getCartao() { return cartao; }
    public BigDecimal getPix() { return pix; }
    public BigDecimal getDinheiroContado() { return dinheiroContado; }
    public String getObservacao() { return observacao; }
    public List<Acerto> getAcertos() { return acertos; }

    /** Uma linha do acerto. Classe, e não record: o JPA a instancia vazia. */
    @Embeddable
    public static class Acerto {

        @Column(name = "entregador_id")
        private UUID entregadorId;

        @Column(nullable = false, length = 80)
        private String nome;

        @Column(nullable = false)
        private int entregas;

        @Column(nullable = false, precision = 19, scale = 2)
        private BigDecimal repasse;

        @Column(nullable = false, precision = 19, scale = 2)
        private BigDecimal dinheiro;

        @Column(nullable = false, precision = 19, scale = 2)
        private BigDecimal cartao;

        protected Acerto() {
        }

        public Acerto(UUID entregadorId, String nome, int entregas, BigDecimal repasse,
                      BigDecimal dinheiro, BigDecimal cartao) {
            this.entregadorId = entregadorId;
            this.nome = nome;
            this.entregas = entregas;
            this.repasse = repasse;
            this.dinheiro = dinheiro;
            this.cartao = cartao;
        }

        public UUID getEntregadorId() { return entregadorId; }
        public String getNome() { return nome; }
        public int getEntregas() { return entregas; }
        public BigDecimal getRepasse() { return repasse; }
        public BigDecimal getDinheiro() { return dinheiro; }
        public BigDecimal getCartao() { return cartao; }
    }
}
