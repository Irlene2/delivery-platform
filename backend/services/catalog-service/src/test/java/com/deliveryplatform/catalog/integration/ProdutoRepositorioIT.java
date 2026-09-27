package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.Disponibilidade;
import com.deliveryplatform.catalog.domain.model.EstadoDeDisponibilidade;
import com.deliveryplatform.catalog.domain.model.EstadoDePublicacao;
import com.deliveryplatform.catalog.domain.model.GrupoDeOpcoes;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Opcao;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.Precos;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A ida e a volta do agregado, e as três decisões de formato que o
 * {@code ProdutoDocumento} tomou.
 *
 * <p>Metade deste arquivo olha o <b>documento cru</b>, e não o objeto que
 * voltou. É de propósito: um mapeamento simétrico passa no teste de ida e
 * volta mesmo gravando coisa errada. Quem grava dinheiro como {@code double}
 * lê de volta o mesmo {@code double} e fica feliz até o primeiro centavo.
 */
@SpringBootTest
class ProdutoRepositorioIT extends Infraestrutura {

    @Autowired ProdutoRepositorio produtos;
    @Autowired MongoTemplate mongo;

    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID CATEGORIA = UUID.randomUUID();
    private static final Instant COM_NANOS = Instant.parse("2026-09-25T22:13:47.123456789Z");
    private static final LocalDate SEXTA = LocalDate.of(2026, 9, 25);

    @BeforeEach
    void limpar() {
        mongo.getCollection("produtos").deleteMany(new Document());
    }

    private static Produto margherita() {
        Produto p = Produto.rascunho(LOJA, CATEGORIA, "Pizza margherita",
                Precos.reais("49.90"), ModoDeControle.QUALITATIVO, 3);
        p.descreverCom("Molho, muçarela e manjericão.");
        p.acrescentarGrupo(GrupoDeOpcoes.novo("Tamanho", 1, 1, 0, List.of(
                Opcao.nova("Pequena", Precos.reais("0.00"), 0),
                Opcao.nova("Grande", Precos.reais("16.00"), 1))));
        p.acrescentarGrupo(GrupoDeOpcoes.novo("Adicionais", 0, 2, 1, List.of(
                Opcao.nova("Bacon", Precos.reais("6.00"), 0),
                Opcao.nova("Sem queijo", Precos.reais("-2.00"), 1))));
        return p;
    }

    private Document cru(UUID id) {
        return mongo.getCollection("produtos").find(new Document("_id", id)).first();
    }

    @Nested
    class IdaEVolta {

        @Test
        @DisplayName("a árvore inteira volta igual — grupos, opções, ordem e preços")
        void a_arvore_inteira() {
            Produto antes = margherita();
            antes.publicar();
            produtos.salvar(antes);

            Produto depois = produtos.buscarPorId(antes.getId()).orElseThrow();

            assertThat(depois.getId()).isEqualTo(antes.getId());
            assertThat(depois.getEstabelecimentoId()).isEqualTo(LOJA);
            assertThat(depois.getCategoriaId()).isEqualTo(CATEGORIA);
            assertThat(depois.getNome()).isEqualTo("Pizza margherita");
            assertThat(depois.getDescricao()).isEqualTo("Molho, muçarela e manjericão.");
            assertThat(depois.getImagemRef()).isNull();
            assertThat(depois.getPrecoBase()).isEqualTo(Precos.reais("49.90"));
            assertThat(depois.getOrdem()).isEqualTo(3);
            assertThat(depois.getEstadoDePublicacao()).isEqualTo(EstadoDePublicacao.ATIVO);
            assertThat(depois.getModoDeControle()).isEqualTo(ModoDeControle.QUALITATIVO);

            assertThat(depois.getGruposDeOpcoes()).hasSize(2);
            GrupoDeOpcoes tamanho = depois.getGruposDeOpcoes().getFirst();
            assertThat(tamanho.nome()).isEqualTo("Tamanho");
            assertThat(tamanho.minEscolhas()).isEqualTo(1);
            assertThat(tamanho.opcoes()).extracting(Opcao::nome)
                    .containsExactly("Pequena", "Grande");
            assertThat(depois.getGruposDeOpcoes().get(1).opcoes().get(1).acrescimo())
                    .as("o desconto atravessa a gravação com o sinal")
                    .isEqualTo(Precos.reais("-2.00"));
        }

