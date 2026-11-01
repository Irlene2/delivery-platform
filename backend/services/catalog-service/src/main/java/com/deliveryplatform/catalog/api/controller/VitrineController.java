package com.deliveryplatform.catalog.api.controller;

import com.deliveryplatform.catalog.api.dto.CardapioPublicoResponse;
import com.deliveryplatform.catalog.application.port.in.ConsultarCardapioPublico;
import com.deliveryplatform.catalog.application.port.in.ConsultarImagemDoProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * As rotas públicas do {@code catalog} (ADR-077): o cardápio pelo link que a loja manda ao
 * cliente e, desde 01/11/2026, a foto de cada produto. Liberadas no {@code SecurityConfig} e no
 * gateway, e só elas.
 */
@RestController
@Tag(name = "vitrine", description = "Público, sem token: o cardápio pelo link da loja.")
public class VitrineController {

    private final ConsultarCardapioPublico cardapio;
    private final ConsultarImagemDoProduto imagens;

    public VitrineController(ConsultarCardapioPublico cardapio, ConsultarImagemDoProduto imagens) {
        this.cardapio = cardapio;
        this.imagens = imagens;
    }

    /**
     * A foto do produto (ADR-077, emenda de 01/11/2026). Pública como o cardápio, e com cache
     * longo: a referência no produto muda a cada foto nova ({@code ?v=}), então a URL antiga
     * nunca precisa ser revalidada.
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A foto, no tipo em que foi enviada"),
            @ApiResponse(responseCode = "400", description = "Identificador malformado", content = @Content),
            @ApiResponse(responseCode = "404", description = "O produto não tem foto, ou não é desta loja", content = @Content)
    })
    // Sem `produces`: com ele, a recusa por identificador malformado não teria como sair
    // como ProblemDetail (o tipo de resposta ficaria preso aos de imagem). O tipo vem da foto.
    @GetMapping(path = "/api/v1/storefront/{estabelecimentoId}/produtos/{produtoId}/imagem")
    @SecurityRequirements
    @Operation(summary = "A foto do produto", description = "Sem token.")
    public ResponseEntity<byte[]> imagem(@PathVariable UUID estabelecimentoId,
                                         @PathVariable UUID produtoId) {
        return imagens.daLoja(estabelecimentoId, produtoId)
                .map(i -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(i.tipo()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                        .body(i.conteudo()))
                .orElseGet(() -> ResponseEntity.notFound().build());
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
