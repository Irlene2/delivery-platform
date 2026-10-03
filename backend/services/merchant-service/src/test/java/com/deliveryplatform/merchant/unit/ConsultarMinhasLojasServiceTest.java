package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.application.port.in.LojaDoUsuario;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.application.port.out.NomesDeEstabelecimentos;
import com.deliveryplatform.merchant.application.port.out.NomesDeEstabelecimentos.NomeDaLoja;
import com.deliveryplatform.merchant.application.usecase.ConsultarMinhasLojasService;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O caso de uso sozinho, com dublês que <b>não escondem a armadilha</b>.
 *
 * <p>O dublê do {@code MembroRepositorio} devolve vínculo em <b>qualquer
 * estado</b>, como o adaptador de verdade — é essa a lição da G-B2, onde um
 * dublê que só devolvia ativos teria escondido exatamente o defeito que o teste
 * existia para pegar.
 *
 * <p>E ele <b>usa o argumento</b>: devolve lista diferente para usuário
 * diferente. Um dublê que ignorasse o {@code usuarioId} faria passar um caso de
 * uso que devolvesse as lojas de outra pessoa — que é a pior falha que esta rota
 * pode ter, e foi a segunda das três asserções que a G-B2 achou sem valor.
 */
class ConsultarMinhasLojasServiceTest {

    private static final UUID MARLI = UUID.randomUUID();
    private static final UUID BIA = UUID.randomUUID();

    private static final UUID PIZZARIA = UUID.randomUUID();
    private static final UUID PADARIA = UUID.randomUUID();
    private static final UUID LANCHONETE = UUID.randomUUID();

    // ── os dublês ───────────────────────────────────────────────────────────

    /**
     * Os vínculos, por usuário — e em qualquer estado, como o repositório real.
     *
     * <p>Os quatro métodos que não são usados estouram. Devolver vazio neles
     * faria um caso de uso que chamasse o método errado passar em silêncio.
     */
    private static MembroRepositorio vinculos(UUID usuario, List<Membro> lista) {
        return new MembroRepositorio() {
            @Override
            public List<Membro> buscarPorUsuario(UUID usuarioId) {
                return usuarioId.equals(usuario) ? lista : List.of();
            }

            @Override
            public Membro salvar(Membro membro) {
                throw new UnsupportedOperationException("não é deste caso de uso");
            }

            @Override
            public java.util.Optional<Membro> buscarPorUsuarioELoja(UUID u, UUID e) {
                throw new UnsupportedOperationException("não é deste caso de uso");
            }

            @Override
            public com.deliveryplatform.merchant.domain.model.Equipe equipeDe(UUID e) {
                throw new UnsupportedOperationException("não é deste caso de uso");
            }

            @Override
            public com.deliveryplatform.merchant.domain.model.Equipe equipeParaAlteracao(UUID e) {
                throw new UnsupportedOperationException("não é deste caso de uso");
            }
        };
    }

    // ── o filtro, que é a razão de esta classe existir ──────────────────────

