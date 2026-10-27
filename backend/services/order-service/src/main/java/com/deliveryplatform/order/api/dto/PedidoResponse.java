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

/** O pedido inteiro, para o painel da loja: com o cliente, o endereço e o pagamento registrado. */
@Schema(description = "O pedido como a loja o opera.")
public record PedidoResponse(
        UUID id,
        String numero,
        StatusDoPedido status,
        Modalidade modalidade,
        Cliente cliente,
        Endereco endereco,
        List<ItemResponse> itens,
        BigDecimal subtotal,
        BigDecimal taxaDeEntrega,
        BigDecimal total,
        PedidoPublicoResponse.PagamentoDeclaradoResponse pagamentoDeclarado,
        PagamentoRegistrado pagamentoRegistrado,
        String entregador,
        String motivoDoCancelamento,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public record Cliente(String nome,
                          @Schema(description = "E.164 — o painel monta o link wa.me com ele.",
                                  example = "+5521999990000") String telefone) {
    }

    public record Endereco(String logradouro, String bairro, String complemento) {
    }

    public record PagamentoRegistrado(FormaDePagamento formaEfetiva, BigDecimal valorRecebido,
                                      BigDecimal trocoDevolvido, boolean pixConferido,
                                      Instant registradoEm) {
    }

    public static PedidoResponse de(Pedido p) {
        var e = p.getEndereco();
        var r = p.getPagamentoRegistrado();
        var d = p.getPagamentoDeclarado();
        return new PedidoResponse(p.getId(), p.numeroLegivel(), p.getStatus(), p.getModalidade(),
                new Cliente(p.getCliente().nome(), p.getCliente().telefone().numero()),
                e == null ? null : new Endereco(e.logradouro(), e.bairro(), e.complemento()),
                p.getItens().stream().map(ItemResponse::de).toList(),
                p.getSubtotal().valor(), p.getTaxaDeEntrega().valor(), p.getTotal().valor(),
                new PedidoPublicoResponse.PagamentoDeclaradoResponse(d.forma(),
                        d.trocoPara() == null ? null : d.trocoPara().valor()),
                r == null ? null : new PagamentoRegistrado(r.formaEfetiva(),
                        r.valorRecebido() == null ? null : r.valorRecebido().valor(),
                        r.trocoDevolvido() == null ? null : r.trocoDevolvido().valor(),
                        r.pixConferido(), r.registradoEm()),
                p.getEntregador(), p.getMotivoDoCancelamento(), p.getCriadoEm(),
                p.getAtualizadoEm());
    }
}
