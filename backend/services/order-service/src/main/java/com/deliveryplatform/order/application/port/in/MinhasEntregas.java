package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.valuetypes.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O app do entregador: as entregas que estão com ele, e os dois atos que são dele. Exige
 * {@code VER_ENTREGA}; pedido que não está com ele é 404, o mesmo de não existir.
 */
public interface MinhasEntregas {

    List<Pedido> doDia(UUID estabelecimentoId, LocalDate dia);

    /** Só {@code SAIU_PARA_ENTREGA} e {@code ENTREGUE}. */
    Pedido avancar(UUID estabelecimentoId, UUID pedidoId, StatusDoPedido destino);

    /** Só {@code DINHEIRO} e {@code CARTAO}: o Pix é conferido pela loja. */
    Pedido registrarPagamento(UUID estabelecimentoId, UUID pedidoId, FormaDePagamento forma,
                              Money valorRecebido);
}
