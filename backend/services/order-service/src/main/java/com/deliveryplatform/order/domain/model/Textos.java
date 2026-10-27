package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada.Motivo;

/** Texto livre que entra no pedido: aparado, obrigatório quando pedido, e com teto. */
final class Textos {

    private Textos() {
    }

    static String obrigatorio(String valor, String campo, int maximo) {
        String aparado = valor == null ? "" : valor.strip();
        if (aparado.isEmpty()) {
            throw new RegraDoPedidoViolada(Motivo.TEXTO_OBRIGATORIO, campo + " em branco", campo);
        }
        return comTeto(aparado, campo, maximo);
    }

    /** Nulo ou em branco vira nulo. */
    static String opcional(String valor, String campo, int maximo) {
        String aparado = valor == null ? "" : valor.strip();
        return aparado.isEmpty() ? null : comTeto(aparado, campo, maximo);
    }

    private static String comTeto(String valor, String campo, int maximo) {
        if (valor.length() > maximo) {
            throw new RegraDoPedidoViolada(Motivo.TEXTO_LONGO_DEMAIS,
                    campo + " com " + valor.length() + " caracteres", campo, String.valueOf(maximo));
        }
        return valor;
    }
}
