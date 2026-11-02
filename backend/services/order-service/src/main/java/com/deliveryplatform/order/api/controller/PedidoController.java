package com.deliveryplatform.order.api.controller;

import com.deliveryplatform.order.api.dto.ConfiguracaoDePedidosDto;
import com.deliveryplatform.order.api.dto.EntregadorRequest;
import com.deliveryplatform.order.api.dto.PagamentoRegistradoRequest;
import com.deliveryplatform.order.api.dto.PedidoResponse;
import com.deliveryplatform.order.api.dto.StatusRequest;
import com.deliveryplatform.order.application.port.in.ConfigurarPedidos;
import com.deliveryplatform.order.application.port.in.ConsultarPedido;
import com.deliveryplatform.order.application.port.in.ListarPedidos;
import com.deliveryplatform.order.application.port.in.OperarPedido;
import com.deliveryplatform.order.config.OpenApiConfig;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.valuetypes.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O painel de pedidos da loja. Toda rota exige token, e a permissão é resolvida no
 * {@code merchant} a cada requisição (ADR-011). Pedido de outra loja é 404 (ADR-056).
 */
@RestController
@RequestMapping("/api/v1/merchants/{estabelecimentoId}/orders")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA)
public class PedidoController {

    private final ListarPedidos lista;
    private final ConsultarPedido consulta;
    private final OperarPedido operacao;
    private final ConfigurarPedidos configuracao;

