package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.exception.EntregadorForaDaEquipe;
import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.exception.RecusaAoEntregador;
import com.deliveryplatform.order.application.port.in.FazerPedido.EnderecoInformado;
import com.deliveryplatform.order.application.port.in.FazerPedido.ItemSolicitado;
import com.deliveryplatform.order.application.port.in.FazerPedido.Solicitacao;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.application.usecase.Autorizador;
import com.deliveryplatform.order.application.usecase.EntregadoresService;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O entregador da equipe, com a conta dele (decisão de 31/10/2026). */
class EntregadoresServiceTest {

    static final UUID LOJA = UUID.randomUUID();
    static final UUID PIZZA = UUID.randomUUID();
    static final Instant AGORA = Instant.parse("2026-11-05T21:00:00Z");

    final Dubles.Pedidos pedidos = new Dubles.Pedidos();
    final Dubles.Entregadores cadastro = new Dubles.Entregadores();
    final Dubles.Merchant merchant = new Dubles.Merchant();
    final Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);
    final Autorizador autorizador = new Autorizador(merchant, pedidos);
    final Dubles.Fechamentos fechamentos = new Dubles.Fechamentos();
    final EntregadoresService entregador = new EntregadoresService(autorizador, cadastro, pedidos,
            relogio, "America/Sao_Paulo", Dubles.trava(fechamentos, relogio));
    final PedidosDaLojaService loja = new PedidosDaLojaService(autorizador, pedidos, cadastro,
            relogio, "America/Sao_Paulo", Dubles.trava(fechamentos, relogio));

    /** Um pedido de entrega, pronto, de R$ 45,00 (40 + 5 de taxa). */
    Pedido prontoParaSair() {
        var cardapio = new Dubles.Cardapio()
                .com(new ProdutoDoCardapio(PIZZA, "Pizza", Money.de("40"), true));
        var configuracoes = new Dubles.Configuracoes();
        configuracoes.salvar(com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos
                .reconstituir(LOJA, Money.de("5"), true, true));
        Pedido pedido = new FazerPedidoService(new Dubles.Lojas(), cardapio, configuracoes, pedidos, relogio,
                Dubles.trava(fechamentos, relogio)).fazer(LOJA,
                new Solicitacao("Maria", "21999990000", Modalidade.ENTREGA,
                        new EnderecoInformado("Rua A, 1", "Centro", null),
                        List.of(new ItemSolicitado(PIZZA, 1, null)), FormaDePagamento.DINHEIRO, null));
        pedido.avancarPara(StatusDoPedido.CONFIRMADO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.EM_PREPARO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.PRONTO, null, AGORA);
        return pedidos.salvar(pedido);
    }

    @Test
    @DisplayName("o entregador se apresenta, e a loja o escolhe pelo usuarioId — com o nome dele")
    void apresentar_e_ser_escolhido() {
        merchant.permite(LOJA, PermissaoDoPedido.VER_ENTREGA);
        entregador.apresentar(LOJA, "Carlos Moto");
        UUID carlos = merchant.eu;

        merchant.eu = UUID.randomUUID();
        merchant.permite(LOJA, PermissaoDoPedido.VER_PEDIDO, PermissaoDoPedido.ALTERAR_STATUS);
        Pedido pedido = prontoParaSair();
        Pedido comCarlos = loja.informarEntregador(LOJA, pedido.getId(), carlos, "ignorado");

        assertThat(comCarlos.getEntregador()).isEqualTo("Carlos Moto");
        assertThat(comCarlos.estaCom(carlos)).isTrue();
        assertThat(loja.daLoja(LOJA, null, null).size()).isEqualTo(1);
    }

    @Test
    void usuario_que_nunca_se_apresentou_e_recusado() {
        merchant.permite(LOJA, PermissaoDoPedido.ALTERAR_STATUS);
        Pedido pedido = prontoParaSair();

        assertThatThrownBy(() -> loja.informarEntregador(LOJA, pedido.getId(), UUID.randomUUID(), null))
                .isInstanceOf(EntregadorForaDaEquipe.class);
    }

    @Test
    @DisplayName("o entregador vê, sai, recebe em dinheiro e entrega — só as entregas dele")
    void a_entrega_pelo_app() {
        merchant.permite(LOJA, PermissaoDoPedido.VER_ENTREGA, PermissaoDoPedido.ALTERAR_STATUS);
        entregador.apresentar(LOJA, "Carlos");
        Pedido meu = prontoParaSair();
        Pedido deOutro = prontoParaSair();
        loja.informarEntregador(LOJA, meu.getId(), merchant.eu, null);
        loja.informarEntregador(LOJA, deOutro.getId(), null, "Avulso");

        assertThat(entregador.doDia(LOJA, null).size()).isEqualTo(1);

        entregador.avancar(LOJA, meu.getId(), StatusDoPedido.SAIU_PARA_ENTREGA);
        entregador.registrarPagamento(LOJA, meu.getId(), FormaDePagamento.DINHEIRO, Money.de("50"));
        Pedido entregue = entregador.avancar(LOJA, meu.getId(), StatusDoPedido.ENTREGUE);

        assertThat(entregue.getStatus()).isEqualTo(StatusDoPedido.ENTREGUE);
        assertThat(entregue.getPagamentoRegistrado().trocoDevolvido()).isEqualTo(Money.de("5"));
        assertThatThrownBy(() -> entregador.avancar(LOJA, deOutro.getId(), StatusDoPedido.SAIU_PARA_ENTREGA))
                .isInstanceOf(PedidoNaoEncontrado.class);
    }

    @Test
    @DisplayName("confirmar e cancelar são da loja")
    void o_que_nao_e_do_entregador() {
        merchant.permite(LOJA, PermissaoDoPedido.VER_ENTREGA);
        UUID qualquer = UUID.randomUUID();

        assertThatThrownBy(() -> entregador.avancar(LOJA, qualquer, StatusDoPedido.CANCELADO))
                .isInstanceOf(RecusaAoEntregador.class);
    }

    @Test
    @DisplayName("o entregador informa Pix, entrega, e a loja confere no extrato depois (02/11/2026)")
    void pix_informado_pelo_entregador() {
        merchant.permite(LOJA, PermissaoDoPedido.VER_ENTREGA, PermissaoDoPedido.ALTERAR_STATUS);
        entregador.apresentar(LOJA, "Carlos");
        Pedido meu = prontoParaSair();
        loja.informarEntregador(LOJA, meu.getId(), merchant.eu, null);
        entregador.avancar(LOJA, meu.getId(), StatusDoPedido.SAIU_PARA_ENTREGA);

        Pedido informado = entregador.registrarPagamento(LOJA, meu.getId(), FormaDePagamento.PIX, null);
        assertThat(informado.pixAConferir()).isTrue();

        Pedido entregue = entregador.avancar(LOJA, meu.getId(), StatusDoPedido.ENTREGUE);
        assertThat(entregue.getStatus()).isEqualTo(StatusDoPedido.ENTREGUE);

        Pedido conferido = loja.conferirPix(LOJA, meu.getId());
        assertThat(conferido.pixAConferir()).isFalse();
        assertThat(conferido.getPagamentoRegistrado().pixConferido()).isTrue();
    }

    @Test
    void sem_ver_entrega_nao_ha_app_de_entregas() {
        merchant.permite(LOJA, PermissaoDoPedido.VER_PEDIDO);

        assertThatThrownBy(() -> entregador.doDia(LOJA, null)).isInstanceOf(AcessoNegado.class);
        assertThatThrownBy(() -> entregador.apresentar(LOJA, "Eu")).isInstanceOf(AcessoNegado.class);
    }
}
