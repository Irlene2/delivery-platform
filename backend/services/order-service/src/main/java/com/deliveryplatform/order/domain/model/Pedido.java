package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada.Motivo;
import com.deliveryplatform.order.domain.exception.TransicaoInvalida;
import com.deliveryplatform.valuetypes.Money;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.deliveryplatform.order.domain.model.StatusDoPedido.AGUARDANDO_CLIENTE;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.CANCELADO;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.CONFIRMADO;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.EM_PREPARO;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.ENTREGUE;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.PRONTO;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.RECEBIDO;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.RETIRADO;
import static com.deliveryplatform.order.domain.model.StatusDoPedido.SAIU_PARA_ENTREGA;

/**
 * O pedido — o agregado do {@code order} na entrega de 15/11
 * ({@code docs/entrega-15-11/pedido.md}).
 *
 * <h2>O que ele garante</h2>
 * <ul>
 *   <li>os itens e os valores são <b>congelados</b> ao nascer: nome e preço vêm do
 *       catálogo naquele instante, e o total é calculado aqui, nunca recebido;</li>
 *   <li>a entrega tem endereço e a taxa da loja; a retirada não tem endereço, e a taxa é
 *       <b>zero</b>;</li>
 *   <li>o troco só existe em dinheiro, e a nota não é menor que o total;</li>
 *   <li>o status só anda pelas setas da tabela; {@code SAIU_PARA_ENTREGA} pede entregador,
 *       e {@code ENTREGUE} e {@code RETIRADO} pedem <b>pagamento registrado</b> (R2 do
 *       planejamento: nenhuma entrega é concluída sem liquidação registrada);</li>
 *   <li>cancelar pede motivo, e só vale antes de ficar pronto.</li>
 * </ul>
 */
public final class Pedido {

    public static final int ITENS_MAXIMOS = 50;
    public static final int MAXIMO_DO_MOTIVO = 200;
    public static final int MAXIMO_DO_ENTREGADOR = 80;

    /** As setas da tabela do pedido.md §4 — as condições extras estão em {@link #avancarPara}. */
    private static final Map<StatusDoPedido, Set<StatusDoPedido>> SETAS = new EnumMap<>(StatusDoPedido.class);

    static {
        SETAS.put(RECEBIDO, EnumSet.of(CONFIRMADO, CANCELADO));
        SETAS.put(CONFIRMADO, EnumSet.of(EM_PREPARO, CANCELADO));
        SETAS.put(EM_PREPARO, EnumSet.of(PRONTO, CANCELADO));
        SETAS.put(PRONTO, EnumSet.of(SAIU_PARA_ENTREGA, AGUARDANDO_CLIENTE));
        SETAS.put(SAIU_PARA_ENTREGA, EnumSet.of(ENTREGUE));
        SETAS.put(AGUARDANDO_CLIENTE, EnumSet.of(RETIRADO));
        SETAS.put(ENTREGUE, EnumSet.noneOf(StatusDoPedido.class));
        SETAS.put(RETIRADO, EnumSet.noneOf(StatusDoPedido.class));
        SETAS.put(CANCELADO, EnumSet.noneOf(StatusDoPedido.class));
    }

    private final UUID id;
    private final UUID estabelecimentoId;
    private final long numero;
    private final UUID codigoDeAcompanhamento;
    private final Cliente cliente;
    private final Modalidade modalidade;
    private final Endereco endereco;
    private final List<ItemDoPedido> itens;
    private final Money subtotal;
    private final Money taxaDeEntrega;
    private final Money total;
    private final PagamentoDeclarado pagamentoDeclarado;
    private final Instant criadoEm;

    private StatusDoPedido status;
    private String motivoDoCancelamento;
    private String entregador;
    /** Quem tem conta: o entregador da equipe que vê o pedido no app dele. Nulo = só o nome. */
    private UUID entregadorId;
    private PagamentoRegistrado pagamentoRegistrado;
    private Instant atualizadoEm;
    private final Long versao;