    public PedidoController(ListarPedidos lista, ConsultarPedido consulta, OperarPedido operacao,
                            ConfigurarPedidos configuracao) {
        this.lista = lista;
        this.consulta = consulta;
        this.operacao = operacao;
        this.configuracao = configuracao;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Os pedidos do dia, do mais novo para o mais velho"),
            @ApiResponse(responseCode = "400", description = "Identificador, status ou data malformados", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_PEDIDO, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lista os pedidos de um dia",
            description = "Exige VER_PEDIDO. dia em AAAA-MM-DD no fuso da loja; sem ele, hoje. "
                    + "status filtra; sem ele, todos. Até 500 pedidos.")
    public List<PedidoResponse> listar(
            @PathVariable UUID estabelecimentoId,
            @RequestParam(required = false) StatusDoPedido status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
        return lista.daLoja(estabelecimentoId, status, dia).stream().map(PedidoResponse::de).toList();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A configuração; o padrão quando a loja nunca configurou"),
            @ApiResponse(responseCode = "400", description = "Identificador malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem CONFIGURAR_LOJA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @GetMapping(path = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "A taxa de entrega e as modalidades da loja", description = "Exige CONFIGURAR_LOJA.")
    public ConfiguracaoDePedidosDto lerConfiguracao(@PathVariable UUID estabelecimentoId) {
        return ConfiguracaoDePedidosDto.de(configuracao.ler(estabelecimentoId));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A configuração gravada"),
            @ApiResponse(responseCode = "400", description = "Taxa negativa, nenhuma modalidade, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem CONFIGURAR_LOJA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @PutMapping(path = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Define a taxa de entrega e as modalidades",
            description = "Exige CONFIGURAR_LOJA. Vale para os pedidos seguintes; os já feitos "
                    + "guardam a taxa do instante em que nasceram.")
    public ConfiguracaoDePedidosDto definirConfiguracao(
            @PathVariable UUID estabelecimentoId,
            @Valid @RequestBody ConfiguracaoDePedidosDto pedido) {
        return ConfiguracaoDePedidosDto.de(configuracao.definir(estabelecimentoId,
                Money.de(pedido.taxaDeEntrega()), pedido.aceitaEntrega(), pedido.aceitaRetirada()));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O pedido inteiro"),
            @ApiResponse(responseCode = "400", description = "Identificador malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_PEDIDO, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "O pedido não existe nesta loja — inexistente ou de outra loja, a mesma resposta", content = @Content)
    })
    @GetMapping(path = "/{pedidoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Um pedido, com telefone e endereço", description = "Exige VER_PEDIDO.")
    public PedidoResponse consultar(@PathVariable UUID estabelecimentoId, @PathVariable UUID pedidoId) {
        return PedidoResponse.de(consulta.consultar(estabelecimentoId, pedidoId));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O pedido no status novo"),
            @ApiResponse(responseCode = "400", description = "Cancelamento sem motivo, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem ALTERAR_STATUS, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "O pedido não existe nesta loja", content = @Content),
            @ApiResponse(responseCode = "409", description = "Transição que a tabela não permite, saída sem entregador, conclusão sem pagamento registrado, ou o pedido mudou enquanto você mexia", content = @Content)
    })
    @PutMapping(path = "/{pedidoId}/status", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Muda o status do pedido",
            description = "Exige ALTERAR_STATUS. A tabela de transições está em "
                    + "docs/entrega-15-11/pedido.md §4.")
    public PedidoResponse mudarStatus(@PathVariable UUID estabelecimentoId,
                                      @PathVariable UUID pedidoId,
                                      @Valid @RequestBody StatusRequest pedido) {
        return PedidoResponse.de(operacao.avancar(estabelecimentoId, pedidoId, pedido.status(),
                pedido.motivo()));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O pedido com o entregador"),
            @ApiResponse(responseCode = "400", description = "Nome em branco, entregador fora da equipe, pedido de retirada, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem ALTERAR_STATUS, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "O pedido não existe nesta loja", content = @Content),
            @ApiResponse(responseCode = "409", description = "O pedido já foi encerrado, ou mudou enquanto você mexia", content = @Content)
    })
    @PutMapping(path = "/{pedidoId}/courier", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Informa quem leva o pedido",
            description = "Exige ALTERAR_STATUS. Só na entrega. Com usuarioId, um entregador da "
                    + "equipe, que passa a ver o pedido no app dele.")
    public PedidoResponse informarEntregador(@PathVariable UUID estabelecimentoId,
                                             @PathVariable UUID pedidoId,
                                             @RequestBody EntregadorRequest pedido) {
        return PedidoResponse.de(operacao.informarEntregador(estabelecimentoId, pedidoId,
                pedido.usuarioId(), pedido.nome()));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O pedido com o pagamento registrado"),
            @ApiResponse(responseCode = "400", description = "Dinheiro sem valor ou com valor menor que o total, Pix não conferido, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem ALTERAR_STATUS, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "O pedido não existe nesta loja", content = @Content),
            @ApiResponse(responseCode = "409", description = "O pedido ainda não está pronto, já foi encerrado, ou mudou enquanto você mexia", content = @Content)
    })
    @PutMapping(path = "/{pedidoId}/settlement", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registra como o pedido foi pago",
            description = "Exige ALTERAR_STATUS. A partir de PRONTO. Em dinheiro, o servidor "
                    + "calcula o troco; em Pix, só vale conferido no extrato.")
    public PedidoResponse registrarPagamento(@PathVariable UUID estabelecimentoId,
                                             @PathVariable UUID pedidoId,
                                             @Valid @RequestBody PagamentoRegistradoRequest pedido) {
        return PedidoResponse.de(operacao.registrarPagamento(estabelecimentoId, pedidoId,
                pedido.formaEfetiva(),
                pedido.valorRecebido() == null ? null : Money.de(pedido.valorRecebido()),
                Boolean.TRUE.equals(pedido.pixConferido())));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O Pix conferido"),
            @ApiResponse(responseCode = "400", description = "O pedido não tem Pix a conferir", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem ALTERAR_STATUS, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "O pedido não existe nesta loja", content = @Content),
            @ApiResponse(responseCode = "409", description = "O caixa do dia deste pedido já foi fechado, ou o pedido mudou enquanto você mexia", content = @Content)
    })
    @PutMapping(path = "/{pedidoId}/pix-check", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirma o Pix que o entregador informou",
            description = "Exige ALTERAR_STATUS. Vale em qualquer estado, inclusive depois de "
                    + "entregue: é quando a loja costuma olhar o extrato.")
    public PedidoResponse conferirPix(@PathVariable UUID estabelecimentoId,
                                      @PathVariable UUID pedidoId) {
        return PedidoResponse.de(operacao.conferirPix(estabelecimentoId, pedidoId));
    }
}
