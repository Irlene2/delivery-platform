package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.port.in.CriarProduto;
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
 * O produto nasce rascunho (ADR-065), e a seção que ele cita tem de existir nesta loja
 * (ADR-066).
 *
 * <h2>A ordem é parte da regra</h2>
 *
 * <ol>
 *   <li><b>permissão</b> — {@code CRIAR_PRODUTO}, pelo caminho do
 *       {@code ListarProdutosService}. Quem não tem vínculo não chega a ter o corpo lido;</li>
 *   <li><b>a categoria existe e é desta loja</b> — antes de construir o agregado, para que a
 *       recusa de referência não dependa de o resto do corpo estar bom. A regra é daqui e
 *       não do {@code Produto}: agregado não valida agregado, como o javadoc dele diz desde
 *       a G-B;</li>
 *   <li>{@code Produto.rascunho(...)}, que cobra o que é dele — o nome, o preço negativo
 *       (C1 na construção);</li>
 *   <li>gravar.</li>
 * </ol>
 *
 * <p><b>Inexistente e de outra loja são a mesma recusa</b>, com a mesma frase (M7): uma
 * resposta que os distinguisse contaria, a quem tentasse ids no escuro, quais seções
 * existem no sistema. E é <b>400</b>, não 404: a categoria vem no corpo, e 404 é de
 * identificador que está na URL (ADR-066 §2).
 *
 * <p>A regra vale <b>na escrita</b>. O semeador grava pelo repositório, sem passar por
 * aqui — e cria a categoria antes do produto desde a E-3b.
 */
@Service
public class CriarProdutoService implements CriarProduto {

    private final AutorizacaoComercialPort autorizacao;
    private final CategoriaRepositorio categorias;
    private final ProdutoRepositorio produtos;

    public CriarProdutoService(AutorizacaoComercialPort autorizacao,
                               CategoriaRepositorio categorias,
                               ProdutoRepositorio produtos) {
        this.autorizacao = autorizacao;
        this.categorias = categorias;
        this.produtos = produtos;
    }

    @Override
    public Produto criar(UUID estabelecimentoId, Pedido pedido) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);
        if (!contexto.pode(PermissaoDoCatalogo.CRIAR_PRODUTO)) {
            throw new AcessoNegado();
        }

        categorias.buscarPorId(pedido.categoriaId())
                .filter(categoria -> categoria.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(() -> new RegraDoCatalogoViolada(
                        RegraDoCatalogoViolada.Motivo.CATEGORIA_NAO_ENCONTRADA,
                        "a categoria do pedido não existe nesta loja"));

        Produto produto = Produto.rascunho(estabelecimentoId, pedido.categoriaId(), pedido.nome(),
                pedido.precoBase(), pedido.modoDeControle(), pedido.ordem());
        produto.descreverCom(pedido.descricao());

        return produtos.salvar(produto);
    }
}
