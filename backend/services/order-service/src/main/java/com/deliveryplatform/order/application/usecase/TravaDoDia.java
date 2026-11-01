package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.port.out.FechamentoRepositorio;
import com.deliveryplatform.order.domain.exception.FechamentoRecusado;
import com.deliveryplatform.order.domain.model.Pedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Depois de fechado, o dia não muda (ADR-077, emenda de 01/11/2026): nenhum pedido daquele dia
 * avança, troca de entregador ou registra pagamento, e a loja não recebe pedido novo no dia que
 * já fechou. O dia é o da criação do pedido, no fuso de {@code delivery.pedidos.fuso} — o mesmo
 * da lista do painel e do fechamento.
 */
@Component
public class TravaDoDia {

    private final FechamentoRepositorio fechamentos;
    private final Clock relogio;
    private final ZoneId fuso;

    public TravaDoDia(FechamentoRepositorio fechamentos, Clock relogio,
                      @Value("${delivery.pedidos.fuso:America/Sao_Paulo}") String fuso) {
        this.fechamentos = fechamentos;
        this.relogio = relogio;
        this.fuso = ZoneId.of(fuso);
    }

    public LocalDate diaDe(Instant instante) {
        return LocalDate.ofInstant(instante, fuso);
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio.withZone(fuso));
    }

    public ZoneId fuso() {
        return fuso;
    }

    /** @throws FechamentoRecusado com {@code DIA_FECHADO} se o dia do pedido já foi fechado */
    public void exigirAberto(Pedido pedido) {
        if (fechamentos.existe(pedido.getEstabelecimentoId(), diaDe(pedido.getCriadoEm()))) {
            throw new FechamentoRecusado(FechamentoRecusado.Motivo.DIA_FECHADO, 0);
        }
    }

    /** A loja já fechou o caixa de hoje? Então não recebe pedido novo hoje. */
    public boolean hojeFechado(UUID estabelecimentoId) {
        return fechamentos.existe(estabelecimentoId, hoje());
    }
}
