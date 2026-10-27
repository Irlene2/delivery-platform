package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O que o cliente vê — na criação e no acompanhamento. <b>Sem telefone e sem endereço</b>
 * (ADR-013): quem tem o código vê o pedido, não a pessoa.
 */
@Schema(description = "O pedido como o cliente o acompanha, sem dados pessoais.")
public record PedidoPublicoResponse(
        UUID id,
        UUID estabelecimentoId,
        @Schema(description = "O número legível da loja, com seis dígitos.", example = "000123")
        String numero,
        UUID codigoDeAcompanhamento,
        StatusDoPedido status,
        Modalidade modalidade,
        List<ItemResponse> itens,
        BigDecimal subtotal,
        BigDecimal taxaDeEntrega,
        BigDecimal total,
        PagamentoDeclaradoResponse pagamento,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public record PagamentoDeclaradoResponse(FormaDePagamento forma, BigDecimal trocoPara) {
    }

    public static PedidoPublicoResponse de(Pedido p) {
        return new PedidoPublicoResponse(p.getId(), p.getEstabelecimentoId(), p.numeroLegivel(),
                p.getCodigoDeAcompanhamento(), p.getStatus(), p.getModalidade(),
                p.getItens().stream().map(ItemResponse::de).toList(),
                p.getSubtotal().valor(), p.getTaxaDeEntrega().valor(), p.getTotal().valor(),
                new PagamentoDeclaradoResponse(p.getPagamentoDeclarado().forma(),
                        p.getPagamentoDeclarado().trocoPara() == null ? null
                                : p.getPagamentoDeclarado().trocoPara().valor()),
                p.getCriadoEm(), p.getAtualizadoEm());
    }
}
