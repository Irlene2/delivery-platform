package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.FechamentoRecusado;
import com.deliveryplatform.valuetypes.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * O caixa do dia, fechado e guardado (ADR-077, emenda de 01/11/2026). Até esta emenda o
 * fechamento era um relatório calculado na tela; agora é apurado <b>aqui, a partir dos pedidos
 * do dia</b> — nunca de totais que o navegador mande —, e gravado uma vez por loja e dia.
 *
 * <h2>As regras</h2>
 *
 * <ul>
 *   <li><b>Não se fecha com pedido em andamento.</b> Todo pedido do dia está entregue, retirado
 *       ou cancelado; senão a recusa diz quantos faltam.</li>
 *   <li><b>O repasse ao entregador é a taxa de cada entrega feita</b> (decisão de 31/10/2026,
 *       PRD §10 emendado), e o acerto é {@code dinheiro recebido − repasse}: positivo, ele
 *       devolve à loja; negativo, a loja paga a ele.</li>
 *   <li><b>A conferência é opcional:</b> quem fecha pode informar o dinheiro contado no caixa, e
 *       a diferença para o dinheiro das vendas fica registrada — sem bloquear.</li>
 *   <li>Depois de fechado, os pedidos daquele dia não mudam mais (a trava é do caso de uso).</li>
 * </ul>
 */
public final class FechamentoDoDia {

    private final UUID id;
    private final UUID estabelecimentoId;
    private final LocalDate dia;
    private final Instant fechadoEm;
    private final UUID fechadoPor;
    private final int pedidos;
    private final int concluidos;
    private final int cancelados;
    private final Money vendas;
    private final Money taxas;
    private final Money dinheiro;
    private final Money cartao;
    private final Money pix;
    private final Money dinheiroContado;
    private final String observacao;
    private final List<AcertoDoEntregador> acertos;

    /** O que cada entregador levou, recebeu e acerta no fim do dia. */
    public record AcertoDoEntregador(UUID entregadorId, String nome, int entregas, Money repasse,
                                     Money dinheiro, Money cartao) {
        public AcertoDoEntregador {
            Objects.requireNonNull(nome, "nome");
            Objects.requireNonNull(repasse, "repasse");
            Objects.requireNonNull(dinheiro, "dinheiro");
            Objects.requireNonNull(cartao, "cartao");
        }

        /** Positivo: o entregador devolve à loja. Negativo: a loja paga a ele. */
        public Money acerto() {
            return dinheiro.menos(repasse);
        }
    }

    private FechamentoDoDia(UUID id, UUID estabelecimentoId, LocalDate dia, Instant fechadoEm,
                            UUID fechadoPor, int pedidos, int concluidos, int cancelados,
                            Money vendas, Money taxas, Money dinheiro, Money cartao, Money pix,
                            Money dinheiroContado, String observacao,
                            List<AcertoDoEntregador> acertos) {
        this.id = Objects.requireNonNull(id, "id");
        this.estabelecimentoId = Objects.requireNonNull(estabelecimentoId, "estabelecimentoId");
        this.dia = Objects.requireNonNull(dia, "dia");
        this.fechadoEm = Objects.requireNonNull(fechadoEm, "fechadoEm");
        this.fechadoPor = Objects.requireNonNull(fechadoPor, "fechadoPor");
        this.pedidos = pedidos;
        this.concluidos = concluidos;
        this.cancelados = cancelados;
        this.vendas = Objects.requireNonNull(vendas, "vendas");
        this.taxas = Objects.requireNonNull(taxas, "taxas");
        this.dinheiro = Objects.requireNonNull(dinheiro, "dinheiro");
        this.cartao = Objects.requireNonNull(cartao, "cartao");
        this.pix = Objects.requireNonNull(pix, "pix");
        if (dinheiroContado != null && dinheiroContado.ehNegativo()) {
            throw new FechamentoRecusado(FechamentoRecusado.Motivo.CONTAGEM_NEGATIVA, 0);
        }
        this.dinheiroContado = dinheiroContado;
        this.observacao = observacao == null || observacao.isBlank() ? null : observacao.trim();
        this.acertos = List.copyOf(acertos);
    }

