package com.deliveryplatform.order.api.controller;

import com.deliveryplatform.order.api.dto.ApresentacaoRequest;
import com.deliveryplatform.order.api.dto.EntregadorResponse;
import com.deliveryplatform.order.api.dto.PedidoResponse;
import com.deliveryplatform.order.api.dto.RecebimentoRequest;
import com.deliveryplatform.order.api.dto.StatusDaEntregaRequest;
import com.deliveryplatform.order.application.port.in.EntregadoresDaLoja;
import com.deliveryplatform.order.application.port.in.MinhasEntregas;
import com.deliveryplatform.order.config.OpenApiConfig;
import com.deliveryplatform.valuetypes.Money;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * O entregador da equipe (decisão de 31/10/2026). A loja lista quem entrega; o entregador se
 * apresenta, vê as entregas dele e opera os dois passos que são dele.
 *
 * <p>Os segmentos {@code couriers} e {@code deliveries} são fixos, e ganham do
 * {@code /{pedidoId}} do {@link PedidoController} no ranking do Spring.
 */
@RestController
@RequestMapping("/api/v1/merchants/{estabelecimentoId}/orders")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA)
@Tag(name = "entregador", description = "O entregador da equipe, com a conta dele.")
public class EntregadorController {

    private final EntregadoresDaLoja equipe;
    private final MinhasEntregas entregas;

    public EntregadorController(EntregadoresDaLoja equipe, MinhasEntregas entregas) {
        this.equipe = equipe;
        this.entregas = entregas;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Os entregadores que já se apresentaram, pelo nome"),
            @ApiResponse(responseCode = "400", description = "Identificador malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_PEDIDO, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @GetMapping(path = "/couriers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Os entregadores da equipe", description = "Exige VER_PEDIDO.")
    public List<EntregadorResponse> listar(@PathVariable UUID estabelecimentoId) {
        return equipe.listar(estabelecimentoId).stream().map(EntregadorResponse::de).toList();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Como a loja me reconhece"),
            @ApiResponse(responseCode = "204", description = "Ainda não me apresentei nesta loja"),
            @ApiResponse(responseCode = "400", description = "Identificador malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_ENTREGA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @GetMapping(path = "/couriers/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Eu, como entregador desta loja", description = "Exige VER_ENTREGA.")
    public ResponseEntity<EntregadorResponse> eu(@PathVariable UUID estabelecimentoId) {
        return equipe.eu(estabelecimentoId)
                .map(e -> ResponseEntity.ok(EntregadorResponse.de(e)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apresentado"),
            @ApiResponse(responseCode = "400", description = "Nome em branco ou longo demais, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_ENTREGA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @PutMapping(path = "/couriers/me", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Me apresento à loja com um nome", description = "Exige VER_ENTREGA.")
    public EntregadorResponse apresentar(@PathVariable UUID estabelecimentoId,
                                         @RequestBody ApresentacaoRequest pedido) {
        return EntregadorResponse.de(equipe.apresentar(estabelecimentoId, pedido.nome()));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "As minhas entregas do dia, da mais nova para a mais velha"),
            @ApiResponse(responseCode = "400", description = "Identificador ou data malformados", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_ENTREGA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @GetMapping(path = "/deliveries", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "As minhas entregas", description = "Exige VER_ENTREGA. dia em AAAA-MM-DD; sem ele, hoje.")
    public List<PedidoResponse> minhas(
            @PathVariable UUID estabelecimentoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
        return entregas.doDia(estabelecimentoId, dia).stream().map(PedidoResponse::de).toList();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A entrega no status novo"),
            @ApiResponse(responseCode = "400", description = "Status que não é do entregador, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_ENTREGA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "A entrega não está comigo — inexistente, de outra loja ou de outro entregador, a mesma resposta", content = @Content),
            @ApiResponse(responseCode = "409", description = "Transição que a tabela não permite, entrega sem pagamento registrado, ou a entrega mudou enquanto você mexia", content = @Content)
    })
    @PutMapping(path = "/deliveries/{pedidoId}/status", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Saí para entregar, ou entreguei", description = "Exige VER_ENTREGA.")
    public PedidoResponse avancar(@PathVariable UUID estabelecimentoId, @PathVariable UUID pedidoId,
                                  @Valid @RequestBody StatusDaEntregaRequest pedido) {
        return PedidoResponse.de(entregas.avancar(estabelecimentoId, pedidoId, pedido.status()));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A entrega com o recebimento registrado"),
            @ApiResponse(responseCode = "400", description = "Dinheiro sem valor ou menor que o total, ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_ENTREGA, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "404", description = "A entrega não está comigo", content = @Content),
            @ApiResponse(responseCode = "409", description = "A entrega ainda não está pronta, já foi encerrada, ou mudou enquanto você mexia", content = @Content)
    })
    @PutMapping(path = "/deliveries/{pedidoId}/settlement", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registro o que recebi na porta",
            description = "Exige VER_ENTREGA. DINHEIRO, CARTAO ou PIX; o troco é calculado pelo "
                    + "servidor, e o PIX fica a conferir pela loja.")
    public PedidoResponse receber(@PathVariable UUID estabelecimentoId, @PathVariable UUID pedidoId,
                                  @Valid @RequestBody RecebimentoRequest pedido) {
        return PedidoResponse.de(entregas.registrarPagamento(estabelecimentoId, pedidoId,
                pedido.formaEfetiva(),
                pedido.valorRecebido() == null ? null : Money.de(pedido.valorRecebido())));
    }
}
