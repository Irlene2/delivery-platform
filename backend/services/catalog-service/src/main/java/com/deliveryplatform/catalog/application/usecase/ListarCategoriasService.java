package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.port.in.ListarCategorias;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.domain.model.Categoria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * As seções da loja, ativas e inativas: quem monta o cardápio precisa ver a seção
 * desativada para poder reativá-la. {@code VER_PRODUTO} — quem vê o cardápio vê as
 * seções dele (ADR-064). A forma do {@code ListarProdutosService}.
 */
@Service
public class ListarCategoriasService implements ListarCategorias {

    private final AutorizacaoComercialPort autorizacao;
    private final CategoriaRepositorio categorias;

    public ListarCategoriasService(AutorizacaoComercialPort autorizacao,
                                   CategoriaRepositorio categorias) {
        this.autorizacao = autorizacao;
        this.categorias = categorias;
    }

    @Override
    public List<Categoria> daLoja(UUID estabelecimentoId) {
        ContextoDeAcesso contexto = autorizacao
                .contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);

        if (!contexto.pode(PermissaoDoCatalogo.VER_PRODUTO)) {
            throw new AcessoNegado();
        }

        return categorias.daLoja(estabelecimentoId);
    }
}
