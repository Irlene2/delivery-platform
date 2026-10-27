package com.deliveryplatform.order.application.port.out;

import java.util.Map;
import java.util.UUID;

/**
 * O cardápio publicado da loja, lido no {@code catalog} na hora do pedido (ADR-077).
 *
 * <p><b>O preço nunca vem do navegador</b> (PRD H4.2): o pedido congela o nome e o preço que
 * esta porta devolve. Produto que não está no mapa não está publicado nesta loja — ou não
 * existe, ou é de outra loja, ou é rascunho; para quem compra, os três são o mesmo.
 *
 * @throws CardapioIndisponivel quando o {@code catalog} não responde. O pedido não nasce.
 */
public interface CardapioPort {

    Map<UUID, ProdutoDoCardapio> publicadosDe(UUID estabelecimentoId);
}
