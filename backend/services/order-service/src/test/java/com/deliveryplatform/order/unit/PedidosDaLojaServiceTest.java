package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.port.in.FazerPedido.ItemSolicitado;
import com.deliveryplatform.order.application.port.in.FazerPedido.Solicitacao;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.application.port.out.SituacaoDaLojaPort;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.application.usecase.Autorizador;
import com.deliveryplatform.order.application.usecase.ConfiguracaoDePedidosService;
import com.deliveryplatform.order.application.usecase.FazerPedidoService;
import com.deliveryplatform.order.application.usecase.PedidosDaLojaService;
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

class PedidosDaLojaServiceTest {

    static final UUID LOJA = UUID.randomUUID();
    static final UUID PIZZA = UUID.randomUUID();
    // 23h30 em São Paulo do dia 05 = 02h30 UTC do dia 06.
    static final Instant AGORA = Instant.parse("2026-11-06T02:30:00Z");

    final Dubles.Pedidos pedidos = new Dubles.Pedidos();
    final Dubles.Configuracoes configuracoes = new Dubles.Configuracoes();
    final Dubles.Merchant merchant = new Dubles.Merchant();
    final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    final Autorizador autorizador = new Autorizador(merchant, pedidos);

    final Dubles.Entregadores entregadores = new Dubles.Entregadores();
    final PedidosDaLojaService servico = new PedidosDaLojaService(autorizador, pedidos,
            entregadores, relogio, "America/Sao_Paulo");

    Pedido umPedido() {
        var cardapio = new Dubles.Cardapio()
                .com(new ProdutoDoCardapio(PIZZA, "Pizza", Money.de("40"), true));
        return new FazerPedidoService(new Dubles.Lojas(), cardapio, configuracoes, pedidos, relogio).fazer(LOJA,
                new Solicitacao("Maria", "21999990000", Modalidade.RETIRADA, null,
                        List.of(new ItemSolicitado(PIZZA, 1, null)), FormaDePagamento.PIX, null));
    }

    @Test
    @DisplayName("'hoje' é o dia de São Paulo, não o de Greenwich")
    void hoje_no_fuso_da_loja() {
        merchant.permite(LOJA, PermissaoDoPedido.VER_PEDIDO);
        Pedido pedido = umPedido();

        List<Pedido> doDia = servico.daLoja(LOJA, null, null);

        assertThat(pedidos.ultimoDesde).isEqualTo(Instant.parse("2026-11-05T03:00:00Z"));
        assertThat(pedidos.ultimoAte).isEqualTo(Instant.parse("2026-11-06T03:00:00Z"));
        assertThat(doDia.size()).isEqualTo(1);
        assertThat(doDia.get(0).getId()).isEqualTo(pedido.getId());
        assertThat(servico.daLoja(LOJA, null, LocalDate.parse("2026-11-06")).isEmpty()).isTrue();
    }

    @Test
    void sem_vinculo_ou_sem_permissao_e_a_mesma_recusa() {
        assertThatThrownBy(() -> servico.daLoja(LOJA, null, null)).isInstanceOf(AcessoNegado.class);

        merchant.permite(LOJA, PermissaoDoPedido.CONFIGURAR_LOJA);
        assertThatThrownBy(() -> servico.daLoja(LOJA, null, null)).isInstanceOf(AcessoNegado.class);
    }

    @Test
    @DisplayName("pedido de outra loja é 404, o mesmo de não existir (ADR-056)")
    void pedido_de_outra_loja() {
        Pedido pedido = umPedido();
        UUID outra = UUID.randomUUID();
        merchant.permite(outra, PermissaoDoPedido.VER_PEDIDO, PermissaoDoPedido.ALTERAR_STATUS);

        assertThatThrownBy(() -> servico.consultar(outra, pedido.getId()))
                .isInstanceOf(PedidoNaoEncontrado.class);
        assertThatThrownBy(() -> servico.avancar(outra, pedido.getId(), StatusDoPedido.CONFIRMADO, null))
                .isInstanceOf(PedidoNaoEncontrado.class);
    }

    @Test
    @DisplayName("quem só vê pedido não muda status")
    void ver_nao_e_alterar() {
        Pedido pedido = umPedido();
        merchant.permite(LOJA, PermissaoDoPedido.VER_PEDIDO);

        assertThatThrownBy(() -> servico.avancar(LOJA, pedido.getId(), StatusDoPedido.CONFIRMADO, null))
                .isInstanceOf(AcessoNegado.class);
    }

    @Test
    void a_retirada_de_ponta_a_ponta_pelo_servico() {
        Pedido pedido = umPedido();
        merchant.permite(LOJA, PermissaoDoPedido.ALTERAR_STATUS);

        servico.avancar(LOJA, pedido.getId(), StatusDoPedido.CONFIRMADO, null);
        servico.avancar(LOJA, pedido.getId(), StatusDoPedido.EM_PREPARO, null);
        servico.avancar(LOJA, pedido.getId(), StatusDoPedido.PRONTO, null);
        servico.avancar(LOJA, pedido.getId(), StatusDoPedido.AGUARDANDO_CLIENTE, null);
        servico.registrarPagamento(LOJA, pedido.getId(), FormaDePagamento.DINHEIRO, Money.de("50"), false);
        Pedido retirado = servico.avancar(LOJA, pedido.getId(), StatusDoPedido.RETIRADO, null);

        assertThat(retirado.getStatus()).isEqualTo(StatusDoPedido.RETIRADO);
        assertThat(retirado.getPagamentoRegistrado().trocoDevolvido()).isEqualTo(Money.de("10"));
    }

    @Test
    @DisplayName("a configuração pública não pede token; a do painel pede CONFIGURAR_LOJA")
    void configuracao() {
        var lojas = new Dubles.Lojas();
        var config = new ConfiguracaoDePedidosService(autorizador, configuracoes, lojas);

        assertThat(config.daLoja(LOJA).getTaxaDeEntrega()).isEqualTo(Money.ZERO);
        assertThatThrownBy(() -> config.definir(LOJA, Money.de("5"), true, true))
                .isInstanceOf(AcessoNegado.class);

        merchant.permite(LOJA, PermissaoDoPedido.CONFIGURAR_LOJA);
        config.definir(LOJA, Money.de("5"), true, false);

        assertThat(config.daLoja(LOJA).getTaxaDeEntrega()).isEqualTo(Money.de("5"));
        assertThat(config.daLoja(LOJA).aceita(Modalidade.RETIRADA)).isFalse();
    }

    @Test
    @DisplayName("a vitrine diz se a loja está aberta; sem o merchant, abre com 'aberta' nula")
    void vitrine() {
        var lojas = new Dubles.Lojas();
        var config = new ConfiguracaoDePedidosService(autorizador, configuracoes, lojas);
        Instant abre = Instant.parse("2026-11-05T21:00:00Z");

        lojas.situacao = SituacaoDaLojaPort.SituacaoDaLoja.fechada(abre);
        var fechada = config.vitrine(LOJA);
        assertThat(fechada.aberta()).isFalse();
        assertThat(fechada.proximaAbertura()).isEqualTo(abre);
        assertThat(fechada.configuracao().getTaxaDeEntrega()).isEqualTo(Money.ZERO);

        lojas.foraDoAr = true;
        assertThat(config.vitrine(LOJA).aberta()).isNull();
    }
}
