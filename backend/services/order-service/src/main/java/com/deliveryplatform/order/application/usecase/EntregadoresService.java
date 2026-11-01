package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.exception.RecusaAoEntregador;
import com.deliveryplatform.order.application.port.in.EntregadoresDaLoja;
import com.deliveryplatform.order.application.port.in.MinhasEntregas;
import com.deliveryplatform.order.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.order.application.port.out.EntregadorRepositorio;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.domain.model.EntregadorDaLoja;
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
import java.util.Optional;
import java.util.UUID;

/**
 * O entregador da equipe (decisão de 31/10/2026): ele entra com a conta dele, convidado com
 * {@code VER_ENTREGA}, e vê só as entregas que a loja passou para ele.
 *
 * <h2>Quem é "eu"</h2>
 *
 * <p>O {@code usuarioId} sai do contexto que o {@code merchant} montou a partir do token
 * (ADR-045) — nunca do corpo nem da URL. Por isso não há rota "entregas do entregador X": há
 * "as minhas".
 *
 * <h2>O que é dele, e o que é da loja</h2>
 *
 * <p>Ele marca a saída e a entrega, e registra o que recebeu em dinheiro ou cartão. Confirmar,
 * preparar e cancelar são da loja; o Pix também, porque cai na conta da loja e só ela vê o
 * extrato. O repasse do dia é a soma das taxas das entregas dele (decisão de 31/10/2026) e é
 * calculado na tela do fechamento — não há dinheiro guardado aqui.
 */
@Service
public class EntregadoresService implements EntregadoresDaLoja, MinhasEntregas {

    static final int TETO_DO_DIA = 200;

    private final Autorizador autorizador;
    private final EntregadorRepositorio entregadores;
    private final PedidoRepositorio pedidos;
    private final Clock relogio;
    private final ZoneId fuso;
    private final TravaDoDia trava;

    public EntregadoresService(Autorizador autorizador, EntregadorRepositorio entregadores,
                               PedidoRepositorio pedidos, Clock relogio,
                               @Value("${delivery.pedidos.fuso:America/Sao_Paulo}") String fuso,
                               TravaDoDia trava) {
        this.autorizador = autorizador;
        this.entregadores = entregadores;
        this.pedidos = pedidos;
        this.relogio = relogio;
        this.fuso = ZoneId.of(fuso);
        this.trava = trava;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregadorDaLoja> listar(UUID estabelecimentoId) {
        autorizador.exigir(estabelecimentoId, PermissaoDoPedido.VER_PEDIDO);
        return entregadores.daLoja(estabelecimentoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntregadorDaLoja> eu(UUID estabelecimentoId) {
        ContextoDeAcesso eu = autorizador.contextoCom(estabelecimentoId, PermissaoDoPedido.VER_ENTREGA);
        return entregadores.buscar(estabelecimentoId, eu.usuarioId());
    }

    @Override
    @Transactional
    public EntregadorDaLoja apresentar(UUID estabelecimentoId, String nome) {
        ContextoDeAcesso eu = autorizador.contextoCom(estabelecimentoId, PermissaoDoPedido.VER_ENTREGA);
        return entregadores.salvar(new EntregadorDaLoja(estabelecimentoId, eu.usuarioId(), nome));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> doDia(UUID estabelecimentoId, LocalDate dia) {
        ContextoDeAcesso eu = autorizador.contextoCom(estabelecimentoId, PermissaoDoPedido.VER_ENTREGA);
        LocalDate alvo = dia != null ? dia : LocalDate.now(relogio.withZone(fuso));
        Instant desde = alvo.atStartOfDay(fuso).toInstant();
        Instant ate = alvo.plusDays(1).atStartOfDay(fuso).toInstant();
        return pedidos.doEntregador(estabelecimentoId, eu.usuarioId(), desde, ate, TETO_DO_DIA);
    }

    @Override
    @Transactional
    public Pedido avancar(UUID estabelecimentoId, UUID pedidoId, StatusDoPedido destino) {
        if (destino != StatusDoPedido.SAIU_PARA_ENTREGA && destino != StatusDoPedido.ENTREGUE) {
            throw new RecusaAoEntregador(RecusaAoEntregador.Motivo.STATUS);
        }
        Pedido pedido = minha(estabelecimentoId, pedidoId);
        trava.exigirAberto(pedido);
        pedido.avancarPara(destino, null, Instant.now(relogio));
        return pedidos.salvar(pedido);
    }

    @Override
    @Transactional
    public Pedido registrarPagamento(UUID estabelecimentoId, UUID pedidoId, FormaDePagamento forma,
                                     Money valorRecebido) {
        if (forma == FormaDePagamento.PIX) {
            throw new RecusaAoEntregador(RecusaAoEntregador.Motivo.PIX);
        }
        Pedido pedido = minha(estabelecimentoId, pedidoId);
        trava.exigirAberto(pedido);
        pedido.registrarPagamento(forma, valorRecebido, false, Instant.now(relogio));
        return pedidos.salvar(pedido);
    }

    /** O pedido desta loja que está comigo. Outro qualquer é 404 — o mesmo de não existir. */
    private Pedido minha(UUID estabelecimentoId, UUID pedidoId) {
        ContextoDeAcesso eu = autorizador.contextoCom(estabelecimentoId, PermissaoDoPedido.VER_ENTREGA);
        return pedidos.buscarPorId(pedidoId)
                .filter(p -> p.getEstabelecimentoId().equals(estabelecimentoId))
                .filter(p -> p.estaCom(eu.usuarioId()))
                .orElseThrow(PedidoNaoEncontrado::new);
    }
}
