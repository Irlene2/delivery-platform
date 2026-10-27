package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.Produto;

import java.util.List;
import java.util.UUID;

/**
 * O cardápio de quem compra, sem token — a porta que a ADR-018 reservou e que a ADR-077 abre
 * para a entrega de 15/11.
 *
 * <p>Loja que não existe responde com o cardápio vazio, e não 404: o {@code catalog} não
 * conhece lojas, só produtos de uma loja, e o {@code merchant} não tem rota pública que diga
 * se uma loja existe. Gatilho escrito: essa rota existir.
 */
public interface ConsultarCardapioPublico {

    Cardapio daLoja(UUID estabelecimentoId);

    /** As seções ativas, e os produtos publicados que estão nelas. */
    record Cardapio(List<Categoria> secoes, List<Produto> produtos) {
    }
}