    private Pedido(UUID id, UUID estabelecimentoId, long numero, UUID codigoDeAcompanhamento,
                   Cliente cliente, Modalidade modalidade, Endereco endereco,
                   List<ItemDoPedido> itens, Money subtotal, Money taxaDeEntrega, Money total,
                   PagamentoDeclarado pagamentoDeclarado, Instant criadoEm, StatusDoPedido status,
                   String motivoDoCancelamento, String entregador, UUID entregadorId,
                   PagamentoRegistrado pagamentoRegistrado, Instant atualizadoEm, Long versao) {
        this.id = Objects.requireNonNull(id, "id");
        this.estabelecimentoId = Objects.requireNonNull(estabelecimentoId, "estabelecimentoId");
        this.numero = numero;
        this.codigoDeAcompanhamento = Objects.requireNonNull(codigoDeAcompanhamento, "codigo");
        this.cliente = Objects.requireNonNull(cliente, "cliente");
        this.modalidade = Objects.requireNonNull(modalidade, "modalidade");
        this.endereco = endereco;
        this.itens = List.copyOf(itens);
        this.subtotal = subtotal;
        this.taxaDeEntrega = taxaDeEntrega;
        this.total = total;
        this.pagamentoDeclarado = Objects.requireNonNull(pagamentoDeclarado, "pagamento");
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm");
        this.status = Objects.requireNonNull(status, "status");
        this.motivoDoCancelamento = motivoDoCancelamento;
        this.entregador = entregador;
        this.entregadorId = entregadorId;
        this.pagamentoRegistrado = pagamentoRegistrado;
        this.atualizadoEm = atualizadoEm;
        this.versao = versao;
    }

    /**
     * O pedido nasce {@code RECEBIDO} — o cliente confirmou; a loja ainda não se
     * comprometeu (pedido.md §2, T01).
     *
     * @param itens já resolvidos no catálogo: nome e preço do instante do pedido
     */
    public static Pedido receber(UUID id, long numero, UUID codigoDeAcompanhamento,
                                 Cliente cliente, Modalidade modalidade, Endereco endereco,
                                 List<ItemDoPedido> itens, ConfiguracaoDePedidos configuracao,
                                 PagamentoDeclarado pagamento, Instant agora) {
        Objects.requireNonNull(configuracao, "configuracao");
        Objects.requireNonNull(modalidade, "modalidade");
        Objects.requireNonNull(pagamento, "pagamento");
        if (itens == null || itens.isEmpty()) {
            throw new RegraDoPedidoViolada(Motivo.SEM_ITENS, "pedido sem itens");
        }
        if (itens.size() > ITENS_MAXIMOS) {
            throw new RegraDoPedidoViolada(Motivo.ITENS_DEMAIS,
                    itens.size() + " itens", String.valueOf(ITENS_MAXIMOS));
        }
        if (!configuracao.aceita(modalidade)) {
            throw new RegraDoPedidoViolada(Motivo.MODALIDADE_NAO_ACEITA,
                    "a loja não aceita " + modalidade, modalidade.name());
        }
        if (modalidade == Modalidade.ENTREGA && endereco == null) {
            throw new RegraDoPedidoViolada(Motivo.ENDERECO_OBRIGATORIO, "entrega sem endereço");
        }
        if (modalidade == Modalidade.RETIRADA && endereco != null) {
            throw new RegraDoPedidoViolada(Motivo.ENDERECO_NA_RETIRADA, "retirada com endereço");
        }

        Money subtotal = itens.stream().map(ItemDoPedido::subtotal).reduce(Money.ZERO, Money::mais);
        Money taxa = configuracao.taxaPara(modalidade);
        Money total = subtotal.mais(taxa);

        if (pagamento.trocoPara() != null) {
            if (pagamento.forma() != FormaDePagamento.DINHEIRO) {
                throw new RegraDoPedidoViolada(Motivo.TROCO_SEM_DINHEIRO,
                        "troco em " + pagamento.forma(), pagamento.forma().name());
            }
            if (pagamento.trocoPara().menorQue(total)) {
                throw new RegraDoPedidoViolada(Motivo.TROCO_MENOR_QUE_O_TOTAL,
                        "troco para " + pagamento.trocoPara() + " num total de " + total,
                        texto(pagamento.trocoPara()), texto(total));
            }
        }

        return new Pedido(id, configuracao.getEstabelecimentoId(), numero, codigoDeAcompanhamento,
                cliente, modalidade, endereco, itens, subtotal, taxa, total, pagamento, agora,
                RECEBIDO, null, null, null, null, agora, null);
    }

