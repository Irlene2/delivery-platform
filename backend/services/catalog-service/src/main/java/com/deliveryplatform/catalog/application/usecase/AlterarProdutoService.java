package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.exception.ProdutoNaoEncontrado;
import com.deliveryplatform.catalog.application.port.in.AlterarProduto;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.exception.RegraDoCatalogoViolada;
import com.deliveryplatform.catalog.domain.model.Produto;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Corrige o cadastro de um produto (ADR-077, emenda de 01/11/2026). A ordem é a da criação e
 * da publicação juntas: permissão ({@code ALTERAR_PRODUTO}), o produto <b>desta</b> loja (de
 * outra é o mesmo 404 de inexistente, ADR-056), a seção do corpo desta loja (ADR-066), e então
 * o agregado — que cobra nome, preço e, se publicado, C1 e C2.
 */
@Service
public class AlterarProdutoService implements AlterarProduto {

    private final AutorizacaoComercialPort autorizacao;
    private final CategoriaRepositorio categorias;
    private final ProdutoRepositorio produtos;

    public AlterarProdutoService(AutorizacaoComercialPort autorizacao,
                                 CategoriaRepositorio categorias,
                                 ProdutoRepositorio produtos) {
        this.autorizacao = autorizacao;
        this.categorias = categorias;
        this.produtos = produtos;
    }

    @Override
    public Produto alterar(UUID estabelecimentoId, UUID produtoId, Alteracao alteracao) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);
        if (!contexto.pode(PermissaoDoCatalogo.ALTERAR_PRODUTO)) {
            throw new AcessoNegado();
        }

        Produto produto = produtos.buscarPorId(produtoId)
                .filter(encontrado -> encontrado.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(ProdutoNaoEncontrado::new);

        categorias.buscarPorId(alteracao.categoriaId())
                .filter(categoria -> categoria.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(() -> new RegraDoCatalogoViolada(
                        RegraDoCatalogoViolada.Motivo.CATEGORIA_NAO_ENCONTRADA,
                        "a categoria da alteração não existe nesta loja"));

        produto.alterarCadastro(alteracao.categoriaId(), alteracao.nome(), alteracao.precoBase(),
                alteracao.descricao());
        if (alteracao.modoDeControle() != null) {
            produto.trocarControle(alteracao.modoDeControle());
        }
        if (alteracao.selo() != null) {
            produto.rotular(alteracao.selo());
        }

        return produtos.salvar(produto);
    }
}
