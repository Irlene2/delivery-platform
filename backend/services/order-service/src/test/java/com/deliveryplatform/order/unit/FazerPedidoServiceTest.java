package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.application.exception.ItemForaDoCardapio;
import com.deliveryplatform.order.application.port.in.FazerPedido.EnderecoInformado;
import com.deliveryplatform.order.application.port.in.FazerPedido.ItemSolicitado;
import com.deliveryplatform.order.application.port.in.FazerPedido.Solicitacao;
import com.deliveryplatform.order.application.port.out.CardapioIndisponivel;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.application.usecase.FazerPedidoService;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.Pedido;
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

class FazerPedidoServiceTest {

    static final UUID LOJA = UUID.randomUUID();
    static final UUID PIZZA = UUID.randomUUID();
    static final UUID SUCO = UUID.randomUUID();
    static final Instant AGORA = Instant.parse("2026-11-05T21:00:00Z");

    final Dubles.Pedidos pedidos = new Dubles.Pedidos();
    final Dubles.Configuracoes configuracoes = new Dubles.Configuracoes();
    final Dubles.Cardapio cardapio = new Dubles.Cardapio()
            .com(new ProdutoDoCardapio(PIZZA, "Pizza margherita", Money.de("49.90"), true))
            .com(new ProdutoDoCardapio(SUCO, "Suco de laranja", Money.de("9.00"), false));

    final FazerPedidoService servico = new FazerPedidoService(cardapio, configuracoes, pedidos,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    static Solicitacao entrega(UUID produto, int quantidade) {
        return new Solicitacao("Maria", "21999990000", Modalidade.ENTREGA,
                new EnderecoInformado("Rua A, 10", "Centro", null),
                List.of(new ItemSolicitado(produto, quantidade, "sem cebola")),
                FormaDePagamento.CARTAO, null);
    }

    @Test
    @DisplayName("o preço e o nome vêm do cardápio, e a taxa vem da configuração da loja")
    void preco_do_servidor_e_taxa_da_loja() {
        configuracoes.salvar(ConfiguracaoDePedidos.reconstituir(LOJA, Money.de("6.50"), true, true));

        Pedido pedido = servico.fazer(LOJA, entrega(PIZZA, 2));

        assertThat(pedido.getItens().get(0).nome()).isEqualTo("Pizza margherita");
        assertThat(pedido.getItens().get(0).precoUnitario()).isEqualTo(Money.de("49.90"));
        assertThat(pedido.getTotal()).isEqualTo(Money.de("106.30"));
        assertThat(pedido.getCriadoEm()).isEqualTo(AGORA);
        assertThat(pedidos.gravados.containsKey(pedido.getId())).isTrue();
    }

    @Test
    @DisplayName("loja que nunca configurou: taxa zero")
    void sem_configuracao_e_taxa_zero() {
        Pedido pedido = servico.fazer(LOJA, entrega(PIZZA, 1));

        assertThat(pedido.getTaxaDeEntrega()).isEqualTo(Money.ZERO);
    }

    @Test
    void a_numeracao_e_por_loja_e_sobe_de_um_em_um() {
        Pedido primeiro = servico.fazer(LOJA, entrega(PIZZA, 1));
        Pedido segundo = servico.fazer(LOJA, entrega(PIZZA, 1));
        Pedido deOutraLoja = servico.fazer(UUID.randomUUID(), entrega(PIZZA, 1));

        assertThat(primeiro.numeroLegivel()).isEqualTo("000001");
        assertThat(segundo.numeroLegivel()).isEqualTo("000002");
        assertThat(deOutraLoja.numeroLegivel()).isEqualTo("000001");
        assertThat(primeiro.getCodigoDeAcompanhamento().equals(segundo.getCodigoDeAcompanhamento()))
                .isFalse();
    }

    @Test
    void produto_fora_do_cardapio_e_recusado_e_nada_e_gravado() {
        assertThatThrownBy(() -> servico.fazer(LOJA, entrega(UUID.randomUUID(), 1)))
                .isInstanceOf(ItemForaDoCardapio.class);
        assertThat(pedidos.gravados.isEmpty()).isTrue();
    }

    @Test
    void produto_que_acabou_e_recusado_com_o_nome() {
        assertThatThrownBy(() -> servico.fazer(LOJA, entrega(SUCO, 1)))
                .isInstanceOf(ItemForaDoCardapio.class)
                .hasMessageContaining("ACABOU");
    }

    @Test
    @DisplayName("catálogo fora do ar: o pedido não nasce, e nenhum número é gasto")
    void catalogo_fora_do_ar() {
        cardapio.foraDoAr = true;

        assertThatThrownBy(() -> servico.fazer(LOJA, entrega(PIZZA, 1)))
                .isInstanceOf(CardapioIndisponivel.class);
        assertThat(pedidos.gravados.isEmpty()).isTrue();
        assertThat(pedidos.numeracao.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("telefone inválido é recusado antes de perguntar ao catálogo")
    void validacao_barata_antes_da_rede() {
        cardapio.foraDoAr = true;
        Solicitacao ruim = new Solicitacao("Maria", "123", Modalidade.RETIRADA, null,
                List.of(new ItemSolicitado(PIZZA, 1, null)), FormaDePagamento.PIX, null);

        assertThatThrownBy(() -> servico.fazer(LOJA, ruim))
                .isInstanceOf(RegraDoPedidoViolada.class);
    }

    @Test
    void carrinho_vazio_e_recusado() {
        Solicitacao vazia = new Solicitacao("Maria", "21999990000", Modalidade.RETIRADA, null,
                List.of(), FormaDePagamento.PIX, null);

        assertThatThrownBy(() -> servico.fazer(LOJA, vazia))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("sem itens");
    }
}
