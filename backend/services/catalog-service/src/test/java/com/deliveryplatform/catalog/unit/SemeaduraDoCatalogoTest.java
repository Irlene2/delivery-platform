package com.deliveryplatform.catalog.unit;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.config.SemeaduraProperties;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.EstadoDeDisponibilidade;
import com.deliveryplatform.catalog.domain.model.EstadoDePublicacao;
import com.deliveryplatform.catalog.domain.model.Opcao;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.infrastructure.semeadura.SemeaduraDoCatalogo;
import com.deliveryplatform.catalog.support.ProdutoDeTeste;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O produto da fixture (ADR-059): publicado, esgotado com carimbo velho, e com
 * uma opção esgotada também — os dois caminhos da reativação. E, desde 01/11/2026, o
 * cardápio de demonstração, idempotente por seção.
 */
@ExtendWith(MockitoExtension.class)
class SemeaduraDoCatalogoTest {

    private static final UUID LOJA = UUID.fromString("5eed0000-0000-4000-8000-000000000001");
    private static final Instant AGORA = Instant.parse("2026-10-10T05:30:00Z");

    @Mock
    CategoriaRepositorio categorias;

    @Mock
    ProdutoRepositorio produtos;

    @Test
    void desligada_nao_toca_no_repositorio() {
        semeadura(false).run(new DefaultApplicationArguments());

        verifyNoInteractions(categorias, produtos);
    }

