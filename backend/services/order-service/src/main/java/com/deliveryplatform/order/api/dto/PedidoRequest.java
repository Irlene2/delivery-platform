package com.deliveryplatform.order.api.dto;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Modalidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * O carrinho fechado. <b>Sem preço</b>: o preço é lido no catálogo pelo servidor. As regras de
 * negócio (endereço na entrega, troco ≥ total…) são do domínio; aqui só a forma.
 */
@Schema(description = "O pedido do cliente, feito pelo link do cardápio. Nenhum preço vem daqui.")
public record PedidoRequest(

        @Valid @NotNull(message = "cliente é obrigatório")
        Cliente cliente,

        @Schema(description = "ENTREGA ou RETIRADA.", example = "ENTREGA")
        @NotNull(message = "modalidade é obrigatória")
        Modalidade modalidade,

        @Schema(description = "Obrigatório na entrega; recusado na retirada.")
        Endereco endereco,

        @Valid
        @NotEmpty(message = "o pedido precisa de ao menos um item")
        @Size(max = 50, message = "no máximo 50 itens por pedido")
        List<Item> itens,

        @Valid @NotNull(message = "pagamento é obrigatório")
        Pagamento pagamento
) {

    public record Cliente(
            @Schema(example = "Maria Souza") String nome,
            @Schema(description = "Com ou sem DDI e formatação; o servidor normaliza.",
                    example = "(21) 99999-0000") String telefone) {
    }

    public record Endereco(
            @Schema(example = "Rua A, 10") String logradouro,
            @Schema(example = "Centro") String bairro,
            @Schema(example = "ap 2") String complemento) {
    }

    public record Item(
            @NotNull(message = "produtoId é obrigatório") UUID produtoId,
            @Schema(description = "De 1 a 99.", example = "2") int quantidade,
            @Schema(example = "sem cebola") String observacao) {
    }

    public record Pagamento(
            @Schema(description = "DINHEIRO, CARTAO ou PIX — na entrega ou no balcão.",
                    example = "DINHEIRO")
            @NotNull(message = "pagamento.forma é obrigatória") FormaDePagamento forma,
            @Schema(description = "Só em DINHEIRO, e maior ou igual ao total.", example = "110.00")
            BigDecimal trocoPara) {
    }
}
