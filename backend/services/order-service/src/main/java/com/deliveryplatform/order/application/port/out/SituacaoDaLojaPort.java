package com.deliveryplatform.order.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Se a loja está recebendo pedidos agora — o {@code merchant} responde, pela rota pública da
 * vitrine (ADR-077, emenda de 01/11/2026). O {@code order} não guarda horário: é do agregado da
 * loja, que compõe horário, pausa e fuso.
 */
public interface SituacaoDaLojaPort {

    /** @throws SituacaoIndisponivel quando o {@code merchant} não responde */
    SituacaoDaLoja daLoja(UUID estabelecimentoId);

    /**
     * {@code proximaAbertura} vazia com a loja fechada: ela não abre por horário — ou o
     * identificador não é de loja nenhuma, que para quem compra dá no mesmo.
     */
    record SituacaoDaLoja(boolean aberta, Optional<Instant> proximaAbertura) {

        public static SituacaoDaLoja abertaAgora() {
            return new SituacaoDaLoja(true, Optional.empty());
        }

        public static SituacaoDaLoja fechada(Instant proximaAbertura) {
            return new SituacaoDaLoja(false, Optional.ofNullable(proximaAbertura));
        }
    }
}