    @Test
    @DisplayName("vínculo SUSPENSO não aparece — quem foi afastado não vê a loja no seletor")
    void suspenso_nao_aparece() {
        var servico = servicoCom(
                List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.SUSPENSO)),
                List.of(loja(PIZZARIA, "Pizzaria da Marli")));

        assertThat(servico.de(MARLI))
                .as("o repositório devolve vínculo em qualquer estado, e este caso de uso "
                        + "não passa por Membro.pode(...) — sem o filtro explícito, "
                        + "alguém afastado abriria o painel e só levaria 403 na primeira ação")
                .isEmpty();
    }

    @Test
    @DisplayName("vínculo REMOVIDO não aparece")
    void removido_nao_aparece() {
        var servico = servicoCom(
                List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.REMOVIDO)),
                List.of(loja(PIZZARIA, "Pizzaria da Marli")));

        assertThat(servico.de(MARLI)).isEmpty();
    }

    @Test
    @DisplayName("entre três vínculos, só o ATIVO passa")
    void so_o_ativo_passa() {
        var servico = servicoCom(
                List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.ATIVO),
                        membro(MARLI, PADARIA, EstadoDoMembro.SUSPENSO),
                        membro(MARLI, LANCHONETE, EstadoDoMembro.REMOVIDO)),
                List.of(loja(PIZZARIA, "Pizzaria"), loja(PADARIA, "Padaria"),
                        loja(LANCHONETE, "Lanchonete")));

        assertThat(servico.de(MARLI))
                .extracting(LojaDoUsuario::estabelecimentoId)
                .containsExactly(PIZZARIA);
    }

    // ── o usuário ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("as lojas são as DESTE usuário — o dublê responde por usuário")
    void nao_devolve_a_loja_de_outra_pessoa() {
        var servico = servicoCom(
                List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.ATIVO)),
                List.of(loja(PIZZARIA, "Pizzaria")));

        assertThat(servico.de(BIA))
                .as("um caso de uso que ignorasse o argumento passaria em todos os "
                        + "outros testes deste arquivo")
                .isEmpty();
    }

    // ── o que vem junto ─────────────────────────────────────────────────────

    @Test
    @DisplayName("papel e permissões vêm na mesma resposta — é o que dispensa a segunda chamada")
    void traz_papel_e_permissoes() {
        Membro colaborador = membro(MARLI, PIZZARIA, EstadoDoMembro.ATIVO);

        var servico = servicoCom(List.of(colaborador), List.of(loja(PIZZARIA, "Pizzaria")));

        LojaDoUsuario unica = servico.de(MARLI).getFirst();
        assertThat(unica.nome()).isEqualTo("Pizzaria");
        assertThat(unica.papel()).isEqualTo(colaborador.getPapel());
        assertThat(unica.permissoes())
                .as("as permissões são as GRAVADAS, não deduzidas do papel — "
                        + "estabelecimento.md §2")
                .containsExactlyInAnyOrderElementsOf(colaborador.getPermissoes());
    }

    @Test
    @DisplayName("a ordem é por nome, e é estável — senão o seletor troca de ordem a cada recarga")
    void ordem_por_nome() {
        var servico = servicoCom(
                List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.ATIVO),
                        membro(MARLI, PADARIA, EstadoDoMembro.ATIVO),
                        membro(MARLI, LANCHONETE, EstadoDoMembro.ATIVO)),
                List.of(loja(PIZZARIA, "Zé da Pizza"), loja(PADARIA, "Ana Pães"),
                        loja(LANCHONETE, "moacir lanches")));

        assertThat(servico.de(MARLI))
                .extracting(LojaDoUsuario::nome)
                .as("sem ordem, a pessoa clica na loja errada depois de uma recarga; "
                        + "e a comparação ignora maiúsculas, senão 'moacir' iria para o fim")
                .containsExactly("Ana Pães", "moacir lanches", "Zé da Pizza");
    }

    // ── os casos que não deveriam acontecer ─────────────────────────────────

    @Test
    void sem_vinculo_nenhum_devolve_vazio_e_nao_estoura() {
        var servico = servicoCom(List.of(), List.of());

        assertThat(servico.de(MARLI))
                .as("é o estado de todo mundo no instante seguinte ao cadastro")
                .isEmpty();
    }

    @Test
    @DisplayName("vínculo ativo para loja que não existe é descartado, não vira linha sem nome")
    void loja_que_sumiu_nao_aparece() {
        var servico = servicoCom(
                List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.ATIVO)),
                List.of());

        assertThat(servico.de(MARLI))
                .as("mostrar uma loja sem nome é pior do que não mostrá-la: o front "
                        + "abriria o painel de uma loja que não existe e toda chamada "
                        + "dele daria 403")
                .isEmpty();
    }

    @Test
    @DisplayName("sem vínculo ativo, nem se pergunta pelos estabelecimentos")
    void nao_consulta_estabelecimentos_a_toa() {
        NomesDeEstabelecimentos queEstoura = ids -> {
            throw new AssertionError("não deveria consultar estabelecimentos sem vínculo ativo");
        };

        var servico = new ConsultarMinhasLojasService(
                vinculos(MARLI, List.of(membro(MARLI, PIZZARIA, EstadoDoMembro.REMOVIDO))),
                queEstoura);

        assertThat(servico.de(MARLI)).isEmpty();
    }

    // ── montagem ────────────────────────────────────────────────────────────

    private static ConsultarMinhasLojasService servicoCom(List<Membro> vinculosDaMarli,
                                                          List<NomeDaLoja> lojas) {
        return new ConsultarMinhasLojasService(
                vinculos(MARLI, vinculosDaMarli),
                ids -> lojas.stream()
                        .filter(l -> ids.contains(l.estabelecimentoId()))
                        .toList());
    }

    /**
     * Um membro no estado pedido.
     *
     * <p><b>Usa {@code Membro.reconstituir(...)}</b>, e não os métodos de
     * transição, porque o que este teste precisa é de um vínculo <i>em</i> um
     * estado, não da história que levou até ele.
     */
    private static Membro membro(UUID usuarioId, UUID lojaId, EstadoDoMembro estado) {
        java.time.Instant agora = java.time.Instant.parse("2026-09-29T12:00:00Z");
        return Membro.reconstituir(
                UUID.randomUUID(), usuarioId, lojaId,
                Papel.COLABORADOR,
                java.util.Set.of(Permissao.VER_PEDIDO, Permissao.VER_PRODUTO),
                estado, agora, agora);
    }

    private static NomeDaLoja loja(UUID id, String nome) {
        return new NomeDaLoja(id, nome);
    }
}
