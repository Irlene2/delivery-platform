package com.deliveryplatform.catalog.unit;

import com.deliveryplatform.catalog.domain.exception.RegraDoCatalogoViolada;
import com.deliveryplatform.catalog.domain.model.Disponibilidade;
import com.deliveryplatform.catalog.domain.model.EstadoDeDisponibilidade;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.Precos;
import com.deliveryplatform.catalog.support.ProdutoDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** O selo da vitrine e a troca do "pode acabar" (13/11/2026). */
class SeloEControleDoProdutoTest {

    private static Produto pizza() {
        return Produto.rascunho(ProdutoDeTeste.LOJA, ProdutoDeTeste.CATEGORIA, "Pizza",
                Precos.reais("40.00"), ModoDeControle.QUALITATIVO, 0);
    }

    @Test
    @DisplayName("o selo é guardado sem os espaços das pontas, e vazio tira")
    void selo_poe_e_tira() {
        Produto pizza = pizza();

        pizza.rotular("  Mais pedido ");
        assertThat(pizza.getSelo()).isEqualTo("Mais pedido");

        pizza.rotular("");
        assertThat(pizza.getSelo()).isNull();
    }

    @Test
    @DisplayName("selo com mais de 30 letras é recusado, e o anterior fica")
    void selo_longo_e_recusado() {
        Produto pizza = pizza();
        pizza.rotular("Novo");

        assertThatThrownBy(() -> pizza.rotular("x".repeat(31)))
                .isInstanceOf(RegraDoCatalogoViolada.class)
                .extracting(e -> ((RegraDoCatalogoViolada) e).motivo())
                .isEqualTo(RegraDoCatalogoViolada.Motivo.SELO_LONGO);
        assertThat(pizza.getSelo()).isEqualTo("Novo");
    }

    @Test
    @DisplayName("virar 'nunca acaba' devolve o produto acabado a disponível")
    void sem_controle_volta_a_disponivel() {
        Produto pizza = pizza();
        pizza.marcar(new Disponibilidade(EstadoDeDisponibilidade.ESGOTADO_INDETERMINADO, null, null));

        pizza.trocarControle(ModoDeControle.SEM_CONTROLE);

        assertThat(pizza.getModoDeControle()).isEqualTo(ModoDeControle.SEM_CONTROLE);
        assertThat(pizza.getDisponibilidade().estado()).isEqualTo(EstadoDeDisponibilidade.DISPONIVEL);
    }

    @Test
    @DisplayName("virar 'pode acabar' mantém a disponibilidade, e a marcação passa a valer")
    void com_controle_passa_a_aceitar_marcacao() {
        Produto lata = Produto.rascunho(ProdutoDeTeste.LOJA, ProdutoDeTeste.CATEGORIA, "Lata",
                Precos.reais("6.00"), ModoDeControle.SEM_CONTROLE, 0);

        lata.trocarControle(ModoDeControle.QUALITATIVO);
        lata.marcar(new Disponibilidade(EstadoDeDisponibilidade.ACABANDO, null, null));

        assertThat(lata.getDisponibilidade().estado()).isEqualTo(EstadoDeDisponibilidade.ACABANDO);
    }
}
