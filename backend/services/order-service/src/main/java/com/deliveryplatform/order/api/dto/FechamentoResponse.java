package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.FechamentoDoDia;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** O caixa fechado, como foi apurado e gravado. */
@Schema(description = "O fechamento do dia: totais, formas de pagamento, conferência e acertos.")
public record FechamentoResponse(
        UUID id,
        LocalDate dia,
        Instant fechadoEm,
        int pedidos,
        int concluidos,
        int cancelados,
        BigDecimal vendas,
        BigDecimal taxas,
        BigDecimal dinheiro,
        BigDecimal cartao,
        BigDecimal pix,
        @Schema(nullable = true) BigDecimal dinheiroContado,
        @Schema(description = "Contado menos as vendas em dinheiro; nulo sem contagem.", nullable = true)
        BigDecimal diferencaDeCaixa,
        BigDecimal repasseTotal,
        @Schema(nullable = true) String observacao,
        List<Acerto> acertos) {

    @Schema(description = "O acerto de um entregador: repasse = a taxa de cada entrega feita; "
            + "acerto = dinheiro recebido − repasse (positivo: ele devolve à loja).")
    public record Acerto(@Schema(nullable = true) UUID entregadorId, String nome, int entregas,
                         BigDecimal repasse, BigDecimal dinheiro, BigDecimal cartao,
                         BigDecimal acerto) {
    }

    public static FechamentoResponse de(FechamentoDoDia f) {
        return new FechamentoResponse(f.getId(), f.getDia(), f.getFechadoEm(), f.getPedidos(),
                f.getConcluidos(), f.getCancelados(), f.getVendas().valor(), f.getTaxas().valor(),
                f.getDinheiro().valor(), f.getCartao().valor(), f.getPix().valor(),
                f.getDinheiroContado() == null ? null : f.getDinheiroContado().valor(),
                f.diferencaDeCaixa() == null ? null : f.diferencaDeCaixa().valor(),
                f.repasseTotal().valor(), f.getObservacao(),
                f.getAcertos().stream()
                        .map(a -> new Acerto(a.entregadorId(), a.nome(), a.entregas(),
                                a.repasse().valor(), a.dinheiro().valor(), a.cartao().valor(),
                                a.acerto().valor()))
                        .toList());
    }
}
