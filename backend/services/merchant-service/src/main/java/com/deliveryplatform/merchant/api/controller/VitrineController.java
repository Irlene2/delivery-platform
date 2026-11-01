package com.deliveryplatform.merchant.api.controller;

import com.deliveryplatform.merchant.api.dto.SituacaoDaLojaResponse;
import com.deliveryplatform.merchant.application.port.in.ConsultarSituacaoDaLoja;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A vitrine do {@code merchant}: a primeira rota pública dele além da leitura do convite
 * (ADR-077, emenda de 01/11/2026).
 *
 * <pre>
 * GET /api/v1/storefront/{estabelecimentoId}/status
 * </pre>
 *
 * <p>Quem chama é o {@code order}, ao receber um pedido e ao montar a vitrine. O gateway
 * <b>não</b> roteia esta rota: o cliente recebe a situação junto com a taxa, numa chamada só.
 */
@RestController
@RequestMapping("/api/v1/storefront/{estabelecimentoId}")
@Tag(name = "vitrine", description = "Público, sem token: a loja vista de quem compra pelo link.")
public class VitrineController {

    private final ConsultarSituacaoDaLoja situacoes;

    public VitrineController(ConsultarSituacaoDaLoja situacoes) {
        this.situacoes = situacoes;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O nome, se está aberta agora e quando abre"),
            @ApiResponse(responseCode = "400", description = "Identificador da loja malformado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Nenhuma loja com este identificador", content = @Content)
    })
    @SecurityRequirements
    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Se a loja está recebendo pedidos agora",
            description = "Público. Compõe horário, pausa e o fuso da loja. Fechada, diz a "
                    + "próxima abertura; nula quando a loja não abre por horário.")
    public SituacaoDaLojaResponse status(@PathVariable UUID estabelecimentoId) {
        ConsultarSituacaoDaLoja.SituacaoDaLoja s = situacoes.daLoja(estabelecimentoId);
        return new SituacaoDaLojaResponse(s.nome(), s.aberta(), s.proximaAbertura().orElse(null));
    }
}
