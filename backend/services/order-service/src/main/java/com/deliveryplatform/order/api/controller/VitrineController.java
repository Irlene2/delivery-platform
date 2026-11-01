package com.deliveryplatform.order.api.controller;

import com.deliveryplatform.order.api.dto.VitrineDaLojaResponse;
import com.deliveryplatform.order.api.dto.PedidoPublicoResponse;
import com.deliveryplatform.order.api.dto.PedidoRequest;
import com.deliveryplatform.order.application.port.in.AcompanharPedido;
import com.deliveryplatform.order.application.port.in.ConsultarConfiguracaoPublica;
import com.deliveryplatform.order.application.port.in.FazerPedido;
import com.deliveryplatform.valuetypes.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * As rotas públicas do pedido — o cliente compra pelo link do cardápio, sem conta (ADR-077).
 * Nenhuma delas recebe token, e nenhuma devolve telefone ou endereço.
 */
@RestController
@RequestMapping("/api/v1/storefront")
@Tag(name = "vitrine", description = "Público, sem token: o cliente pelo link do cardápio.")
public class VitrineController {

    private final ConsultarConfiguracaoPublica configuracoes;
    private final FazerPedido criacao;
    private final AcompanharPedido acompanhamento;

    public VitrineController(ConsultarConfiguracaoPublica configuracoes, FazerPedido criacao,
                             AcompanharPedido acompanhamento) {
        this.configuracoes = configuracoes;
        this.criacao = criacao;
        this.acompanhamento = acompanhamento;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Taxa e modalidades (o padrão quando a loja nunca configurou), e se a loja está aberta agora"),
            @ApiResponse(responseCode = "400", description = "Identificador da loja malformado", content = @Content)
    })
    @GetMapping(path = "/{estabelecimentoId}/orders/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Taxa de entrega, modalidades e se a loja está aberta, para o checkout",
            description = "Loja que nunca configurou: taxa 0,00 e as duas modalidades. A situação "
                    + "vem do merchant; sem resposta dele, aberta é nula.")
    public VitrineDaLojaResponse configuracao(@PathVariable UUID estabelecimentoId) {
        return VitrineDaLojaResponse.de(configuracoes.vitrine(estabelecimentoId));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "O pedido criado, com o código de acompanhamento"),
            @ApiResponse(responseCode = "400", description = "Recusado, com a frase que diz o que corrigir: produto fora do cardápio ou que acabou, endereço na entrega, troco menor que o total, modalidade que a loja não aceita, campo obrigatório", content = @Content),
            @ApiResponse(responseCode = "409", description = "A loja está fechada agora; a frase diz quando abre", content = @Content),
            @ApiResponse(responseCode = "503", description = "O catálogo ou a loja não responderam; o pedido não foi criado", content = @Content)
    })
    @PostMapping(path = "/{estabelecimentoId}/orders",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Faz o pedido",
            description = "O preço de cada item é lido no catálogo nesta hora e congelado no "
                    + "pedido; o total da resposta é o que vale. Location aponta para o "
                    + "acompanhamento.")
    public ResponseEntity<PedidoPublicoResponse> fazer(
            @PathVariable UUID estabelecimentoId,
            @Valid @RequestBody PedidoRequest pedido) {

        var endereco = pedido.endereco() == null ? null
                : new FazerPedido.EnderecoInformado(pedido.endereco().logradouro(),
                        pedido.endereco().bairro(), pedido.endereco().complemento());
        var itens = pedido.itens().stream()
                .map(i -> new FazerPedido.ItemSolicitado(i.produtoId(), i.quantidade(), i.observacao()))
                .toList();
        var troco = pedido.pagamento().trocoPara() == null ? null
                : Money.de(pedido.pagamento().trocoPara());

        var criado = criacao.fazer(estabelecimentoId, new FazerPedido.Solicitacao(
                pedido.cliente().nome(), pedido.cliente().telefone(), pedido.modalidade(),
                endereco, itens, pedido.pagamento().forma(), troco));

        return ResponseEntity
                .created(URI.create("/api/v1/storefront/orders/" + criado.getCodigoDeAcompanhamento()))
                .body(PedidoPublicoResponse.de(criado));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O pedido, sem telefone nem endereço"),
            @ApiResponse(responseCode = "400", description = "Código malformado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Nenhum pedido com este código", content = @Content)
    })
    @GetMapping(path = "/orders/{codigoDeAcompanhamento}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Acompanha o pedido pelo código recebido na criação")
    public PedidoPublicoResponse acompanhar(@PathVariable UUID codigoDeAcompanhamento) {
        return PedidoPublicoResponse.de(acompanhamento.porCodigo(codigoDeAcompanhamento));
    }
}
