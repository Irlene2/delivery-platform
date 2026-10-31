package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.domain.exception.TransicaoInvalidaException;
import com.deliveryplatform.order.domain.model.EstadoDoPedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.deliveryplatform.order.domain.model.EstadoDoPedido.*;
import static org.junit.jupiter.api.Assertions.*;

class EstadoDoPedidoTest {

    // Cópia da tabela de pedido.md §3, escrita de forma independente do código.
    private static final Set<List<EstadoDoPedido>> TABELA = Set.of(
            List.of(RECEBIDO, CONFIRMADO),            // T02
            List.of(RECEBIDO, PAGO),                  // T03
            List.of(RECEBIDO, AGUARDANDO_ESTOQUE),    // T04
            List.of(AGUARDANDO_ESTOQUE, CONFIRMADO),  // T05
            List.of(AGUARDANDO_ESTOQUE, CANCELADO),   // T06
            List.of(RECEBIDO, CANCELADO),             // T07, T08
            List.of(CONFIRMADO, EM_PREPARO),          // T09
            List.of(PAGO, EM_PREPARO),                // T10
            List.of(CONFIRMADO, CANCELADO),           // T11
            List.of(PAGO, CANCELADO),                 // T12
            List.of(EM_PREPARO, EM_PREPARO),          // T13
            List.of(EM_PREPARO, PRONTO),              // T14
            List.of(EM_PREPARO, CANCELADO),           // T15
            List.of(PRONTO, SAIU_PARA_ENTREGA),       // T16
            List.of(PRONTO, AGUARDANDO_CLIENTE),      // T17
            List.of(PRONTO, CANCELADO),               // T18
            List.of(SAIU_PARA_ENTREGA, ENTREGUE),     // T19
            List.of(SAIU_PARA_ENTREGA, NAO_ENTREGUE), // T20
            List.of(NAO_ENTREGUE, SAIU_PARA_ENTREGA), // T21
            List.of(NAO_ENTREGUE, ENTREGUE),          // T22
            List.of(NAO_ENTREGUE, CANCELADO),         // T23
            List.of(AGUARDANDO_CLIENTE, RETIRADO),    // T24
            List.of(AGUARDANDO_CLIENTE, CANCELADO)    // T25
    );

    static Stream<Arguments> todosOsPares() {
        return Arrays.stream(values()).flatMap(de ->
                Arrays.stream(values()).map(para ->
                        Arguments.of(de, para, TABELA.contains(List.of(de, para)))));
    }

    @ParameterizedTest(name = "{0} -> {1}: permitido = {2}")
    @MethodSource("todosOsPares")
    void apenasAsTransicoesDaTabelaSaoPermitidas(EstadoDoPedido de, EstadoDoPedido para, boolean esperado) {
        assertEquals(esperado, de.podeIrPara(para));
        if (esperado) {
            assertEquals(para, de.irPara(para));
        } else {
            assertThrows(TransicaoInvalidaException.class, () -> de.irPara(para));
        }
    }

    @Test
    void estadosTerminaisNaoTemSaida() {
        assertTrue(ENTREGUE.terminal());
        assertTrue(RETIRADO.terminal());
        assertTrue(CANCELADO.terminal());
    }

    @Test
    void nenhumEstadoNaoTerminalFicaSemSaida() {
        for (EstadoDoPedido estado : values()) {
            if (estado != ENTREGUE && estado != RETIRADO && estado != CANCELADO) {
                assertFalse(estado.terminal(), estado + " ficaria sem saída");
            }
        }
    }

    @Test
    void todoEstadoEAlcancavelAPartirDeRecebido() {
        Set<EstadoDoPedido> visitados = EnumSet.of(RECEBIDO);
        ArrayDeque<EstadoDoPedido> fila = new ArrayDeque<>(List.of(RECEBIDO));
        while (!fila.isEmpty()) {
            for (EstadoDoPedido proximo : fila.poll().saidas()) {
                if (visitados.add(proximo)) {
                    fila.add(proximo);
                }
            }
        }
        assertEquals(EnumSet.allOf(EstadoDoPedido.class), visitados);
    }
}