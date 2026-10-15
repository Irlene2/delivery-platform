package com.deliveryplatform.catalog.api.controller;

import com.deliveryplatform.catalog.api.dto.CategoriaRequest;
import com.deliveryplatform.catalog.api.dto.CategoriaResponse;
import com.deliveryplatform.catalog.application.port.in.CriarCategoria;
import com.deliveryplatform.catalog.application.port.in.ListarCategorias;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * As seções do cardápio (ADR-064). Controlador próprio porque o recurso é outro: o
 * {@code ProdutoController} é dono de {@code /produtos}.
 *
 * <p><b>Só criar e listar.</b> Renomear, reordenar, ativar e desativar existem no
 * agregado e não têm rota — cada um tem gatilho no {@code dividas.md}, e reordenar
 * arrasta o outbox que o {@code catalog} não tem (invariante 7).
 */
@RestController
@RequestMapping("/api/v1/merchants/{estabelecimentoId}/catalog/categorias")
public class CategoriaController {

    private final CriarCategoria criacao;
    private final ListarCategorias listagem;

    public CategoriaController(CriarCategoria criacao, ListarCategorias listagem) {
        this.criacao = criacao;
        this.listagem = listagem;
    }

    /**
     * Cria a seção. <b>201 sem {@code Location}</b>: não há {@code GET} de categoria
     * por id, e um {@code Location} para rota inexistente é o defeito que a E-1 fez com
     * {@code /me/estabelecimentos/{id}} (ADR-064 §3). O corpo é a seção criada, na forma
     * da listagem.
     *
     * <p>{@code @ResponseStatus} aqui é sobre o sucesso, e não sobre erro: o
     * {@code TratadorDeErros} explica por que não se usa a anotação lá.
     */
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "A seção criada, ativa"),
            @ApiResponse(responseCode = "400", description = "Corpo malformado — nome em branco, ordem ausente — ou identificador malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem CRIAR_PRODUTO, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma seção do cardápio",
            description = "Exige CRIAR_PRODUTO. A seção nasce ativa. Sem Location: não há "
                    + "rota de uma seção por id, e a tela precisa da lista.")
    public CategoriaResponse criar(
            @PathVariable UUID estabelecimentoId,
            @Valid @RequestBody CategoriaRequest pedido) {

        return CategoriaResponse.de(
                criacao.criar(estabelecimentoId, pedido.nome(), pedido.ordem()));
    }

    /**
     * As seções da loja, ativas e inativas, pela ordem. Loja sem seção é {@code []},
     * não 404 — é o estado de toda loja recém-cadastrada. Sem paginação, como a lista de
     * lojas do {@code merchant}; o gatilho está na ADR-064.
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "As seções, pela ordem; vazia é []"),
            @ApiResponse(responseCode = "400", description = "Identificador malformado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem VER_PRODUTO, loja inexistente ou merchant indisponível — a mesma recusa", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lista as seções do cardápio",
            description = "Exige VER_PRODUTO. Ativas e inativas, pela ordem e, no empate, "
                    + "pelo nome.")
    public List<CategoriaResponse> listar(@PathVariable UUID estabelecimentoId) {
        return listagem.daLoja(estabelecimentoId).stream()
                .map(CategoriaResponse::de)
                .toList();
    }
}
