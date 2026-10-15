package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.port.in.CriarCategoria;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.domain.model.Categoria;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cria uma seção do cardápio (ADR-064).
 *
 * <p><b>{@code CRIAR_PRODUTO}, e não uma permissão nova</b>: criar seção é montar o
 * cardápio, o mesmo ato comercial. É escolha, não leitura — nenhum documento nomeia
 * permissão de categoria —, e o gatilho para separar está na ADR-064.
 *
 * <p>A autorização vem <b>antes</b> de tocar no agregado, pelo mesmo caminho do
 * {@code ListarProdutosService}: sem contexto é 403, sem a permissão também, e o corpo
 * de quem não tem vínculo nunca chega a ser validado.
 */
@Service
public class CriarCategoriaService implements CriarCategoria {

    private final AutorizacaoComercialPort autorizacao;
    private final CategoriaRepositorio categorias;

    public CriarCategoriaService(AutorizacaoComercialPort autorizacao,
                                 CategoriaRepositorio categorias) {
        this.autorizacao = autorizacao;
        this.categorias = categorias;
    }

    @Override
    public Categoria criar(UUID estabelecimentoId, String nome, int ordem) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);

        if (!contexto.pode(PermissaoDoCatalogo.CRIAR_PRODUTO)) {
            throw new AcessoNegado();
        }

        return categorias.salvar(Categoria.nova(estabelecimentoId, nome, ordem));
    }
}
