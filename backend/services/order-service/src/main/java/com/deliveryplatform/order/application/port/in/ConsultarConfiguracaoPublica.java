package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;

import java.util.UUID;

/** Taxa e modalidades, para o checkout. Loja que nunca configurou: o padrão. */
public interface ConsultarConfiguracaoPublica {

    ConfiguracaoDePedidos daLoja(UUID estabelecimentoId);
}
