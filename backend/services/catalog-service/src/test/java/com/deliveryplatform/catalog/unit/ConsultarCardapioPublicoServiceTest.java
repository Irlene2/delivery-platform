package com.deliveryplatform.catalog.unit;

import com.deliveryplatform.catalog.application.port.in.ConsultarCardapioPublico.Cardapio;
import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.VitrineRepositorio;
import com.deliveryplatform.catalog.application.usecase.ConsultarCardapioPublicoService;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.Precos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** O cardápio público (ADR-077): só seção ativa, e dentro dela só o que está publicado. */
class ConsultarCardapioPublicoServiceTest {

    static final UUID LOJA = UUID.randomUUID();

    final List<Categoria> categorias = new ArrayList<>();
    final List<Produto> publicados = new ArrayList<>();

    final ConsultarCardapioPublicoService servico = new ConsultarCardapioPublicoService(
            new CategoriaRepositorio() {
                @Override
                public Categoria salvar(Categoria categoria) {
                    categorias.add(categoria);
                    return categoria;
                }

                @Override
                public Optional<Categoria> buscarPorId(UUID id) {
                    return categorias.stream().filter(c -> c.getId().equals(id)).findFirst();
                }

                @Override
                public List<Categoria> daLoja(UUID estabelecimentoId) {
                    return List.copyOf(categorias);
                }
            },
            (VitrineRepositorio) (estabelecimentoId, limite) -> List.copyOf(publicados));

    Produto publicado(Categoria secao, String nome) {
        Produto produto = Produto.rascunho(LOJA, secao.getId(), nome, Precos.reais("10.00"),
                ModoDeControle.SEM_CONTROLE, 0);
        produto.publicar();
        publicados.add(produto);
        return produto;
    }

    @Test
    @DisplayName("produto publicado em seção ativa aparece")
    void publicado_em_secao_ativa() {
        Categoria lanches = Categoria.nova(LOJA, "Lanches", 0);
        categorias.add(lanches);
        Produto xBurguer = publicado(lanches, "X-Burguer");

        Cardapio cardapio = servico.daLoja(LOJA);

        assertThat(cardapio.secoes()).containsExactly(lanches);
        assertThat(cardapio.produtos()).containsExactly(xBurguer);
    }

    @Test
    @DisplayName("seção desativada some do cardápio, e os produtos dela junto")
    void secao_desativada_some() {
        Categoria sorvetes = Categoria.nova(LOJA, "Sorvetes", 1);
        sorvetes.desativar();
        categorias.add(sorvetes);
        publicado(sorvetes, "Picolé");

        Cardapio cardapio = servico.daLoja(LOJA);

        assertThat(cardapio.secoes()).isEmpty();
        assertThat(cardapio.produtos()).isEmpty();
    }

    @Test
    @DisplayName("loja sem nada: cardápio vazio, e não erro")
    void loja_sem_nada() {
        Cardapio cardapio = servico.daLoja(UUID.randomUUID());

        assertThat(cardapio.secoes()).isEmpty();
        assertThat(cardapio.produtos()).isEmpty();
    }
}
