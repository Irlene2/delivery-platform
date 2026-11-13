package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.exception.ProdutoNaoEncontrado;
import com.deliveryplatform.catalog.application.port.in.ExcluirProduto;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.ImagemDoProdutoRepositorio;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.Produto;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * O produto sai do cadastro (13/11/2026). A ordem é a das outras escritas: permissão
 * ({@code DESATIVAR_PRODUTO}), o produto <b>desta</b> loja — de outra loja é o mesmo 404 de
 * inexistente (ADR-056) —, e só então apaga. A foto sai primeiro: se o segundo passo falhar,
 * sobra um produto sem foto, que a tela mostra com a imagem padrão, e não uma foto sem dono.
 */
@Service
public class ExcluirProdutoService implements ExcluirProduto {

    private final AutorizacaoComercialPort autorizacao;
    private final ProdutoRepositorio produtos;
    private final ImagemDoProdutoRepositorio imagens;

    public ExcluirProdutoService(AutorizacaoComercialPort autorizacao,
                                 ProdutoRepositorio produtos,
                                 ImagemDoProdutoRepositorio imagens) {
        this.autorizacao = autorizacao;
        this.produtos = produtos;
        this.imagens = imagens;
    }

    @Override
    public void excluir(UUID estabelecimentoId, UUID produtoId) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);
        if (!contexto.pode(PermissaoDoCatalogo.DESATIVAR_PRODUTO)) {
            throw new AcessoNegado();
        }

        Produto produto = produtos.buscarPorId(produtoId)
                .filter(encontrado -> encontrado.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(ProdutoNaoEncontrado::new);

        imagens.excluir(produto.getId());
        produtos.excluir(produto.getId());
    }
}
