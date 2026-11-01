package com.deliveryplatform.catalog.unit;

import com.deliveryplatform.catalog.domain.exception.RegraDoCatalogoViolada;
import com.deliveryplatform.catalog.domain.model.ImagemDoProduto;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.Precos;
import com.deliveryplatform.catalog.support.ProdutoDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A correção do cadastro e a foto do produto (ADR-077, emenda de 01/11/2026). */
class AlteracaoDoCadastroTest {

    @Test
    @DisplayName("rascunho: nome, preço, descrição e seção mudam; descrição em branco vira nenhuma")
    void rascunho_muda_tudo() {
        Produto pizza = ProdutoDeTeste.margherita();
        UUID outraSecao = UUID.randomUUID();

        pizza.alterarCadastro(outraSecao, "  Pizza napolitana ", Precos.reais("52.90"), "   ");

        assertThat(pizza.getNome()).isEqualTo("Pizza napolitana");
        assertThat(pizza.getPrecoBase()).isEqualTo(Precos.reais("52.90"));
        assertThat(pizza.getCategoriaId()).isEqualTo(outraSecao);
        assertThat(pizza.getDescricao()).isNull();
    }

    @Test
    @DisplayName("rascunho aceita preço zero, como na criação; negativo nunca")
    void rascunho_e_o_preco() {
        Produto pizza = ProdutoDeTeste.margherita();

        pizza.alterarCadastro(ProdutoDeTeste.CATEGORIA, "Pizza", Precos.zero(), null);
        assertThat(pizza.getPrecoBase()).isEqualTo(Precos.zero());

        assertThatThrownBy(() -> pizza.alterarCadastro(ProdutoDeTeste.CATEGORIA, "Pizza",
                Precos.reais("-1.00"), null))
                .isInstanceOf(RegraDoCatalogoViolada.class);
    }

    @Test
    @DisplayName("publicado: preço zero é o C1 da publicação, e nada muda")
    void publicado_nao_vai_a_zero() {
        Produto pizza = ProdutoDeTeste.margherita();
        pizza.publicar();

        assertThatThrownBy(() -> pizza.alterarCadastro(ProdutoDeTeste.CATEGORIA, "Outro nome",
                Precos.zero(), "x"))
                .isInstanceOf(RegraDoCatalogoViolada.class);
        assertThat(pizza.getNome()).isEqualTo("Pizza margherita");
        assertThat(pizza.getPrecoBase()).isEqualTo(Precos.reais("49.90"));
    }

    @Test
    @DisplayName("nome em branco é recusado")
    void nome_em_branco() {
        assertThatThrownBy(() -> ProdutoDeTeste.margherita().alterarCadastro(
                ProdutoDeTeste.CATEGORIA, "  ", Precos.reais("10.00"), null))
                .isInstanceOf(RegraDoCatalogoViolada.class);
    }

    @Test
    @DisplayName("foto: só JPG, PNG ou WebP, e até 600 KB")
    void foto() {
        UUID produto = UUID.randomUUID();
        UUID loja = UUID.randomUUID();

        assertThat(new ImagemDoProduto(produto, loja, "image/jpeg", new byte[]{1, 2, 3}).tipo())
                .isEqualTo("image/jpeg");
        assertThatThrownBy(() -> new ImagemDoProduto(produto, loja, "application/pdf", new byte[]{1}))
                .isInstanceOf(RegraDoCatalogoViolada.class);
        assertThatThrownBy(() -> new ImagemDoProduto(produto, loja, "image/png",
                new byte[ImagemDoProduto.TAMANHO_MAXIMO + 1]))
                .isInstanceOf(RegraDoCatalogoViolada.class);
        assertThatThrownBy(() -> new ImagemDoProduto(produto, loja, "image/png", new byte[0]))
                .isInstanceOf(RegraDoCatalogoViolada.class);
    }
}
