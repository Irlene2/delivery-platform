package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.valuetypes.Money;

import java.util.UUID;

/** A taxa de entrega e as modalidades da loja, pelo painel. Exige {@code CONFIGURAR_LOJA}. */
public interface ConfigurarPedidos {

    ConfiguracaoDePedidos ler(UUID estabelecimentoId);

    ConfiguracaoDePedidos definir(UUID estabelecimentoId, Money taxaDeEntrega,
                                  boolean aceitaEntrega, boolean aceitaRetirada);
}
