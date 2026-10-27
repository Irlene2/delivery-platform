package com.deliveryplatform.order.application.port.out;

import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepositorio {

    /**
     * Grava o pedido inteiro. Com versão: dois operadores mexendo no mesmo pedido ao mesmo
     * tempo, o segundo leva {@code OptimisticLockingFailureException} (409 na borda).
     */
    Pedido salvar(Pedido pedido);

    Optional<Pedido> buscarPorId(UUID id);

    Optional<Pedido> buscarPorCodigo(UUID codigoDeAcompanhamento);

    /**
     * Os pedidos da loja criados em {@code [desde, ate)}, do mais novo para o mais velho.
     *
     * @param status nulo = todos
     */
    List<Pedido> daLoja(UUID estabelecimentoId, StatusDoPedido status, Instant desde, Instant ate,
                        int limite);

    /**
     * O próximo número legível da loja (1, 2, 3…). Precisa de transação: a numeração é
     * travada por loja até o fim dela, e dois pedidos simultâneos nunca recebem o mesmo número.
     */
    long proximoNumero(UUID estabelecimentoId);
}
