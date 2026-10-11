package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.exception.ProdutoNaoEncontrado;
import com.deliveryplatform.catalog.application.port.in.PublicarProduto;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.Produto;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * O produto vai ao cardápio (ADR-065). {@code publicar()} cobra C1, C5, C4 e C2 e é
 * idempotente desde a G-B — <b>este é o primeiro chamador que não é teste</b>.
 *
 * <p>A ordem é a da marcação: permissão ({@code ALTERAR_PRODUTO} — escolha declarada na
 * ADR-065 §4, porque nenhum documento nomeia permissão de publicação), busca o produto
 * <b>desta</b> loja — de outra loja é o mesmo 404 de inexistente (ADR-056) —, publica,
 * grava. <b>Publicar o que já está publicado não é erro</b>: o agregado não faz nada, e
 * a rota responde 200 com o estado que já vale (ADR-065 §5). Um {@code INATIVO} volta ao
 * cardápio pelo mesmo ato, e a volta também passa pelas invariantes.
 *
 * <p><b>O evento não sai.</b> O {@code catalogo.md} §8 declara {@code ProdutoPublicadoV1}
 * na transição {@code RASCUNHO → ATIVO}, com o {@code conversation} como consumidor —
 * e o {@code catalog} não tem outbox, e o consumidor não tem código. É a mesma situação
 * do {@code DisponibilidadeAlteradaV1} na marcação, e a mesma decisão: adiar.
 * <b>Gatilho escrito:</b> o {@code conversation} ganhar código, que é quem o consome.
 */
@Service
public class PublicarProdutoService implements PublicarProduto {

    private final AutorizacaoComercialPort autorizacao;
    private final ProdutoRepositorio produtos;

    public PublicarProdutoService(AutorizacaoComercialPort autorizacao,
                                  ProdutoRepositorio produtos) {
        this.autorizacao = autorizacao;
        this.produtos = produtos;
    }

    @Override
    public Produto publicar(UUID estabelecimentoId, UUID produtoId) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);
        if (!contexto.pode(PermissaoDoCatalogo.ALTERAR_PRODUTO)) {
            throw new AcessoNegado();
        }

        Produto produto = produtos.buscarPorId(produtoId)
                .filter(encontrado -> encontrado.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(ProdutoNaoEncontrado::new);

        produto.publicar();

        return produtos.salvar(produto);
    }
}
