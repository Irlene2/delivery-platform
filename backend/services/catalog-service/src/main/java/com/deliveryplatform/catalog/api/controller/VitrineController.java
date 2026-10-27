package com.deliveryplatform.catalog.api.controller;

import com.deliveryplatform.catalog.api.dto.CardapioPublicoResponse;
import com.deliveryplatform.catalog.application.port.in.ConsultarCardapioPublico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A primeira rota pública do {@code catalog} (ADR-077): o cardápio pelo link que a loja manda
 * ao cliente. Liberada no {@code SecurityConfig} e no gateway, e só ela.
 */
@RestController
@Tag(name = "vitrine", description = "Público, sem token: o cardápio pelo link da loja.")
public class VitrineController {

    private final ConsultarCardapioPublico cardapio;

    public VitrineController(ConsultarCardapioPublico cardapio) {
        this.cardapio = cardapio;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "As seções ativas e os produtos publicados; vazio quando a loja não tem nada publicado"),
            @ApiResponse(responseCode = "400", description = "Identificador da loja malformado", content = @Content)
    })
    @GetMapping(path = "/api/v1/storefront/{estabelecimentoId}/menu",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(summary = "O cardápio público da loja",
            description = "Sem token. Só produto publicado, em seção ativa, pela ordem. Sem "
                    + "rascunho, sem inativo, sem grupos de opções. Loja inexistente responde "
                    + "o cardápio vazio: o catalog não conhece lojas.")
    public CardapioPublicoResponse cardapio(@PathVariable UUID estabelecimentoId) {
        return CardapioPublicoResponse.de(cardapio.daLoja(estabelecimentoId));
    }
}
