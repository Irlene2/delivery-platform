package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.exception.ItemForaDoCardapio;
import com.deliveryplatform.order.application.exception.LojaFechada;
import com.deliveryplatform.order.application.port.in.FazerPedido;
import com.deliveryplatform.order.application.port.out.CardapioPort;
import com.deliveryplatform.order.application.port.out.SituacaoDaLojaPort;
import com.deliveryplatform.order.application.port.out.ConfiguracaoDePedidosRepositorio;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.model.Cliente;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.domain.model.Endereco;
import com.deliveryplatform.order.domain.model.ItemDoPedido;
import com.deliveryplatform.order.domain.model.PagamentoDeclarado;
import com.deliveryplatform.order.domain.model.Pedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Valida o que vem da tela, lê o cardápio, congela os preços, numera e grava.
 *
 * <h2>A ordem</h2>
 *
 * <p>Primeiro o que é barato e não depende de ninguém (nome, telefone, carrinho vazio) — o
 * domínio cobra ao montar o cliente. Depois o {@code merchant}, que diz se a loja está aberta
 * (ADR-077, emenda de 01/11/2026), e o {@code catalog} — duas idas à rede. Só então
 * o número, que trava a numeração da loja até o fim da transação: pegá-lo antes da ida ao
 * {@code catalog} seria segurar a trava durante uma chamada HTTP.
 *
 * <h2>Item repetido</h2>
 *
 * <p>O mesmo produto duas vezes no carrinho vira dois itens — com observações diferentes, são
 * pedidos diferentes ("uma sem cebola, outra normal").
 */
@Service
public class FazerPedidoService implements FazerPedido {

    private final SituacaoDaLojaPort situacoes;
    private final TravaDoDia trava;
    private final CardapioPort cardapio;
    private final ConfiguracaoDePedidosRepositorio configuracoes;
    private final PedidoRepositorio pedidos;
    private final Clock relogio;

    public FazerPedidoService(SituacaoDaLojaPort situacoes, CardapioPort cardapio,
                              ConfiguracaoDePedidosRepositorio configuracoes,
                              PedidoRepositorio pedidos, Clock relogio, TravaDoDia trava) {
        this.situacoes = situacoes;
        this.trava = trava;
        this.cardapio = cardapio;
        this.configuracoes = configuracoes;
        this.pedidos = pedidos;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public Pedido fazer(UUID estabelecimentoId, Solicitacao s) {
        Objects.requireNonNull(estabelecimentoId, "estabelecimentoId");
        Cliente cliente = Cliente.de(s.nomeDoCliente(), s.telefone());
        Endereco endereco = s.endereco() == null ? null
                : new Endereco(s.endereco().logradouro(), s.endereco().bairro(),
                        s.endereco().complemento());
        PagamentoDeclarado pagamento = new PagamentoDeclarado(s.formaDePagamento(), s.trocoPara());
        if (s.itens() == null || s.itens().isEmpty()) {
            throw new RegraDoPedidoViolada(RegraDoPedidoViolada.Motivo.SEM_ITENS, "pedido sem itens");
        }
        if (s.itens().size() > Pedido.ITENS_MAXIMOS) {
            throw new RegraDoPedidoViolada(RegraDoPedidoViolada.Motivo.ITENS_DEMAIS,
                    s.itens().size() + " itens", String.valueOf(Pedido.ITENS_MAXIMOS));
        }

        SituacaoDaLojaPort.SituacaoDaLoja situacao = situacoes.daLoja(estabelecimentoId);
        if (!situacao.aberta()) {
            throw new LojaFechada(situacao.proximaAbertura());
        }
        // O caixa de hoje já foi fechado: a loja encerrou o dia (ADR-077, emenda de 01/11/2026).
        if (trava.hojeFechado(estabelecimentoId)) {
            throw new LojaFechada(Optional.empty());
        }

        ConfiguracaoDePedidos configuracao = configuracoes.daLoja(estabelecimentoId)
                .orElseGet(() -> ConfiguracaoDePedidos.padrao(estabelecimentoId));

        List<ItemDoPedido> itens = congelar(s.itens(), cardapio.publicadosDe(estabelecimentoId));

        Instant agora = Instant.now(relogio);
        Pedido pedido = Pedido.receber(UUID.randomUUID(), pedidos.proximoNumero(estabelecimentoId),
                UUID.randomUUID(), cliente, s.modalidade(), endereco, itens, configuracao,
                pagamento, agora);

        return pedidos.salvar(pedido);
    }

    /** O nome e o preço do instante do pedido, lidos do servidor — nunca do navegador. */
    private static List<ItemDoPedido> congelar(List<ItemSolicitado> pedidosDaTela,
                                               Map<UUID, ProdutoDoCardapio> publicados) {
        List<ItemDoPedido> itens = new ArrayList<>(pedidosDaTela.size());
        for (ItemSolicitado solicitado : pedidosDaTela) {
            if (solicitado == null || solicitado.produtoId() == null) {
                throw new RegraDoPedidoViolada(RegraDoPedidoViolada.Motivo.TEXTO_OBRIGATORIO,
                        "item sem produto", "produto do item");
            }
            ProdutoDoCardapio produto = publicados.get(solicitado.produtoId());
            if (produto == null) {
                throw new ItemForaDoCardapio(ItemForaDoCardapio.Motivo.NAO_PUBLICADO,
                        solicitado.produtoId(), null);
            }
            if (!produto.vendavel()) {
                throw new ItemForaDoCardapio(ItemForaDoCardapio.Motivo.ACABOU,
                        produto.id(), produto.nome());
            }
            itens.add(new ItemDoPedido(produto.id(), produto.nome(), produto.preco(),
                    solicitado.quantidade(), solicitado.observacao()));
        }
        return itens;
    }
}
