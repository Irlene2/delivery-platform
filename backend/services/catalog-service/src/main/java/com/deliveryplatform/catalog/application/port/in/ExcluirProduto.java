package com.deliveryplatform.catalog.application.port.in;

import java.util.UUID;

/**
 * Apaga um produto do cadastro da loja, com a foto dele (13/11/2026). Exige
 * {@code DESATIVAR_PRODUTO}.
 *
 * <p><b>Apagar não mexe em pedido nenhum.</b> O pedido congela nome e preço do item quando é
 * feito (ADR-018), e não volta ao catálogo para lê-los: o histórico, o acompanhamento e o caixa
 * do dia continuam inteiros depois que o produto sai.
 */
public interface ExcluirProduto {

    void excluir(UUID estabelecimentoId, UUID produtoId);
}
