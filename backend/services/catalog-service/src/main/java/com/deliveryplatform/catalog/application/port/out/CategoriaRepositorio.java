package com.deliveryplatform.catalog.application.port.out;

import com.deliveryplatform.catalog.domain.model.Categoria;

import java.util.List;
import java.util.UUID;

/**
 * A porta de saída da categoria (ADR-064). <b>Dois métodos, e só os que alguém
 * chama</b>: criar e listar. Porta com método que ninguém chama é superfície — o
 * {@code buscarPorId} nasce com a rota que cria produto (E-3c), que é quem precisa
 * conferir se a categoria existe e é da mesma loja.
 */
public interface CategoriaRepositorio {

    Categoria salvar(Categoria categoria);

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
