package com.deliveryplatform.catalog.application.usecase;

import com.deliveryplatform.catalog.application.port.in.ConsultarCardapioPublico;
import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.VitrineRepositorio;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.Produto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Monta o cardápio público: seções ativas e, dentro delas, os produtos publicados.
 *
 * <p><b>Sem autorização, e é a decisão</b> (ADR-077): o cardápio é o que a loja quer que
 * qualquer um veja. Rascunho e inativo nunca saem daqui.
 *
 * <p>Produto publicado numa seção desativada <b>não aparece</b> — é o "efeito de montagem do
 * cardápio" que a {@link Categoria} descreve. Ele continua {@code vendavel()} pelo domínio, mas
 * o pedido da entrega de 15/11 lê os preços deste mesmo cardápio, então na prática não se
 * compra. Se a loja quiser vender, reativa a seção.
 *
 * <h2>Teto</h2>
 *
 * <p>{@link #TETO} produtos. Um cardápio de loja pequena tem dezenas; o teto existe para que a
 * rota pública nunca carregue a coleção inteira, como o {@code max-page-size} faz no painel.
 * Sem paginação: a tela do cliente mostra o cardápio inteiro de uma vez.
 */
@Service
public class ConsultarCardapioPublicoService implements ConsultarCardapioPublico {

    static final int TETO = 300;

    private final CategoriaRepositorio categorias;
    private final VitrineRepositorio vitrine;

    public ConsultarCardapioPublicoService(CategoriaRepositorio categorias,
                                           VitrineRepositorio vitrine) {
        this.categorias = categorias;
        this.vitrine = vitrine;
    }

    @Override
    public Cardapio daLoja(UUID estabelecimentoId) {
        List<Categoria> ativas = categorias.daLoja(estabelecimentoId).stream()
                .filter(Categoria::isAtiva)
                .toList();
        Set<UUID> idsAtivos = ativas.stream().map(Categoria::getId).collect(Collectors.toSet());

        List<Produto> produtos = vitrine.publicadosDaLoja(estabelecimentoId, TETO).stream()
                .filter(p -> idsAtivos.contains(p.getCategoriaId()))
                .toList();

        return new Cardapio(ativas, produtos);
    }
}
