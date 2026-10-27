package com.deliveryplatform.order.application.port.out;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** O que o {@code merchant} respondeu sobre o portador do token nesta loja, no recorte do pedido. */
public record ContextoDeAcesso(UUID usuarioId, UUID estabelecimentoId, Set<PermissaoDoPedido> permissoes) {

    public ContextoDeAcesso {
        permissoes = permissoes == null || permissoes.isEmpty()
                ? EnumSet.noneOf(PermissaoDoPedido.class)
                : EnumSet.copyOf(permissoes);
    }

    public boolean pode(PermissaoDoPedido permissao) {
        return permissoes.contains(permissao);
    }

    @Override
    public Set<PermissaoDoPedido> permissoes() {
        return EnumSet.copyOf(permissoes);
    }
}
