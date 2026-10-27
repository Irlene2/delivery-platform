package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;

/** Quem pediu. Sem conta: o pedido pelo cardápio público pede só nome e telefone. */
public record Cliente(String nome, Telefone telefone) {

    public static final int MAXIMO_DO_NOME = 120;

    public Cliente {
        nome = Textos.obrigatorio(nome, "nome do cliente", MAXIMO_DO_NOME);
        if (telefone == null) {
            throw RegraDoPedidoViolada.integridade("cliente sem telefone");
        }
    }

    public static Cliente de(String nome, String telefone) {
        return new Cliente(nome, Telefone.de(telefone));
    }
}