    /** Para o repositório: o pedido como foi gravado, sem refazer as regras de nascimento. */
    public static Pedido reconstituir(UUID id, UUID estabelecimentoId, long numero,
                                      UUID codigoDeAcompanhamento, Cliente cliente,
                                      Modalidade modalidade, Endereco endereco,
                                      List<ItemDoPedido> itens, Money subtotal,
                                      Money taxaDeEntrega, Money total,
                                      PagamentoDeclarado pagamentoDeclarado, Instant criadoEm,
                                      StatusDoPedido status, String motivoDoCancelamento,
                                      String entregador, UUID entregadorId,
                                      PagamentoRegistrado pagamentoRegistrado,
                                      Instant atualizadoEm, Long versao) {
        return new Pedido(id, estabelecimentoId, numero, codigoDeAcompanhamento, cliente,
                modalidade, endereco, itens, subtotal, taxaDeEntrega, total, pagamentoDeclarado,
                criadoEm, status, motivoDoCancelamento, entregador, entregadorId,
                pagamentoRegistrado, atualizadoEm, versao);
    }

    /**
     * Muda o status pela tabela. {@code motivo} só é lido no cancelamento, onde é
     * obrigatório.
     */
    public void avancarPara(StatusDoPedido destino, String motivo, Instant agora) {
        Objects.requireNonNull(destino, "destino");
        if (!SETAS.get(status).contains(destino)) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.NAO_PERMITIDA, status, destino);
        }
        if (destino == SAIU_PARA_ENTREGA && modalidade != Modalidade.ENTREGA
                || destino == AGUARDANDO_CLIENTE && modalidade != Modalidade.RETIRADA) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.NAO_PERMITIDA, status, destino);
        }
        if (destino == SAIU_PARA_ENTREGA && entregador == null) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.FALTA_ENTREGADOR, status, destino);
        }
        if ((destino == ENTREGUE || destino == RETIRADO) && pagamentoRegistrado == null) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.FALTA_PAGAMENTO, status, destino);
        }
        if (destino == CANCELADO) {
            String aparado = motivo == null ? "" : motivo.strip();
            if (aparado.isEmpty()) {
                throw new RegraDoPedidoViolada(Motivo.MOTIVO_OBRIGATORIO, "cancelamento sem motivo");
            }
            this.motivoDoCancelamento = Textos.obrigatorio(aparado, "motivo do cancelamento",
                    MAXIMO_DO_MOTIVO);
        }
        this.status = destino;
        this.atualizadoEm = Objects.requireNonNull(agora, "agora");
    }

    /** O entregador sem conta — só o nome (um motoboy avulso, por exemplo). */
    public void informarEntregador(String nome, Instant agora) {
        informarEntregador(null, nome, agora);
    }

    /**
     * O entregador que leva o pedido. Com {@code usuarioId}, é alguém da equipe com conta, e o
     * pedido aparece no app dele; sem, é só o nome.
     */
    public void informarEntregador(UUID usuarioId, String nome, Instant agora) {
        if (modalidade != Modalidade.ENTREGA) {
            throw new RegraDoPedidoViolada(Motivo.ENTREGADOR_NA_RETIRADA, "entregador numa retirada");
        }
        if (status.encerrado()) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.PEDIDO_ENCERRADO, status, null);
        }
        this.entregador = Textos.obrigatorio(nome, "nome do entregador", MAXIMO_DO_ENTREGADOR);
        this.entregadorId = usuarioId;
        this.atualizadoEm = Objects.requireNonNull(agora, "agora");
    }

    /**
     * Registra como o pedido foi pago de verdade, do pronto até antes de encerrar. Pode
     * ser registrado de novo para corrigir, enquanto o pedido não encerrou.
     *
     * @param valorRecebido obrigatório em dinheiro, e não menor que o total
     * @param pixConferido obrigatório e verdadeiro no Pix
     */
    public void registrarPagamento(FormaDePagamento forma, Money valorRecebido,
                                   boolean pixConferido, Instant agora) {
        Objects.requireNonNull(forma, "forma");
        if (!status.aceitaPagamento()) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.PAGAMENTO_FORA_DE_HORA, status, null);
        }
        Money recebido = null;
        Money troco = null;
        switch (forma) {
            case DINHEIRO -> {
                if (valorRecebido == null) {
                    throw new RegraDoPedidoViolada(Motivo.VALOR_RECEBIDO_OBRIGATORIO,
                            "dinheiro sem valor recebido");
                }
                if (valorRecebido.menorQue(total)) {
                    throw new RegraDoPedidoViolada(Motivo.VALOR_RECEBIDO_MENOR_QUE_O_TOTAL,
                            "recebido " + valorRecebido + " num total de " + total,
                            texto(valorRecebido), texto(total));
                }
                recebido = valorRecebido;
                troco = valorRecebido.menos(total);
            }
            case PIX -> {
                if (!pixConferido) {
                    throw new RegraDoPedidoViolada(Motivo.PIX_NAO_CONFERIDO, "pix sem conferência");
                }
            }
            case CARTAO -> {
                // Só a forma: bandeira e NSU são o marco 4.
            }
        }
        this.pagamentoRegistrado = new PagamentoRegistrado(forma, recebido, troco,
                forma == FormaDePagamento.PIX, Objects.requireNonNull(agora, "agora"));
        this.atualizadoEm = agora;
    }

    /**
     * O entregador informa que o cliente pagou por Pix (02/11/2026). Fica registrado como Pix
     * <b>a conferir</b>: o dinheiro cai na conta da loja, e só quem vê o extrato confirma. O
     * pedido pode ser concluído — a entrega aconteceu —, mas o caixa do dia não fecha enquanto
     * houver Pix a conferir ({@link FechamentoDoDia}).
     */
    public void registrarPixInformado(Instant agora) {
        if (!status.aceitaPagamento()) {
            throw new TransicaoInvalida(TransicaoInvalida.Motivo.PAGAMENTO_FORA_DE_HORA, status, null);
        }
        this.pagamentoRegistrado = new PagamentoRegistrado(FormaDePagamento.PIX, null, null, false,
                Objects.requireNonNull(agora, "agora"));
        this.atualizadoEm = agora;
    }

    /**
     * A loja confirma, no extrato, o Pix que o entregador informou. Vale em qualquer estado —
     * inclusive depois de entregue, que é quando a conferência costuma acontecer.
     */
    public void conferirPix(Instant agora) {
        if (pagamentoRegistrado == null
                || pagamentoRegistrado.formaEfetiva() != FormaDePagamento.PIX
                || pagamentoRegistrado.pixConferido()) {
            throw new RegraDoPedidoViolada(Motivo.NADA_A_CONFERIR, "sem pix a conferir");
        }
        this.pagamentoRegistrado = new PagamentoRegistrado(FormaDePagamento.PIX, null, null, true,
                pagamentoRegistrado.registradoEm());
        this.atualizadoEm = Objects.requireNonNull(agora, "agora");
    }

    /** O Pix informado pelo entregador que a loja ainda não conferiu. */
    public boolean pixAConferir() {
        return pagamentoRegistrado != null
                && pagamentoRegistrado.formaEfetiva() == FormaDePagamento.PIX
                && !pagamentoRegistrado.pixConferido();
    }

    /** O número como o cliente e a loja falam: seis dígitos, com zeros à esquerda. */
    public String numeroLegivel() {
        return String.format("%06d", numero);
    }

    private static String texto(Money valor) {
        return valor.valor().toPlainString();
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

    public Cliente getCliente() {
        return cliente;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    /** Nulo na retirada. */
    public Endereco getEndereco() {
        return endereco;
    }

    public List<ItemDoPedido> getItens() {
        return itens;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getTaxaDeEntrega() {
        return taxaDeEntrega;
    }

    public Money getTotal() {
        return total;
    }

    public PagamentoDeclarado getPagamentoDeclarado() {
        return pagamentoDeclarado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
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

    /** Se este pedido está com este entregador da equipe. */
    public boolean estaCom(UUID usuarioId) {
        return usuarioId != null && usuarioId.equals(entregadorId);
    }

    public PagamentoRegistrado getPagamentoRegistrado() {
        return pagamentoRegistrado;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /** A versão do bloqueio otimista; nula antes da primeira gravação. */
    public Long getVersao() {
        return versao;
    }
}
