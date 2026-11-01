package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;

import java.time.Instant;
import java.util.UUID;

/** Taxa e modalidades, para o checkout. Loja que nunca configurou: o padrão. */
public interface ConsultarConfiguracaoPublica {

    ConfiguracaoDePedidos daLoja(UUID estabelecimentoId);

    /**
     * O que a vitrine mostra antes do carrinho: a configuração e se a loja está aberta (ADR-077,
     * emenda de 01/11/2026).
     */
    Vitrine vitrine(UUID estabelecimentoId);

    /**
     * {@code aberta} nulo quando o {@code merchant} não respondeu: a vitrine abre assim mesmo, e
     * quem decide é o {@code POST} do pedido, que falha fechado. {@code proximaAbertura} nula
     * com a loja aberta, ou quando ela não abre por horário.
     */
    record Vitrine(ConfiguracaoDePedidos configuracao, Boolean aberta, Instant proximaAbertura) {
    }
}
