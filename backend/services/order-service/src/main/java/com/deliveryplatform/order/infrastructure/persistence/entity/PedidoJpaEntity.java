package com.deliveryplatform.order.infrastructure.persistence.entity;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O pedido como linha. Plano de propósito: o agregado é pequeno, e colunas planas deixam as
 * restrições da migration (total, endereço, cancelamento, pagamento inteiro) legíveis no banco.
 *
 * <p>{@code @Version} nulo = pedido novo: o Spring Data faz {@code persist}, e não
 * {@code merge}, sem um {@code SELECT} antes.
 */
@Entity
@Table(name = "pedido")
public class PedidoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "estabelecimento_id", nullable = false)
    private UUID estabelecimentoId;

    @Column(name = "numero", nullable = false)
    private long numero;

    @Column(name = "codigo_de_acompanhamento", nullable = false, unique = true)
    private UUID codigoDeAcompanhamento;

    @Column(name = "cliente_nome", nullable = false, length = 120)
    private String clienteNome;

    @Column(name = "cliente_telefone", nullable = false, length = 16)
    private String clienteTelefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade", nullable = false, length = 16)
    private Modalidade modalidade;

    @Column(name = "endereco_logradouro", length = 200)
    private String enderecoLogradouro;

    @Column(name = "endereco_bairro", length = 80)
    private String enderecoBairro;

    @Column(name = "endereco_complemento", length = 80)
    private String enderecoComplemento;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "taxa_de_entrega", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxaDeEntrega;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_declarada", nullable = false, length = 16)
    private FormaDePagamento formaDeclarada;

    @Column(name = "troco_para", precision = 19, scale = 2)
    private BigDecimal trocoPara;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private StatusDoPedido status;

    @Column(name = "motivo_do_cancelamento", length = 200)
    private String motivoDoCancelamento;

    @Column(name = "entregador", length = 80)
    private String entregador;

    @Column(name = "entregador_id")
    private UUID entregadorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_efetiva", length = 16)
    private FormaDePagamento formaEfetiva;

    @Column(name = "valor_recebido", precision = 19, scale = 2)
    private BigDecimal valorRecebido;

    @Column(name = "troco_devolvido", precision = 19, scale = 2)
    private BigDecimal trocoDevolvido;

    @Column(name = "pix_conferido")
    private Boolean pixConferido;

    @Column(name = "pagamento_registrado_em")
    private Instant pagamentoRegistradoEm;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_do_pedido", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicao")
    private List<ItemDoPedidoJpa> itens = new ArrayList<>();

    @Version
    private Long versao;

    protected PedidoJpaEntity() {
    }

    public PedidoJpaEntity(UUID id,
            UUID estabelecimentoId,
            long numero,
            UUID codigoDeAcompanhamento,
            String clienteNome,
            String clienteTelefone,
            Modalidade modalidade,
            String enderecoLogradouro,
            String enderecoBairro,
            String enderecoComplemento,
            BigDecimal subtotal,
            BigDecimal taxaDeEntrega,
            BigDecimal total,
            FormaDePagamento formaDeclarada,
            BigDecimal trocoPara,
            StatusDoPedido status,
            String motivoDoCancelamento,
            String entregador,
            UUID entregadorId,
            FormaDePagamento formaEfetiva,
            BigDecimal valorRecebido,
            BigDecimal trocoDevolvido,
            Boolean pixConferido,
            Instant pagamentoRegistradoEm,
            Instant criadoEm,
            Instant atualizadoEm,
            List<ItemDoPedidoJpa> itens,
            Long versao) {
        this.id = id;
        this.estabelecimentoId = estabelecimentoId;
        this.numero = numero;
        this.codigoDeAcompanhamento = codigoDeAcompanhamento;
        this.clienteNome = clienteNome;
        this.clienteTelefone = clienteTelefone;
        this.modalidade = modalidade;
        this.enderecoLogradouro = enderecoLogradouro;
        this.enderecoBairro = enderecoBairro;
        this.enderecoComplemento = enderecoComplemento;
        this.subtotal = subtotal;
        this.taxaDeEntrega = taxaDeEntrega;
        this.total = total;
        this.formaDeclarada = formaDeclarada;
        this.trocoPara = trocoPara;
        this.status = status;
        this.motivoDoCancelamento = motivoDoCancelamento;
        this.entregador = entregador;
        this.entregadorId = entregadorId;
        this.formaEfetiva = formaEfetiva;
        this.valorRecebido = valorRecebido;
        this.trocoDevolvido = trocoDevolvido;
        this.pixConferido = pixConferido;
        this.pagamentoRegistradoEm = pagamentoRegistradoEm;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.itens = new ArrayList<>(itens);
        this.versao = versao;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstabelecimentoId() {
        return estabelecimentoId;
    }

    public long getNumero() {
        return numero;
    }

    public UUID getCodigoDeAcompanhamento() {
        return codigoDeAcompanhamento;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteTelefone() {
        return clienteTelefone;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public String getEnderecoLogradouro() {
        return enderecoLogradouro;
    }

    public String getEnderecoBairro() {
        return enderecoBairro;
    }

    public String getEnderecoComplemento() {
        return enderecoComplemento;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxaDeEntrega() {
        return taxaDeEntrega;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public FormaDePagamento getFormaDeclarada() {
        return formaDeclarada;
    }

    public BigDecimal getTrocoPara() {
        return trocoPara;
    }

    public StatusDoPedido getStatus() {
        return status;
    }

    public String getMotivoDoCancelamento() {
        return motivoDoCancelamento;
    }

    public String getEntregador() {
        return entregador;
    }

    public UUID getEntregadorId() {
        return entregadorId;
    }

    public FormaDePagamento getFormaEfetiva() {
        return formaEfetiva;
    }

    public BigDecimal getValorRecebido() {
        return valorRecebido;
    }

    public BigDecimal getTrocoDevolvido() {
        return trocoDevolvido;
    }

    public Boolean getPixConferido() {
        return pixConferido;
    }

    public Instant getPagamentoRegistradoEm() {
        return pagamentoRegistradoEm;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public List<ItemDoPedidoJpa> getItens() {
        return itens;
    }

    public Long getVersao() {
        return versao;
    }

}
