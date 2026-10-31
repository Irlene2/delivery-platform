package com.deliveryplatform.order.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Um entregador da equipe da loja, como o pedido o conhece: a conta (o {@code usuarioId} do
 * token) e o nome com que a loja o reconhece.
 *
 * <p>Mora no {@code order}, e não no {@code merchant}: a equipe do {@code merchant} não guarda
 * nome, e a entrega de 15/11 não mexe no agregado da loja (ADR-077). O próprio entregador se
 * apresenta na primeira vez que abre o app — quem garante que ele é da equipe é a permissão
 * {@code VER_ENTREGA}, resolvida no {@code merchant} a cada requisição.
 */
public record EntregadorDaLoja(UUID estabelecimentoId, UUID usuarioId, String nome) {

    public EntregadorDaLoja {
        Objects.requireNonNull(estabelecimentoId, "estabelecimentoId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        nome = Textos.obrigatorio(nome, "nome do entregador", Pedido.MAXIMO_DO_ENTREGADOR);
    }
}
