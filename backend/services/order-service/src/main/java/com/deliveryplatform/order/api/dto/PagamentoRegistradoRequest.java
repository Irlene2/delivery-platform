package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Como o pedido foi pago, de fato. Pode ser outra forma que a declarada.")
public record PagamentoRegistradoRequest(

        @Schema(example = "DINHEIRO")
        @NotNull(message = "formaEfetiva é obrigatória")
        FormaDePagamento formaEfetiva,

        @Schema(description = "Só em DINHEIRO, e maior ou igual ao total; o troco é calculado "
                + "pelo servidor.", example = "110.00")
        BigDecimal valorRecebido,

        @Schema(description = "Só em PIX: conferido no extrato. Comprovante na tela não vale.")
        Boolean pixConferido
) {
}