        @Test
        @DisplayName("o rascunho volta rascunho — a reconstituição não republica nada")
        void o_estado_e_preservado() {
            Produto rascunho = produtos.salvar(margherita());

            assertThat(produtos.buscarPorId(rascunho.getId()).orElseThrow()
                    .getEstadoDePublicacao())
                    .isEqualTo(EstadoDePublicacao.RASCUNHO);
        }

        @Test
        @DisplayName("o instante volta com os nanossegundos — Date truncaria e ninguém veria")
        void o_instante_nao_e_truncado() {
            Produto p = margherita();
            p.marcar(Disponibilidade.esgotadoHoje(COM_NANOS, SEXTA));
            produtos.salvar(p);

            Disponibilidade d = produtos.buscarPorId(p.getId()).orElseThrow()
                    .getDisponibilidade();

            assertThat(d.estado()).isEqualTo(EstadoDeDisponibilidade.ESGOTADO_HOJE);
            assertThat(d.marcadoEm()).isEqualTo(COM_NANOS);
            assertThat(d.expedienteDeReferencia()).isEqualTo(SEXTA);
        }

        @Test
        void produto_que_nao_existe_volta_vazio() {
            assertThat(produtos.buscarPorId(UUID.randomUUID())).isEmpty();
        }
    }

    @Nested
    class ODocumentoCru {

        @Test
        @DisplayName("dinheiro é texto, e não número — a ADR-009 não sobrevive a um double")
        void dinheiro_e_texto() {
            Produto p = produtos.salvar(margherita());

            Document preco = cru(p.getId()).get("precoBase", Document.class);

            assertThat(preco.get("valor")).isInstanceOf(String.class).isEqualTo("49.90");
            assertThat(preco.get("moeda")).isEqualTo("BRL");
        }

        @Test
        @DisplayName("o dia operacional é texto ISO — um Date lhe daria uma hora e um fuso")
        void o_expediente_e_texto() {
            Produto p = margherita();
            p.marcar(Disponibilidade.esgotadoHoje(COM_NANOS, SEXTA));
            produtos.salvar(p);

            Document disponibilidade = cru(p.getId()).get("disponibilidade", Document.class);

            assertThat(disponibilidade.get("expedienteDeReferencia"))
                    .as("a reativação compara datas (C11); um fuso a mais nessa comparação "
                            + "é o buraco da hora de corte por outra porta")
                    .isEqualTo("2026-09-25");
            assertThat(disponibilidade.get("marcadoEm"))
                    .isEqualTo("2026-09-25T22:13:47.123456789Z");
        }

        @Test
        @DisplayName("o vendável não está gravado em lugar nenhum")
        void nao_existe_campo_vendavel() {
            Produto p = margherita();
            p.publicar();
            produtos.salvar(p);

            assertThat(cru(p.getId()).toJson())
                    .as("ele é derivado (§5). Um booleano gravado teria cinco caminhos de "
                            + "escrita, e o quinto que alguém esquecesse mentiria para sempre")
                    .doesNotContain("vendavel");
        }

        @Test
        @DisplayName("o carimbo ausente grava nulo dos dois lados, e não some")
        void carimbo_ausente() {
            Produto p = produtos.salvar(margherita());

            Document disponibilidade = cru(p.getId()).get("disponibilidade", Document.class);

            assertThat(disponibilidade.get("estado")).isEqualTo("DISPONIVEL");
            assertThat(disponibilidade.get("marcadoEm")).isNull();
            assertThat(disponibilidade.get("expedienteDeReferencia")).isNull();
        }
    }

    @Nested
    class AConsultaDoPrimeiroIndice {

        @Test
        @DisplayName("publicadosDe traz só os ATIVO, e só os da loja pedida")
        void publicados_de_uma_loja() {
            Produto ativo = margherita();
            ativo.publicar();
            produtos.salvar(ativo);

            produtos.salvar(margherita());          // rascunho, mesma loja

            Produto deOutraLoja = Produto.rascunho(UUID.randomUUID(), CATEGORIA, "Coxinha",
                    Precos.reais("8.00"), ModoDeControle.SEM_CONTROLE, 0);
            deOutraLoja.publicar();
            produtos.salvar(deOutraLoja);

            assertThat(produtos.publicadosDe(LOJA))
                    .extracting(Produto::getId)
                    .containsExactly(ativo.getId());
        }

        @Test
        void loja_sem_produto_publicado_volta_vazia() {
            produtos.salvar(margherita());

            assertThat(produtos.publicadosDe(LOJA)).isEmpty();
        }
    }
}
