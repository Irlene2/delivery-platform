package com.deliveryplatform.order.application.port.out;

import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;

import java.util.Optional;
import java.util.UUID;

public interface ConfiguracaoDePedidosRepositorio {

    /** Vazio quando a loja nunca configurou — quem chama usa {@code ConfiguracaoDePedidos.padrao}. */
    Optional<ConfiguracaoDePedidos> daLoja(UUID estabelecimentoId);

    ConfiguracaoDePedidos salvar(ConfiguracaoDePedidos configuracao);
}
