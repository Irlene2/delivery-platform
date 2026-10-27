package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada.Motivo;
import com.deliveryplatform.valuetypes.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Um item, <b>congelado</b>: o nome e o preço são os que o catálogo tinha no instante do
 * pedido (ADR-009, pedido.md §5). Mudar o cardápio depois não muda o pedido.
 *
 * <p>Sem as opções do produto nesta entrega (docs/entrega-15-11/pedido.md §6).
 */
public record ItemDoPedido(
        UUID produtoId,
        String nome,
        Money precoUnitario,
        int quantidade,
        String observacao) {

    public static final int QUANTIDADE_MAXIMA = 99;

    public ItemDoPedido {
        Objects.requireNonNull(produtoId, "produtoId");
        nome = Textos.obrigatorio(nome, "nome do produto", 120);
        if (precoUnitario == null || precoUnitario.ehNegativo()) {
            throw RegraDoPedidoViolada.integridade("preço unitário ausente ou negativo");
        }
        if (quantidade < 1 || quantidade > QUANTIDADE_MAXIMA) {
            throw new RegraDoPedidoViolada(Motivo.QUANTIDADE_INVALIDA,
                    "quantidade " + quantidade, nome, String.valueOf(QUANTIDADE_MAXIMA));
        }
        observacao = Textos.opcional(observacao, "observação do item", 140);
    }

    public Money subtotal() {
        return precoUnitario.vezes(quantidade);
    }
}
