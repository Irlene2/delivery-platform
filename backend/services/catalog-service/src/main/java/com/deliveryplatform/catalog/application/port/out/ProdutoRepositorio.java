package com.deliveryplatform.catalog.application.port.out;

import com.deliveryplatform.catalog.domain.model.Produto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A porta de saída do produto. Três métodos, e nenhum a mais.
 *
 * <p>Não há {@code apagar}: a C9 diz que produto nunca é apagado, e a forma
 * mais barata de garantir isso é não existir o método. Produto que sai do
 * cardápio vai a {@code INATIVO} — o pedido antigo continua reimprimível
 * (ADR-018), e um produto apagado tornaria um pedido de junho ilegível em
 * julho.
 *
 * <p>Não há {@code atualizar} separado de {@code salvar}: a raiz é gravada
 * inteira, que é o que a §7 quer dizer com <i>"árvore lida inteira, gravada
 * inteira"</i>. Gravar pedaço de agregado é como um invariante do agregado
 * deixa de valer sem ninguém perceber.
 */
public interface ProdutoRepositorio {

    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(UUID id);

    /**
     * Os produtos publicados de uma loja — a consulta que o primeiro índice da
     * §7 existe para servir.
     *
     * <p>Devolve os {@code ATIVO}, e não os <i>vendáveis</i>: vendável é
     * derivado (§5) e não é campo, então filtrar por ele é trabalho de quem
     * chamou, sobre o que voltou daqui.
     */
    List<Produto> publicadosDe(UUID estabelecimentoId);
}
