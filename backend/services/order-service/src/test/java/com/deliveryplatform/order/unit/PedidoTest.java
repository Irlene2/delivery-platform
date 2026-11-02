package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.TransicaoInvalida;
import com.deliveryplatform.order.domain.model.Cliente;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.domain.model.Endereco;
import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.ItemDoPedido;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.PagamentoDeclarado;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.valuetypes.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O pedido da entrega de 15/11 ({@code docs/entrega-15-11/pedido.md}): nascimento,
 * valores congelados, a tabela de estados e o pagamento registrado.
 */
class PedidoTest {

    static final UUID LOJA = UUID.fromString("3f6c1a2e-0000-4000-8000-000000000001");
    static final Instant AGORA = Instant.parse("2026-11-05T21:00:00Z");
    static final Endereco ENDERECO = new Endereco("Rua A, 10", "Centro", "ap 2");

    static ConfiguracaoDePedidos lojaComTaxa(String taxa) {
        return ConfiguracaoDePedidos.reconstituir(LOJA, Money.de(taxa), true, true);
    }

    static ItemDoPedido item(String nome, String preco, int quantidade) {
        return new ItemDoPedido(UUID.randomUUID(), nome, Money.de(preco), quantidade, null);
    }

    static Pedido entrega(PagamentoDeclarado pagamento) {
        return Pedido.receber(UUID.randomUUID(), 123, UUID.randomUUID(),
                Cliente.de("Maria Souza", "(21) 99999-0000"), Modalidade.ENTREGA, ENDERECO,
                List.of(item("Pizza margherita", "49.90", 2)), lojaComTaxa("5.00"), pagamento, AGORA);
    }

    static Pedido entrega() {
        return entrega(new PagamentoDeclarado(FormaDePagamento.CARTAO, null));
    }

    static Pedido retirada() {
        return Pedido.receber(UUID.randomUUID(), 124, UUID.randomUUID(),
                Cliente.de("João", "21988887777"), Modalidade.RETIRADA, null,
                List.of(item("Refrigerante lata", "7.00", 1)), lojaComTaxa("5.00"),
                new PagamentoDeclarado(FormaDePagamento.PIX, null), AGORA);
    }

    // ---------------- nascimento ----------------

