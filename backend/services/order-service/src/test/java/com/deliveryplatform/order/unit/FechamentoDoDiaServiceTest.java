package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.exception.LojaFechada;
import com.deliveryplatform.order.application.port.in.FazerPedido.EnderecoInformado;
import com.deliveryplatform.order.application.port.in.FazerPedido.ItemSolicitado;
import com.deliveryplatform.order.application.port.in.FazerPedido.Solicitacao;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.application.usecase.Autorizador;
import com.deliveryplatform.order.application.usecase.FazerPedidoService;
import com.deliveryplatform.order.application.usecase.FechamentoDoDiaService;
import com.deliveryplatform.order.application.usecase.PedidosDaLojaService;
import com.deliveryplatform.order.domain.exception.FechamentoRecusado;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.domain.model.FechamentoDoDia;
import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.valuetypes.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O caixa do dia, fechado e guardado (ADR-077, emenda de 01/11/2026). */
class FechamentoDoDiaServiceTest {

    static final UUID LOJA = UUID.randomUUID();
    static final UUID PIZZA = UUID.randomUUID();
    // 21h em São Paulo do dia 05/11/2026.
    static final Instant AGORA = Instant.parse("2026-11-06T00:00:00Z");
    static final LocalDate HOJE = LocalDate.parse("2026-11-05");

    final Dubles.Pedidos pedidos = new Dubles.Pedidos();
    final Dubles.Configuracoes configuracoes = new Dubles.Configuracoes();
    final Dubles.Merchant merchant = new Dubles.Merchant();
    final Dubles.Entregadores entregadores = new Dubles.Entregadores();
    final Dubles.Fechamentos fechamentos = new Dubles.Fechamentos();
    final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    final Autorizador autorizador = new Autorizador(merchant, pedidos);
    final PedidosDaLojaService loja = new PedidosDaLojaService(autorizador, pedidos, entregadores,
            relogio, "America/Sao_Paulo", Dubles.trava(fechamentos, relogio));
    final FazerPedidoService vitrine;
    final FechamentoDoDiaService caixa = new FechamentoDoDiaService(autorizador, pedidos,
            fechamentos, Dubles.trava(fechamentos, relogio), relogio);

    FechamentoDoDiaServiceTest() {
        configuracoes.salvar(ConfiguracaoDePedidos.reconstituir(LOJA, Money.de("5"), true, true));
        var cardapio = new Dubles.Cardapio()
                .com(new ProdutoDoCardapio(PIZZA, "Pizza", Money.de("40"), true));
        vitrine = new FazerPedidoService(new Dubles.Lojas(), cardapio, configuracoes, pedidos,
                relogio, Dubles.trava(fechamentos, relogio));
        merchant.permite(LOJA, PermissaoDoPedido.VER_PEDIDO, PermissaoDoPedido.ALTERAR_STATUS,
                PermissaoDoPedido.VER_VENDAS, PermissaoDoPedido.GERENCIAR_JORNADA);
    }

    /** Uma entrega de R$ 45,00 (40 + 5 de taxa), levada pelo entregador e paga como dado. */
    Pedido entregue(String entregador, FormaDePagamento forma) {
        Pedido p = vitrine.fazer(LOJA, new Solicitacao("Maria", "21999990000", Modalidade.ENTREGA,
                new EnderecoInformado("Rua A, 1", "Centro", null),
                List.of(new ItemSolicitado(PIZZA, 1, null)), forma, null));
        UUID id = p.getId();
        loja.avancar(LOJA, id, StatusDoPedido.CONFIRMADO, null);
        loja.avancar(LOJA, id, StatusDoPedido.EM_PREPARO, null);
        loja.avancar(LOJA, id, StatusDoPedido.PRONTO, null);
        loja.informarEntregador(LOJA, id, null, entregador);
        loja.avancar(LOJA, id, StatusDoPedido.SAIU_PARA_ENTREGA, null);
        loja.registrarPagamento(LOJA, id, forma,
                forma == FormaDePagamento.DINHEIRO ? Money.de("50") : null,
                forma == FormaDePagamento.PIX);
        return loja.avancar(LOJA, id, StatusDoPedido.ENTREGUE, null);
    }