    /**
     * Apura o dia a partir dos pedidos dele — todos os criados naquele dia, no fuso da loja.
     *
     * @throws FechamentoRecusado com {@code EM_ANDAMENTO} se algum pedido não terminou
     */
    public static FechamentoDoDia apurar(UUID id, UUID estabelecimentoId, LocalDate dia,
                                         List<Pedido> pedidosDoDia, UUID fechadoPor,
                                         Money dinheiroContado, String observacao, Instant agora) {
        long emAndamento = pedidosDoDia.stream().filter(p -> !p.getStatus().encerrado()).count();
        if (emAndamento > 0) {
            throw new FechamentoRecusado(FechamentoRecusado.Motivo.EM_ANDAMENTO, (int) emAndamento);
        }

        Money vendas = Money.ZERO;
        Money taxas = Money.ZERO;
        Map<FormaDePagamento, Money> porForma = new LinkedHashMap<>();
        for (FormaDePagamento forma : FormaDePagamento.values()) {
            porForma.put(forma, Money.ZERO);
        }
        Map<String, AcertoDoEntregador> porEntregador = new LinkedHashMap<>();
        int concluidos = 0;
        int cancelados = 0;

        for (Pedido pedido : pedidosDoDia) {
            if (pedido.getStatus() == StatusDoPedido.CANCELADO) {
                cancelados++;
                continue;
            }
            concluidos++;
            vendas = vendas.mais(pedido.getTotal());
            taxas = taxas.mais(pedido.getTaxaDeEntrega());
            FormaDePagamento forma = pedido.getPagamentoRegistrado() != null
                    ? pedido.getPagamentoRegistrado().formaEfetiva()
                    : pedido.getPagamentoDeclarado().forma();
            porForma.merge(forma, pedido.getTotal(), Money::mais);

            if (pedido.getStatus() == StatusDoPedido.ENTREGUE && pedido.getEntregador() != null) {
                String chave = pedido.getEntregadorId() != null
                        ? pedido.getEntregadorId().toString()
                        : "nome:" + pedido.getEntregador();
                AcertoDoEntregador atual = porEntregador.getOrDefault(chave,
                        new AcertoDoEntregador(pedido.getEntregadorId(), pedido.getEntregador(), 0,
                                Money.ZERO, Money.ZERO, Money.ZERO));
                porEntregador.put(chave, new AcertoDoEntregador(atual.entregadorId(), atual.nome(),
                        atual.entregas() + 1,
                        atual.repasse().mais(pedido.getTaxaDeEntrega()),
                        forma == FormaDePagamento.DINHEIRO
                                ? atual.dinheiro().mais(pedido.getTotal()) : atual.dinheiro(),
                        forma == FormaDePagamento.CARTAO
                                ? atual.cartao().mais(pedido.getTotal()) : atual.cartao()));
            }
        }

        List<AcertoDoEntregador> acertos = new ArrayList<>(porEntregador.values());
        acertos.sort(Comparator.comparing(AcertoDoEntregador::nome, String.CASE_INSENSITIVE_ORDER));

        return new FechamentoDoDia(id, estabelecimentoId, dia, agora, fechadoPor,
                pedidosDoDia.size(), concluidos, cancelados, vendas, taxas,
                porForma.get(FormaDePagamento.DINHEIRO), porForma.get(FormaDePagamento.CARTAO),
                porForma.get(FormaDePagamento.PIX), dinheiroContado, observacao, acertos);
    }

    /** Remonta um fechamento gravado. Sem reapurar: o que foi fechado é o que vale. */
    public static FechamentoDoDia reconstituir(UUID id, UUID estabelecimentoId, LocalDate dia,
                                               Instant fechadoEm, UUID fechadoPor, int pedidos,
                                               int concluidos, int cancelados, Money vendas,
                                               Money taxas, Money dinheiro, Money cartao,
                                               Money pix, Money dinheiroContado, String observacao,
                                               List<AcertoDoEntregador> acertos) {
        return new FechamentoDoDia(id, estabelecimentoId, dia, fechadoEm, fechadoPor, pedidos,
                concluidos, cancelados, vendas, taxas, dinheiro, cartao, pix, dinheiroContado,
                observacao, acertos);
    }

    /** O total que os entregadores recebem no dia: a soma das taxas das entregas feitas. */
    public Money repasseTotal() {
        return acertos.stream().map(AcertoDoEntregador::repasse).reduce(Money.ZERO, Money::mais);
    }

    /** Contado menos as vendas em dinheiro; nulo quando ninguém contou. */
    public Money diferencaDeCaixa() {
        return dinheiroContado == null ? null : dinheiroContado.menos(dinheiro);
    }

    public UUID getId() { return id; }
    public UUID getEstabelecimentoId() { return estabelecimentoId; }
    public LocalDate getDia() { return dia; }
    public Instant getFechadoEm() { return fechadoEm; }
    public UUID getFechadoPor() { return fechadoPor; }
    public int getPedidos() { return pedidos; }
    public int getConcluidos() { return concluidos; }
    public int getCancelados() { return cancelados; }
    public Money getVendas() { return vendas; }
    public Money getTaxas() { return taxas; }
    public Money getDinheiro() { return dinheiro; }
    public Money getCartao() { return cartao; }
    public Money getPix() { return pix; }
    public Money getDinheiroContado() { return dinheiroContado; }
    public String getObservacao() { return observacao; }
    public List<AcertoDoEntregador> getAcertos() { return acertos; }
}
