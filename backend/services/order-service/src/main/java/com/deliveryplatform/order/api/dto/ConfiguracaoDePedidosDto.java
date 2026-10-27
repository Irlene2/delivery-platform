package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Ida e volta com a mesma forma: o que o painel grava é o que o checkout lê. */
@Schema(description = "A taxa fixa de entrega da loja e as modalidades aceitas.")
public record ConfiguracaoDePedidosDto(

        @Schema(description = "Em reais. Na retirada não se cobra.", example = "5.00")
        @NotNull(message = "taxaDeEntrega é obrigatória")
        BigDecimal taxaDeEntrega,

        @NotNull(message = "aceitaEntrega é obrigatório")
        Boolean aceitaEntrega,

        @NotNull(message = "aceitaRetirada é obrigatório")
        Boolean aceitaRetirada
) {

    public static ConfiguracaoDePedidosDto de(ConfiguracaoDePedidos c) {
        return new ConfiguracaoDePedidosDto(c.getTaxaDeEntrega().valor(), c.isAceitaEntrega(),
                c.isAceitaRetirada());
    }
}