    @Test
    @DisplayName("nasce RECEBIDO, e o total é calculado aqui: itens mais a taxa da loja")
    void nasce_recebido_com_o_total_calculado() {
        Pedido pedido = entrega();

        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.RECEBIDO);
        assertThat(pedido.getSubtotal()).isEqualTo(Money.de("99.80"));
        assertThat(pedido.getTaxaDeEntrega()).isEqualTo(Money.de("5.00"));
        assertThat(pedido.getTotal()).isEqualTo(Money.de("104.80"));
        assertThat(pedido.numeroLegivel()).isEqualTo("000123");
        assertThat(pedido.getCliente().telefone().numero()).isEqualTo("+5521999990000");
    }

    @Test
    @DisplayName("na retirada a taxa é zero, e não nula")
    void retirada_tem_taxa_zero() {
        Pedido pedido = retirada();

        assertThat(pedido.getTaxaDeEntrega()).isEqualTo(Money.ZERO);
        assertThat(pedido.getTotal()).isEqualTo(Money.de("7.00"));
        assertThat(pedido.getEndereco()).isNull();
    }

    @Test
    void entrega_sem_endereco_e_recusada() {
        assertThatThrownBy(() -> Pedido.receber(UUID.randomUUID(), 1, UUID.randomUUID(),
                Cliente.de("Maria", "21999990000"), Modalidade.ENTREGA, null,
                List.of(item("Pizza", "10", 1)), lojaComTaxa("5"),
                new PagamentoDeclarado(FormaDePagamento.CARTAO, null), AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("sem endereço");
    }

    @Test
    void retirada_com_endereco_e_recusada() {
        assertThatThrownBy(() -> Pedido.receber(UUID.randomUUID(), 1, UUID.randomUUID(),
                Cliente.de("Maria", "21999990000"), Modalidade.RETIRADA, ENDERECO,
                List.of(item("Pizza", "10", 1)), lojaComTaxa("5"),
                new PagamentoDeclarado(FormaDePagamento.CARTAO, null), AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("retirada com endereço");
    }

    @Test
    void modalidade_que_a_loja_nao_aceita_e_recusada() {
        ConfiguracaoDePedidos soRetirada =
                ConfiguracaoDePedidos.reconstituir(LOJA, Money.ZERO, false, true);

        assertThatThrownBy(() -> Pedido.receber(UUID.randomUUID(), 1, UUID.randomUUID(),
                Cliente.de("Maria", "21999990000"), Modalidade.ENTREGA, ENDERECO,
                List.of(item("Pizza", "10", 1)), soRetirada,
                new PagamentoDeclarado(FormaDePagamento.CARTAO, null), AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("não aceita");
    }

    @Test
    void sem_itens_e_recusado() {
        assertThatThrownBy(() -> Pedido.receber(UUID.randomUUID(), 1, UUID.randomUUID(),
                Cliente.de("Maria", "21999990000"), Modalidade.RETIRADA, null, List.of(),
                lojaComTaxa("0"), new PagamentoDeclarado(FormaDePagamento.CARTAO, null), AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("sem itens");
    }

    @Test
    void quantidade_fora_de_um_a_noventa_e_nove_e_recusada() {
        assertThatThrownBy(() -> item("Pizza", "10", 0)).isInstanceOf(RegraDoPedidoViolada.class);
        assertThatThrownBy(() -> item("Pizza", "10", 100)).isInstanceOf(RegraDoPedidoViolada.class);
    }

    @Test
    @DisplayName("o troco só existe em dinheiro, e a nota não pode ser menor que o total")
    void regras_do_troco() {
        assertThatThrownBy(() -> entrega(new PagamentoDeclarado(FormaDePagamento.PIX, Money.de("200"))))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("troco em PIX");
        assertThatThrownBy(() -> entrega(new PagamentoDeclarado(FormaDePagamento.DINHEIRO, Money.de("100"))))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("troco para");

        Pedido exato = entrega(new PagamentoDeclarado(FormaDePagamento.DINHEIRO, Money.de("104.80")));
        assertThat(exato.getPagamentoDeclarado().trocoPara()).isEqualTo(Money.de("104.80"));
    }

    @Test
    void nome_do_cliente_em_branco_e_recusado() {
        assertThatThrownBy(() -> Cliente.de("  ", "21999990000"))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("nome do cliente");
    }

    @Test
    void telefone_invalido_e_recusado() {
        assertThatThrownBy(() -> Cliente.de("Maria", "123"))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("telefone");
    }

    // ---------------- estados ----------------

    @Test
    @DisplayName("a entrega de ponta a ponta: entregador antes de sair, pagamento antes de entregar")
    void caminho_da_entrega() {
        Pedido pedido = entrega();
        pedido.avancarPara(StatusDoPedido.CONFIRMADO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.EM_PREPARO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.PRONTO, null, AGORA);

        assertThatThrownBy(() -> pedido.avancarPara(StatusDoPedido.SAIU_PARA_ENTREGA, null, AGORA))
                .isInstanceOf(TransicaoInvalida.class)
                .hasMessageContaining("FALTA_ENTREGADOR");

        pedido.informarEntregador("Carlos", AGORA);
        pedido.avancarPara(StatusDoPedido.SAIU_PARA_ENTREGA, null, AGORA);

        assertThatThrownBy(() -> pedido.avancarPara(StatusDoPedido.ENTREGUE, null, AGORA))
                .isInstanceOf(TransicaoInvalida.class)
                .hasMessageContaining("FALTA_PAGAMENTO");

        pedido.registrarPagamento(FormaDePagamento.CARTAO, null, false, AGORA);
        pedido.avancarPara(StatusDoPedido.ENTREGUE, null, AGORA);

        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.ENTREGUE);
        assertThat(pedido.getStatus().encerrado()).isTrue();
    }

    @Test
    void caminho_da_retirada() {
        Pedido pedido = retirada();
        pedido.avancarPara(StatusDoPedido.CONFIRMADO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.EM_PREPARO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.PRONTO, null, AGORA);

        assertThatThrownBy(() -> pedido.avancarPara(StatusDoPedido.SAIU_PARA_ENTREGA, null, AGORA))
                .isInstanceOf(TransicaoInvalida.class);

        pedido.avancarPara(StatusDoPedido.AGUARDANDO_CLIENTE, null, AGORA);
        pedido.registrarPagamento(FormaDePagamento.PIX, null, true, AGORA);
        pedido.avancarPara(StatusDoPedido.RETIRADO, null, AGORA);

        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.RETIRADO);
    }

    @Test
    void pular_etapa_e_conflito() {
        Pedido pedido = entrega();

        assertThatThrownBy(() -> pedido.avancarPara(StatusDoPedido.PRONTO, null, AGORA))
                .isInstanceOf(TransicaoInvalida.class)
                .hasMessageContaining("RECEBIDO → PRONTO");
        assertThat(pedido.getStatus()).isEqualTo(StatusDoPedido.RECEBIDO);
    }

    @Test
    void cancelar_pede_motivo_e_so_vale_antes_de_ficar_pronto() {
        Pedido semMotivo = entrega();
        assertThatThrownBy(() -> semMotivo.avancarPara(StatusDoPedido.CANCELADO, "  ", AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class);

        Pedido cancelado = entrega();
        cancelado.avancarPara(StatusDoPedido.CANCELADO, "cliente desistiu", AGORA);
        assertThat(cancelado.getMotivoDoCancelamento()).isEqualTo("cliente desistiu");

        Pedido pronto = entrega();
        pronto.avancarPara(StatusDoPedido.CONFIRMADO, null, AGORA);
        pronto.avancarPara(StatusDoPedido.EM_PREPARO, null, AGORA);
        pronto.avancarPara(StatusDoPedido.PRONTO, null, AGORA);
        assertThatThrownBy(() -> pronto.avancarPara(StatusDoPedido.CANCELADO, "tarde demais", AGORA))
                .isInstanceOf(TransicaoInvalida.class);
    }

    @Test
    void entregador_numa_retirada_e_recusado() {
        assertThatThrownBy(() -> retirada().informarEntregador("Carlos", AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class);
    }

    // ---------------- pagamento registrado ----------------

    @Test
    void pagamento_antes_de_ficar_pronto_e_conflito() {
        assertThatThrownBy(() -> entrega().registrarPagamento(FormaDePagamento.CARTAO, null, false, AGORA))
                .isInstanceOf(TransicaoInvalida.class)
                .hasMessageContaining("PAGAMENTO_FORA_DE_HORA");
    }

    @Test
    @DisplayName("em dinheiro, o troco é calculado aqui e a nota não pode ser menor que o total")
    void dinheiro_calcula_o_troco() {
        Pedido pedido = pronto(entrega());

        assertThatThrownBy(() -> pedido.registrarPagamento(FormaDePagamento.DINHEIRO, null, false, AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class);
        assertThatThrownBy(() -> pedido.registrarPagamento(FormaDePagamento.DINHEIRO, Money.de("100"), false, AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class);

        pedido.registrarPagamento(FormaDePagamento.DINHEIRO, Money.de("110"), false, AGORA);
        assertThat(pedido.getPagamentoRegistrado().trocoDevolvido()).isEqualTo(Money.de("5.20"));
        assertThat(pedido.getPagamentoRegistrado().valorRecebido()).isEqualTo(Money.de("110"));
    }

    @Test
    @DisplayName("o Pix só vale conferido no extrato — comprovante na tela não é pagamento")
    void pix_precisa_de_conferencia() {
        Pedido pedido = pronto(entrega());

        assertThatThrownBy(() -> pedido.registrarPagamento(FormaDePagamento.PIX, null, false, AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("pix");

        pedido.registrarPagamento(FormaDePagamento.PIX, null, true, AGORA);
        assertThat(pedido.getPagamentoRegistrado().pixConferido()).isTrue();
    }

    @Test
    @DisplayName("o Pix informado fica a conferir; conferir só vale para Pix pendente")
    void pix_informado_e_conferido() {
        Pedido pedido = pronto(entrega());

        pedido.registrarPixInformado(AGORA);
        assertThat(pedido.pixAConferir()).isTrue();
        assertThat(pedido.getPagamentoRegistrado().formaEfetiva()).isEqualTo(FormaDePagamento.PIX);

        pedido.conferirPix(AGORA);
        assertThat(pedido.pixAConferir()).isFalse();
        assertThatThrownBy(() -> pedido.conferirPix(AGORA))
                .isInstanceOf(RegraDoPedidoViolada.class)
                .hasMessageContaining("sem pix a conferir");
    }

    @Test
    void a_forma_efetiva_pode_ser_outra_que_a_declarada() {
        Pedido pedido = pronto(entrega(new PagamentoDeclarado(FormaDePagamento.DINHEIRO, Money.de("110"))));

        pedido.registrarPagamento(FormaDePagamento.CARTAO, null, false, AGORA);

        assertThat(pedido.getPagamentoDeclarado().forma()).isEqualTo(FormaDePagamento.DINHEIRO);
        assertThat(pedido.getPagamentoRegistrado().formaEfetiva()).isEqualTo(FormaDePagamento.CARTAO);
    }

    // ---------------- configuração ----------------

    @Test
    void configuracao_recusa_taxa_negativa_e_loja_sem_modalidade() {
        assertThatThrownBy(() -> ConfiguracaoDePedidos.reconstituir(LOJA, Money.de("-1"), true, true))
                .isInstanceOf(RegraDoPedidoViolada.class);
        assertThatThrownBy(() -> ConfiguracaoDePedidos.reconstituir(LOJA, Money.ZERO, false, false))
                .isInstanceOf(RegraDoPedidoViolada.class);

        ConfiguracaoDePedidos padrao = ConfiguracaoDePedidos.padrao(LOJA);
        assertThat(padrao.getTaxaDeEntrega()).isEqualTo(Money.ZERO);
        assertThat(padrao.aceita(Modalidade.ENTREGA)).isTrue();
        assertThat(padrao.aceita(Modalidade.RETIRADA)).isTrue();
    }

    private static Pedido pronto(Pedido pedido) {
        pedido.avancarPara(StatusDoPedido.CONFIRMADO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.EM_PREPARO, null, AGORA);
        pedido.avancarPara(StatusDoPedido.PRONTO, null, AGORA);
        return pedido;
    }
}
