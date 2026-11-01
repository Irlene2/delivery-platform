package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.exception.EntregadorForaDaEquipe;
import com.deliveryplatform.order.application.port.in.ConsultarPedido;
import com.deliveryplatform.order.application.port.in.ListarPedidos;
import com.deliveryplatform.order.application.port.in.OperarPedido;
import com.deliveryplatform.order.application.port.out.EntregadorRepositorio;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * O painel de pedidos: a lista do dia, um pedido, e os três atos da loja sobre ele.
 *
 * <h2>"Hoje" é no fuso da loja</h2>
 *
 * <p>O {@code merchant} guarda o fuso de cada loja, mas o {@code order} não pergunta por ele
 * nesta entrega: todas as lojas da demonstração estão no mesmo fuso, configurado em
 * {@code delivery.pedidos.fuso}. Gatilho: a primeira loja fora dele.
 *
 * <h2>Teto da lista</h2>
 *
 * <p>Um dia de uma loja pequena cabe folgado em 500 pedidos; o teto existe para que a lista
 * nunca carregue a tabela inteira (o {@code CLAUDE.md} manda paginar), e é alto o bastante para
 * não precisar de página na tela.
 */
@Service
public class PedidosDaLojaService implements ListarPedidos, ConsultarPedido, OperarPedido {

    static final int TETO_DA_LISTA = 500;

    private final Autorizador autorizador;
    private final PedidoRepositorio pedidos;
    private final EntregadorRepositorio entregadores;
    private final Clock relogio;
    private final ZoneId fuso;
    private final TravaDoDia trava;

    public PedidosDaLojaService(Autorizador autorizador, PedidoRepositorio pedidos,
                                EntregadorRepositorio entregadores, Clock relogio,
                                @Value("${delivery.pedidos.fuso:America/Sao_Paulo}") String fuso,
                                TravaDoDia trava) {
        this.autorizador = autorizador;
        this.pedidos = pedidos;
        this.entregadores = entregadores;
        this.relogio = relogio;
        this.fuso = ZoneId.of(fuso);
        this.trava = trava;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> daLoja(UUID estabelecimentoId, StatusDoPedido status, LocalDate dia) {
        autorizador.exigir(estabelecimentoId, PermissaoDoPedido.VER_PEDIDO);
        LocalDate alvo = dia != null ? dia : LocalDate.now(relogio.withZone(fuso));
        Instant desde = alvo.atStartOfDay(fuso).toInstant();
        Instant ate = alvo.plusDays(1).atStartOfDay(fuso).toInstant();
        return pedidos.daLoja(estabelecimentoId, status, desde, ate, TETO_DA_LISTA);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido consultar(UUID estabelecimentoId, UUID pedidoId) {
        return autorizador.pedidoDaLoja(estabelecimentoId, pedidoId, PermissaoDoPedido.VER_PEDIDO);
    }

    @Override
    @Transactional
    public Pedido avancar(UUID estabelecimentoId, UUID pedidoId, StatusDoPedido destino,
                          String motivo) {
        return operar(estabelecimentoId, pedidoId,
                p -> p.avancarPara(destino, motivo, Instant.now(relogio)));
    }

    @Override
    @Transactional
    public Pedido informarEntregador(UUID estabelecimentoId, UUID pedidoId, UUID usuarioId,
                                     String nome) {
        Pedido pedido = autorizador.pedidoDaLoja(
                estabelecimentoId, pedidoId, PermissaoDoPedido.ALTERAR_STATUS);
        trava.exigirAberto(pedido);
        if (usuarioId == null) {
            pedido.informarEntregador(nome, Instant.now(relogio));
        } else {
            // O nome vem do cadastro, e não do corpo: é o nome com que ele se apresentou.
            var entregador = entregadores.buscar(estabelecimentoId, usuarioId)
                    .orElseThrow(EntregadorForaDaEquipe::new);
            pedido.informarEntregador(usuarioId, entregador.nome(), Instant.now(relogio));
        }
        return pedidos.salvar(pedido);
    }

    @Override
    @Transactional
    public Pedido registrarPagamento(UUID estabelecimentoId, UUID pedidoId, FormaDePagamento forma,
                                     Money valorRecebido, boolean pixConferido) {
        return operar(estabelecimentoId, pedidoId,
                p -> p.registrarPagamento(forma, valorRecebido, pixConferido, Instant.now(relogio)));
    }

    private Pedido operar(UUID estabelecimentoId, UUID pedidoId, Consumer<Pedido> ato) {
        Pedido pedido = autorizador.pedidoDaLoja(
                estabelecimentoId, pedidoId, PermissaoDoPedido.ALTERAR_STATUS);
        trava.exigirAberto(pedido);
        ato.accept(pedido);
        return pedidos.salvar(pedido);
    }
}
