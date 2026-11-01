package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.exception.ProdutoNaoEncontrado;
import com.deliveryplatform.catalog.application.port.in.ConsultarImagemDoProduto;
import com.deliveryplatform.catalog.application.port.in.IlustrarProduto;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.ImagemDoProdutoRepositorio;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.ImagemDoProduto;
import com.deliveryplatform.catalog.domain.model.Produto;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * A foto do produto (ADR-077, emenda de 01/11/2026): grava a imagem à parte e põe no produto a
 * referência pública dela.
 *
 * <p><b>A referência muda a cada foto</b> ({@code ?v=} com o instante): o navegador guarda a
 * imagem em cache, e uma URL igual mostraria a foto velha depois da troca.
 *
 * <p>A leitura é pública e não pergunta autorização: a foto é o que a loja mostra no cardápio.
 * Só sai a foto do produto daquela loja — o identificador da URL não é confiado.
 */
@Service
public class IlustrarProdutoService implements IlustrarProduto, ConsultarImagemDoProduto {

    private final AutorizacaoComercialPort autorizacao;
    private final ProdutoRepositorio produtos;
    private final ImagemDoProdutoRepositorio imagens;
    private final Clock relogio;

    public IlustrarProdutoService(AutorizacaoComercialPort autorizacao, ProdutoRepositorio produtos,
                                  ImagemDoProdutoRepositorio imagens, Clock relogio) {
        this.autorizacao = autorizacao;
        this.produtos = produtos;
        this.imagens = imagens;
        this.relogio = relogio;
    }

    /** A rota pública da foto — a mesma que o {@code VitrineController} serve. */
    public static String referencia(UUID estabelecimentoId, UUID produtoId, long versao) {
        return "/api/v1/storefront/" + estabelecimentoId + "/produtos/" + produtoId
                + "/imagem?v=" + versao;
    }

    @Override
    public Produto ilustrar(UUID estabelecimentoId, UUID produtoId, String tipo, byte[] conteudo) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);
        if (!contexto.pode(PermissaoDoCatalogo.ALTERAR_PRODUTO)) {
            throw new AcessoNegado();
        }

        Produto produto = produtos.buscarPorId(produtoId)
                .filter(encontrado -> encontrado.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(ProdutoNaoEncontrado::new);

        imagens.salvar(new ImagemDoProduto(produtoId, estabelecimentoId, tipo, conteudo));
        produto.ilustrarCom(referencia(estabelecimentoId, produtoId, relogio.millis()));
        return produtos.salvar(produto);
    }

    @Override
    public Optional<ImagemDoProduto> daLoja(UUID estabelecimentoId, UUID produtoId) {
        return imagens.buscar(produtoId)
                .filter(imagem -> imagem.estabelecimentoId().equals(estabelecimentoId));
    }
}
