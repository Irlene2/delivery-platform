package com.deliveryplatform.catalog.application.port.out;

import com.deliveryplatform.catalog.domain.model.Produto;

import java.util.List;
import java.util.UUID;

/**
 * A leitura do cardápio público (ADR-077): só produto {@code ATIVO}, na ordem da lista.
 *
 * <p>Porta separada do {@link ProdutoRepositorio}, e não um método a mais nele, para que os
 * dublês de teste que já o implementam não precisem mudar por uma leitura que não usam.
 */
public interface VitrineRepositorio {

    /** Os produtos publicados da loja, pela ordem e depois pelo id, até {@code limite}. */
    List<Produto> publicadosDaLoja(UUID estabelecimentoId, int limite);
}
