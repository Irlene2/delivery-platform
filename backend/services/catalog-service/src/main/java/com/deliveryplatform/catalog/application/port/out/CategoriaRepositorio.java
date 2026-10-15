package com.deliveryplatform.catalog.application.port.out;

import com.deliveryplatform.catalog.domain.model.Categoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A porta de saída da categoria (ADR-064). <b>Três métodos, e só os que alguém
 * chama</b>. A ADR-064 nasceu com dois — <i>"porta com método que ninguém chama é
 * superfície"</i> —, e o {@code buscarPorId} veio na E-3c com o chamador: o caso de uso
 * que cria produto, que confere se a categoria existe e é da mesma loja (ADR-066).
 */
public interface CategoriaRepositorio {

    Categoria salvar(Categoria categoria);

    /**
     * A categoria, de qualquer loja. <b>Quem confere a loja é o chamador</b>, como o
     * {@code ProdutoRepositorio.buscarPorId} — a forma do vizinho.
     */
    Optional<Categoria> buscarPorId(UUID id);

    /**
     * As categorias da loja, ativas e inativas, pela {@code ordem} — e, no empate,
     * pelo nome e pelo id. Sem o desempate, duas seções com a mesma ordem trocariam de
     * lugar entre recargas, e a pessoa clicaria na errada.
     *
     * <p><b>Sem paginação</b>, como a lista de lojas do {@code merchant}: uma loja tem
     * dezenas de seções, não milhares. O gatilho está na ADR-064.
     */
    List<Categoria> daLoja(UUID estabelecimentoId);
}
