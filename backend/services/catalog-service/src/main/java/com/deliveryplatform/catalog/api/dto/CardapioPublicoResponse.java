package com.deliveryplatform.catalog.api.dto;

import com.deliveryplatform.catalog.application.port.in.ConsultarCardapioPublico;
import com.deliveryplatform.catalog.domain.model.EstadoDeDisponibilidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * O cardápio de quem compra. Sem estado de publicação (tudo aqui está publicado), sem versão
 * e sem grupos de opções — o pedido da entrega de 15/11 leva o produto sem as opções
 * ({@code docs/entrega-15-11/pedido.md} §6).
 */
@Schema(description = "O cardápio público da loja: seções ativas e produtos publicados.")
public record CardapioPublicoResponse(List<Secao> categorias, List<ProdutoNoCardapio> produtos) {

    public record Secao(UUID id, String nome, int ordem) {
    }

    public record ProdutoNoCardapio(
            UUID id,
            UUID categoriaId,
            String nome,
            String descricao,
            @Schema(description = "Em reais, duas casas. É o preço que o pedido vai cobrar.",
                    example = "49.90")
            BigDecimal precoBase,
            int ordem,
            EstadoDeDisponibilidade disponibilidade,
            @Schema(description = "Falso quando acabou: aparece, mas não se pede.")
            boolean vendavel,
            @Schema(description = "O caminho público da foto, relativo ao gateway; nulo sem foto.",
                    nullable = true)
            String imagem,
            @Schema(description = "O selo da vitrine, como \"Mais pedido\"; nulo sem selo.",
                    nullable = true)
            String selo) {
    }

    public static CardapioPublicoResponse de(ConsultarCardapioPublico.Cardapio cardapio) {
        return new CardapioPublicoResponse(
                cardapio.secoes().stream()
                        .map(c -> new Secao(c.getId(), c.getNome(), c.getOrdem()))
                        .toList(),
                cardapio.produtos().stream()
                        .map(p -> new ProdutoNoCardapio(p.getId(), p.getCategoriaId(), p.getNome(),
                                p.getDescricao(), p.getPrecoBase().valor(), p.getOrdem(),
                                p.getDisponibilidade().estado(), p.vendavel(),
                                p.getImagemRef(), p.getSelo()))
                        .toList());
    }
}
