package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.exception.FechamentoNaoEncontrado;
import com.deliveryplatform.order.application.port.in.FecharODia;
import com.deliveryplatform.order.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.order.application.port.out.FechamentoRepositorio;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.domain.exception.FechamentoRecusado;
import com.deliveryplatform.order.domain.model.FechamentoDoDia;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fecha o caixa do dia (ADR-077, emenda de 01/11/2026). Fechar exige {@code GERENCIAR_JORNADA}
 * — mexe em dinheiro, como fechar jornada no {@code merchant}; consultar exige
 * {@code VER_VENDAS}. O administrador tem as duas.
 *
 * <p>A apuração é do agregado, sobre os pedidos do dia lidos aqui: o navegador não manda total
 * nenhum, só a contagem do caixa, se quiser conferir.
 */
@Service
public class FechamentoDoDiaService implements FecharODia {

    /** O mesmo teto da lista do painel: um dia de loja pequena cabe folgado. */
    static final int TETO = PedidosDaLojaService.TETO_DA_LISTA;
    static final int HISTORICO = 60;

    private final Autorizador autorizador;
    private final PedidoRepositorio pedidos;
    private final FechamentoRepositorio fechamentos;
    private final TravaDoDia trava;
    private final Clock relogio;

    public FechamentoDoDiaService(Autorizador autorizador, PedidoRepositorio pedidos,
                                  FechamentoRepositorio fechamentos, TravaDoDia trava,
                                  Clock relogio) {
        this.autorizador = autorizador;
        this.pedidos = pedidos;
        this.fechamentos = fechamentos;
        this.trava = trava;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public FechamentoDoDia fechar(UUID estabelecimentoId, LocalDate dia, Money dinheiroContado,
                                  String observacao) {
        ContextoDeAcesso contexto =
                autorizador.contextoCom(estabelecimentoId, PermissaoDoPedido.GERENCIAR_JORNADA);
        LocalDate alvo = dia != null ? dia : trava.hoje();
        if (alvo.isAfter(trava.hoje())) {
            throw new FechamentoRecusado(FechamentoRecusado.Motivo.DIA_FUTURO, 0);
        }
        if (fechamentos.existe(estabelecimentoId, alvo)) {
            throw new FechamentoRecusado(FechamentoRecusado.Motivo.JA_FECHADO, 0);
        }

        Instant desde = alvo.atStartOfDay(trava.fuso()).toInstant();
        Instant ate = alvo.plusDays(1).atStartOfDay(trava.fuso()).toInstant();
        List<Pedido> doDia = pedidos.daLoja(estabelecimentoId, null, desde, ate, TETO);

        FechamentoDoDia fechamento = FechamentoDoDia.apurar(UUID.randomUUID(), estabelecimentoId,
                alvo, doDia, contexto.usuarioId(), dinheiroContado, observacao,
                Instant.now(relogio));
        return fechamentos.salvar(fechamento);
    }

    @Override
    @Transactional(readOnly = true)
    public FechamentoDoDia doDia(UUID estabelecimentoId, LocalDate dia) {
        autorizador.exigir(estabelecimentoId, PermissaoDoPedido.VER_VENDAS);
        return fechamentos.doDia(estabelecimentoId, dia).orElseThrow(FechamentoNaoEncontrado::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FechamentoDoDia> recentes(UUID estabelecimentoId) {
        autorizador.exigir(estabelecimentoId, PermissaoDoPedido.VER_VENDAS);
        return fechamentos.recentes(estabelecimentoId, HISTORICO);
    }
}
