package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.application.port.in.ConsultarConfiguracaoPublica.Vitrine;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A configuração de pedidos com a situação da loja — o que o checkout lê antes do carrinho
 * (ADR-077, emenda de 01/11/2026). Os três primeiros campos são os de
 * {@link ConfiguracaoDePedidosDto}, com os mesmos nomes: a tela que já os lia continua lendo.
 */
@Schema(description = "Taxa, modalidades e se a loja está recebendo pedidos agora.")
public record VitrineDaLojaResponse(
        @Schema(description = "Em reais. Na retirada não se cobra.", example = "5.00")
        BigDecimal taxaDeEntrega,
        boolean aceitaEntrega,
        boolean aceitaRetirada,
        @Schema(description = "Se a loja aceita pedido agora. Nulo quando não deu para confirmar; "
                + "nesse caso o pedido decide.", nullable = true)
        Boolean aberta,
        @Schema(description = "Com a loja fechada, quando ela abre. Nulo quando não abre por horário.",
                example = "2026-11-01T21:00:00Z", nullable = true)
        Instant proximaAbertura
) {
    public static VitrineDaLojaResponse de(Vitrine v) {
        return new VitrineDaLojaResponse(v.configuracao().getTaxaDeEntrega().valor(),
                v.configuracao().isAceitaEntrega(), v.configuracao().isAceitaRetirada(),
                v.aberta(), v.proximaAbertura());
    }
}
