package com.deliveryplatform.order.application.port.out;

import java.util.Optional;

/**
 * As permissões que o {@code order} sabe nomear — o recorte da {@code Permissao} do
 * {@code merchant} sobre o qual este serviço tem regra. Cópia deliberada da forma da
 * {@code PermissaoDoCatalogo}: a ADR-001 proíbe importar de outro serviço, e o contrato
 * entre os dois é o texto.
 *
 * <p>{@link #de(String)} perdoa: permissão que este serviço não conhece é descartada,
 * nunca concedida (ADR-027 §2).
 */
public enum PermissaoDoPedido {

    VER_PEDIDO,
    ALTERAR_STATUS,
    /** O entregador da equipe: vê e opera só as entregas dele. */
    VER_ENTREGA,
    CONFIGURAR_LOJA,
    /** Ver o histórico dos fechamentos — leitura de vendas (ADR-077, emenda de 01/11/2026). */
    VER_VENDAS,
    /** Fechar o caixa do dia: mexe em dinheiro, como fechar jornada no {@code merchant}. */
    GERENCIAR_JORNADA;

    public static Optional<PermissaoDoPedido> de(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        for (PermissaoDoPedido permissao : values()) {
            if (permissao.name().equals(nome)) {
                return Optional.of(permissao);
            }
        }
        return Optional.empty();
    }
}
