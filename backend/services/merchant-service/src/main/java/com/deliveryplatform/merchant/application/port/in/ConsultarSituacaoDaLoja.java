package com.deliveryplatform.merchant.application.port.in;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Se a loja está recebendo pedidos agora — para quem compra pelo link do cardápio, sem sessão
 * (ADR-077, emenda de 01/11/2026).
 *
 * <p>É a primeira resposta em código à pergunta da {@code OperacaoDoEstabelecimentoPort} do
 * {@code estabelecimento.md} §3, e ela não é recalculada aqui: quem compõe horário, pausa e fuso
 * é o {@code Estabelecimento.estaAberta}.
 */
public interface ConsultarSituacaoDaLoja {

    /** @throws com.deliveryplatform.merchant.application.exception.LojaNaoEncontrada */
    SituacaoDaLoja daLoja(UUID estabelecimentoId);

    /**
     * {@code proximaAbertura} vazia com a loja fechada quer dizer que ela não abre por horário —
     * a loja recém-cadastrada, antes de alguém definir o horário.
     */
    record SituacaoDaLoja(String nome, boolean aberta, Optional<Instant> proximaAbertura) {
    }
}
