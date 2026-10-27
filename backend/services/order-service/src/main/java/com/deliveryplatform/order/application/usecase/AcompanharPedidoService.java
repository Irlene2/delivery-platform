package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.port.in.AcompanharPedido;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.domain.model.Pedido;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sem autorização, e é seguro: o código é um UUID aleatório que só quem fez o pedido recebeu.
 * O que a borda devolve aqui não leva telefone nem endereço (ADR-013).
 */
@Service
public class AcompanharPedidoService implements AcompanharPedido {

    private final PedidoRepositorio pedidos;

    public AcompanharPedidoService(PedidoRepositorio pedidos) {
        this.pedidos = pedidos;
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido porCodigo(UUID codigoDeAcompanhamento) {
        return pedidos.buscarPorCodigo(codigoDeAcompanhamento)
                .orElseThrow(PedidoNaoEncontrado::new);
    }
}