    @Test
    @DisplayName("apura no servidor: vendas por forma, e o repasse de cada entregador é a taxa de cada entrega")
    void apura_o_dia() {
        entregue("Carlos", FormaDePagamento.DINHEIRO);
        entregue("Carlos", FormaDePagamento.CARTAO);
        entregue("Joana", FormaDePagamento.PIX);

        FechamentoDoDia f = caixa.fechar(LOJA, null, Money.de("40"), "  conferido  ");

        assertThat(f.getDia()).isEqualTo(HOJE);
        assertThat(f.getConcluidos()).isEqualTo(3);
        assertThat(f.getVendas()).isEqualTo(Money.de("135"));
        assertThat(f.getDinheiro()).isEqualTo(Money.de("45"));
        assertThat(f.getCartao()).isEqualTo(Money.de("45"));
        assertThat(f.getPix()).isEqualTo(Money.de("45"));
        assertThat(f.getAcertos().size()).isEqualTo(2);
        FechamentoDoDia.AcertoDoEntregador carlos = f.getAcertos().get(0);
        assertThat(carlos.nome()).isEqualTo("Carlos");
        assertThat(carlos.entregas()).isEqualTo(2);
        assertThat(carlos.repasse()).isEqualTo(Money.de("10"));
        assertThat(carlos.acerto()).isEqualTo(Money.de("35"));
        assertThat(f.repasseTotal()).isEqualTo(Money.de("15"));
        assertThat(f.diferencaDeCaixa()).isEqualTo(Money.de("-5"));
        assertThat(f.getObservacao()).isEqualTo("conferido");
        assertThat(f.getFechadoPor()).isEqualTo(merchant.eu);
    }

    @Test
    @DisplayName("com pedido em andamento não fecha, e a recusa diz quantos")
    void nao_fecha_com_pedido_em_andamento() {
        entregue("Carlos", FormaDePagamento.CARTAO);
        vitrine.fazer(LOJA, new Solicitacao("João", "21988887777", Modalidade.RETIRADA, null,
                List.of(new ItemSolicitado(PIZZA, 1, null)), FormaDePagamento.PIX, null));

        assertThatThrownBy(() -> caixa.fechar(LOJA, null, null, null))
                .isInstanceOfSatisfying(FechamentoRecusado.class, e -> {
                    assertThat(e.motivo()).isEqualTo(FechamentoRecusado.Motivo.EM_ANDAMENTO);
                    assertThat(e.quantos()).isEqualTo(1);
                });
        assertThat(fechamentos.gravados.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("fechado, o dia não muda: nem pedido antigo, nem pedido novo")
    void depois_de_fechado() {
        Pedido cancelavel = vitrine.fazer(LOJA, new Solicitacao("João", "21988887777",
                Modalidade.RETIRADA, null, List.of(new ItemSolicitado(PIZZA, 1, null)),
                FormaDePagamento.PIX, null));
        loja.avancar(LOJA, cancelavel.getId(), StatusDoPedido.CANCELADO, "desistiu");
        caixa.fechar(LOJA, null, null, null);

        assertThatThrownBy(() -> caixa.fechar(LOJA, HOJE, null, null))
                .isInstanceOfSatisfying(FechamentoRecusado.class,
                        e -> assertThat(e.motivo()).isEqualTo(FechamentoRecusado.Motivo.JA_FECHADO));
        assertThatThrownBy(() -> vitrine.fazer(LOJA, new Solicitacao("Ana", "21977776666",
                Modalidade.RETIRADA, null, List.of(new ItemSolicitado(PIZZA, 1, null)),
                FormaDePagamento.PIX, null)))
                .isInstanceOf(LojaFechada.class);
        assertThat(caixa.recentes(LOJA).size()).isEqualTo(1);
        assertThat(caixa.doDia(LOJA, HOJE).getCancelados()).isEqualTo(1);
    }

    @Test
    @DisplayName("fechar exige GERENCIAR_JORNADA; amanhã não se fecha")
    void permissao_e_dia() {
        assertThatThrownBy(() -> caixa.fechar(LOJA, HOJE.plusDays(1), null, null))
                .isInstanceOf(FechamentoRecusado.class);

        merchant.permite(LOJA, PermissaoDoPedido.VER_PEDIDO, PermissaoDoPedido.VER_VENDAS);
        assertThatThrownBy(() -> caixa.fechar(LOJA, null, null, null))
                .isInstanceOf(AcessoNegado.class);
    }
}