    @Test
    void grava_um_produto_publicado_e_esgotado_que_a_proxima_abertura_reativa() {
        when(produtos.daLoja(eq(LOJA), any())).thenReturn(Page.empty());
        when(categorias.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(produtos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        semeadura(true).run(new DefaultApplicationArguments());

        ArgumentCaptor<Categoria> secoes = ArgumentCaptor.forClass(Categoria.class);
        verify(categorias, atLeastOnce()).salvar(secoes.capture());
        ArgumentCaptor<Produto> gravados = ArgumentCaptor.forClass(Produto.class);
        verify(produtos, atLeastOnce()).salvar(gravados.capture());
        // A margherita é a primeira gravação, na primeira seção; o cardápio vem depois.
        Categoria secao = secoes.getAllValues().get(0);
        Produto produto = gravados.getAllValues().get(0);

        assertThat(secao.getNome()).isEqualTo("Pizzas");
        assertThat(secao.getEstabelecimentoId()).isEqualTo(LOJA);
        assertThat(secao.isAtiva()).isTrue();
        assertThat(produto.getNome()).isEqualTo("Pizza margherita");
        assertThat(produto.getCategoriaId())
                .as("até a E-3b era uma constante sem linha; agora é a categoria gravada")
                .isEqualTo(secao.getId());

        assertThat(produto.getEstabelecimentoId()).isEqualTo(LOJA);
        assertThat(produto.getEstadoDePublicacao()).isEqualTo(EstadoDePublicacao.ATIVO);
        assertThat(produto.getDisponibilidade().estado()).isEqualTo(EstadoDeDisponibilidade.ESGOTADO_HOJE);
        assertThat(produto.getDisponibilidade().expedienteDeReferencia()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(produto.vendavel()).isFalse();

        List<Opcao> opcoes = produto.getGruposDeOpcoes().get(0).opcoes();
        assertThat(opcoes).extracting(o -> o.disponibilidade().estado()).containsExactly(
                EstadoDeDisponibilidade.DISPONIVEL,
                EstadoDeDisponibilidade.DISPONIVEL,
                EstadoDeDisponibilidade.ESGOTADO_HOJE);

        // Trinta dias atrás é menor que o expediente que abrir, em qualquer fuso.
        LocalDate ontemEmQualquerLugar = LocalDate.of(2026, 10, 9);
        assertThat(produto.getDisponibilidade().deveReativarNoExpediente(ontemEmQualquerLugar)).isTrue();
        assertThat(opcoes.get(2).disponibilidade().deveReativarNoExpediente(ontemEmQualquerLugar)).isTrue();
    }

    @Test
    void grava_o_cardapio_de_demonstracao_publicado_com_descricao_foto_e_opcoes() {
        when(produtos.daLoja(eq(LOJA), any())).thenReturn(Page.empty());
        when(categorias.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(produtos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        semeadura(true).run(new DefaultApplicationArguments());

        ArgumentCaptor<Categoria> secoes = ArgumentCaptor.forClass(Categoria.class);
        verify(categorias, times(7)).salvar(secoes.capture());
        assertThat(secoes.getAllValues()).extracting(Categoria::getNome).containsExactly(
                "Pizzas", "Hambúrgueres", "Porções", "Hot Dogs", "Combos", "Bebidas", "Sobremesas");

        ArgumentCaptor<Produto> gravados = ArgumentCaptor.forClass(Produto.class);
        verify(produtos, times(25)).salvar(gravados.capture());
        List<Produto> cardapio = gravados.getAllValues().subList(1, 25);
        assertThat(cardapio).allSatisfy(produto -> {
            assertThat(produto.getEstabelecimentoId()).isEqualTo(LOJA);
            assertThat(produto.getEstadoDePublicacao()).isEqualTo(EstadoDePublicacao.ATIVO);
            assertThat(produto.vendavel()).isTrue();
            assertThat(produto.getDescricao()).isNotBlank();
        });
        // Só fotos de licença livre; as outras ficam sem foto.
        assertThat(cardapio).extracting(Produto::getImagemRef).filteredOn(ref -> ref != null)
                .hasSize(20)
                .allSatisfy(ref -> assertThat(ref).matches(
                        "https://(images\\.unsplash\\.com|upload\\.wikimedia\\.org)/.*"));

        Produto classico = cardapio.get(0);
        assertThat(classico.getNome()).isEqualTo("Aki Classic");
        assertThat(classico.getCategoriaId()).isEqualTo(secoes.getAllValues().get(1).getId());
        assertThat(classico.getGruposDeOpcoes()).extracting(g -> g.nome())
                .containsExactly("Ponto da carne", "Adicionais");
    }

    @Test
    void loja_que_ja_tem_produto_nao_ganha_outra_margherita_mas_ganha_as_secoes_que_faltam() {
        when(produtos.daLoja(eq(LOJA), any()))
                .thenReturn(new PageImpl<>(List.of(ProdutoDeTeste.margheritaPublicada())));
        when(categorias.daLoja(LOJA)).thenReturn(List.of(
                Categoria.nova(LOJA, "Pizzas", 0),
                Categoria.nova(LOJA, "Hambúrgueres", 1),
                Categoria.nova(LOJA, "Porções", 2),
                Categoria.nova(LOJA, "Hot Dogs", 3),
                Categoria.nova(LOJA, "Combos", 4),
                Categoria.nova(LOJA, "Bebidas", 5)));
        when(categorias.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(produtos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        semeadura(true).run(new DefaultApplicationArguments());

        ArgumentCaptor<Categoria> secao = ArgumentCaptor.forClass(Categoria.class);
        verify(categorias).salvar(secao.capture());
        assertThat(secao.getValue().getNome()).isEqualTo("Sobremesas");
        ArgumentCaptor<Produto> gravados = ArgumentCaptor.forClass(Produto.class);
        verify(produtos, times(4)).salvar(gravados.capture());
        assertThat(gravados.getAllValues()).extracting(Produto::getNome)
                .doesNotContain("Pizza margherita")
                .contains("Pudim de Leite");
    }

    @Test
    void loja_com_o_cardapio_inteiro_nao_ganha_nada() {
        when(produtos.daLoja(eq(LOJA), any()))
                .thenReturn(new PageImpl<>(List.of(ProdutoDeTeste.margheritaPublicada())));
        when(categorias.daLoja(LOJA)).thenReturn(List.of(
                "Pizzas", "Hambúrgueres", "Porções", "Hot Dogs", "Combos", "Bebidas", "Sobremesas")
                .stream().map(nome -> Categoria.nova(LOJA, nome, 0)).toList());

        semeadura(true).run(new DefaultApplicationArguments());

        verify(produtos, never()).salvar(any());
        verify(categorias, never()).salvar(any());
    }

    private SemeaduraDoCatalogo semeadura(boolean ligada) {
        return new SemeaduraDoCatalogo(
                new SemeaduraProperties(ligada), categorias, produtos, Clock.fixed(AGORA, ZoneOffset.UTC));
    }
}
