package com.deliveryplatform.order.api.controller;

import com.deliveryplatform.order.api.dto.FechamentoRequest;
import com.deliveryplatform.order.api.dto.FechamentoResponse;
import com.deliveryplatform.order.application.port.in.FecharODia;
import com.deliveryplatform.order.config.OpenApiConfig;
import com.deliveryplatform.order.domain.model.FechamentoDoDia;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O caixa do dia, fechado e guardado (ADR-077, emenda de 01/11/2026). Fechar exige
 * {@code GERENCIAR_JORNADA}; ver exige {@code VER_VENDAS}. O gateway já roteia
 * {@code /orders/**} para este serviço.
 */
@RestController
@RequestMapping("/api/v1/merchants/{estabelecimentoId}/orders/closings")
@Tag(name = "fechamento", description = "O caixa do dia: fechar, conferir e consultar o histórico.")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA)
public class FechamentoController {

    private final FecharODia caixa;

    public FechamentoController(FecharODia caixa) {
        this.caixa = caixa;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "O caixa fechado, apurado no servidor"),
            @ApiResponse(responseCode = "400", description = "Dia futuro, contagem negativa ou corpo malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo ou sem GERENCIAR_JORNADA — a mesma recusa", content = @Content),
            @ApiResponse(responseCode = "409", description = "Há pedido do dia em andamento, ou o dia já foi fechado", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fecha o caixa do dia",
            description = "Apura vendas, formas de pagamento e o acerto de cada entregador a "
                    + "partir dos pedidos do dia. Depois de fechado, os pedidos daquele dia não "
                    + "mudam mais e a loja não recebe pedido novo naquele dia.")
    public ResponseEntity<FechamentoResponse> fechar(@PathVariable UUID estabelecimentoId,
                                                     @Valid @RequestBody FechamentoRequest pedido) {
        var fechado = caixa.fechar(estabelecimentoId, pedido.dia(),
                pedido.dinheiroContado() == null ? null : Money.de(pedido.dinheiroContado()),
                pedido.observacao());
        return ResponseEntity
                .created(URI.create("/api/v1/merchants/" + estabelecimentoId + "/orders/closings/"
                        + fechado.getDia()))
                .body(FechamentoResponse.de(fechado));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Os fechamentos mais recentes primeiro"),
            @ApiResponse(responseCode = "400", description = "Período invertido, incompleto ou maior que um ano", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo ou sem VER_VENDAS", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "O histórico dos fechamentos",
            description = "Sem período, os mais recentes. Com `de` e `ate` (datas ISO, inclusive), "
                    + "os fechamentos do período, de até um ano — as vendas de um período.")
    public List<FechamentoResponse> historico(
            @PathVariable UUID estabelecimentoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        List<FechamentoDoDia> lidos =
                de == null && ate == null
                        ? caixa.recentes(estabelecimentoId)
                        : caixa.doPeriodo(estabelecimentoId, de, ate);
        return lidos.stream().map(FechamentoResponse::de).toList();
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O fechamento do dia"),
            @ApiResponse(responseCode = "400", description = "Data malformada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo ou sem VER_VENDAS", content = @Content),
            @ApiResponse(responseCode = "404", description = "O dia ainda não foi fechado", content = @Content)
    })
    @GetMapping(path = "/{dia}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "O fechamento de um dia")
    public FechamentoResponse doDia(@PathVariable UUID estabelecimentoId,
                                    @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {
        return FechamentoResponse.de(caixa.doDia(estabelecimentoId, dia));
    }
}
