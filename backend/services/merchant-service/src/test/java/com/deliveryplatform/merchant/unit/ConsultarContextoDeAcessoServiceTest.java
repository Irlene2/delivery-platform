package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.port.in.ConsultarContextoDeAcesso;
import com.deliveryplatform.merchant.application.port.in.ContextoDeAcesso;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.application.usecase.ConsultarContextoDeAcessoService;
import com.deliveryplatform.merchant.domain.model.Equipe;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O caso de uso da autorização entre serviços, e o teste que existe por causa
 * de uma armadilha do repositório.
 *
 * <p>O {@code buscarPorUsuarioELoja} devolve vínculo em qualquer estado. Três
 * dos testes abaixo — suspenso, removido, sem vínculo — provam que os três dão
 * a <b>mesma</b> recusa, e o do suspenso é o que pegaria a falta do
 * {@code filter(Membro::ativo)}. Sem ele, um funcionário demitido continuaria
 * autorizado em todos os serviços.
 */
class ConsultarContextoDeAcessoServiceTest {

    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final Instant AGORA = Instant.parse("2026-09-27T12:00:00Z");

    /**
     * Dublê mínimo, escrito aqui de propósito: ele devolve o vínculo <b>em
     * qualquer estado</b>, que é exatamente o que o adaptador de verdade faz.
     * Um dublê que filtrasse {@code ativo()} esconderia a armadilha que este
     * arquivo existe para cobrir.
     *
     * <p>Se o repositório já tiver um dublê de {@code MembroRepositorio} em
     * {@code support/}, use-o — <b>desde que ele não filtre estado</b> — e
     * apague este.
     */
    private static ConsultarContextoDeAcesso servicoCom(Membro vinculo) {
        return new ConsultarContextoDeAcessoService(new MembroRepositorio() {
            @Override
            public Membro salvar(Membro membro) {
                throw new UnsupportedOperationException("não usado neste teste");
            }

            @Override
            public Optional<Membro> buscarPorUsuarioELoja(UUID usuarioId, UUID estabelecimentoId) {
                return Optional.ofNullable(vinculo);
            }

            @Override
            public Equipe equipeDe(UUID estabelecimentoId) {
                throw new UnsupportedOperationException("não usado neste teste");
            }

            @Override
            public Equipe equipeParaAlteracao(UUID estabelecimentoId) {
                throw new UnsupportedOperationException("não usado neste teste");
            }
        });
    }

    private static Membro vinculo(Papel papel, Set<Permissao> permissoes, EstadoDoMembro estado) {
        return Membro.reconstituir(UUID.randomUUID(), USUARIO, LOJA,
                papel, permissoes, estado, AGORA, AGORA);
    }

    @Nested
    class OQueEleResponde {

        @Test
        @DisplayName("vínculo ativo devolve os quatro campos do record da §3")
        void vinculo_ativo() {
            ContextoDeAcesso contexto = servicoCom(vinculo(Papel.COLABORADOR,
                    EnumSet.of(Permissao.VER_PEDIDO, Permissao.ALTERAR_STATUS),
                    EstadoDoMembro.ATIVO)).doPortador(LOJA, USUARIO);

            assertThat(contexto.usuarioId()).isEqualTo(USUARIO);
            assertThat(contexto.estabelecimentoId()).isEqualTo(LOJA);
            assertThat(contexto.papel()).isEqualTo(Papel.COLABORADOR);
            assertThat(contexto.permissoes())
                    .containsExactlyInAnyOrder(Permissao.VER_PEDIDO, Permissao.ALTERAR_STATUS);
        }

        @Test
        @DisplayName("administrador com uma permissão devolve UMA — papel não é lista de permissões")
        void o_papel_nao_deduz_permissao() {
            ContextoDeAcesso contexto = servicoCom(vinculo(Papel.ADMINISTRADOR,
                    EnumSet.of(Permissao.VER_VENDAS),
                    EstadoDoMembro.ATIVO)).doPortador(LOJA, USUARIO);

            assertThat(contexto.papel()).isEqualTo(Papel.ADMINISTRADOR);
            assertThat(contexto.permissoes())
                    .as("o estabelecimento.md §2 diz que papel não é lista de permissões, e o "
                            + "promover do agregado muda só o papel. Deduzir 'administrador tem "
                            + "tudo' daria dez permissões que ninguém concedeu, a cada promoção")
                    .containsExactly(Permissao.VER_VENDAS);
        }

        @Test
        @DisplayName("colaborador sem permissão nenhuma é resposta válida, e não recusa")
        void sem_permissao_nao_e_recusa() {
            ContextoDeAcesso contexto = servicoCom(vinculo(Papel.COLABORADOR,
                    EnumSet.noneOf(Permissao.class),
                    EstadoDoMembro.ATIVO)).doPortador(LOJA, USUARIO);

            assertThat(contexto.permissoes())
                    .as("quem tem vínculo ativo e nenhuma permissão existe: é o recém-convidado "
                            + "cujas permissões ainda não foram dadas. Quem decide o que fazer "
                            + "com isso é o serviço que perguntou")
                    .isEmpty();
        }
    }

    @Nested
    class AsQuatroRecusas {

        @Test
        @DisplayName("sem vínculo — ou loja que não existe, que é a mesma consulta")
        void sem_vinculo() {
            assertThatThrownBy(() -> servicoCom(null).doPortador(LOJA, USUARIO))
                    .isInstanceOf(AcessoNegado.class);
        }

        @Test
        @DisplayName("SUSPENSO recusa — é o teste que pega a falta do filter(Membro::ativo)")
        void suspenso() {
            assertThatThrownBy(() -> servicoCom(vinculo(Papel.COLABORADOR,
                    EnumSet.allOf(Permissao.class),
                    EstadoDoMembro.SUSPENSO)).doPortador(LOJA, USUARIO))
                    .as("o repositório devolve suspenso preenchido. Sem o filtro, este vínculo "
                            + "responderia com TODAS as permissões")
                    .isInstanceOf(AcessoNegado.class);
        }

        @Test
        @DisplayName("REMOVIDO recusa — o demitido não autoriza nada em nenhum serviço")
        void removido() {
            assertThatThrownBy(() -> servicoCom(vinculo(Papel.ADMINISTRADOR,
                    EnumSet.allOf(Permissao.class),
                    EstadoDoMembro.REMOVIDO)).doPortador(LOJA, USUARIO))
                    .isInstanceOf(AcessoNegado.class);
        }

        @Test
        @DisplayName("as recusas são indistinguíveis — mesma exceção, mesma mensagem (M7)")
        void indistinguiveis() {
            Throwable semVinculo = capturar(null);
            Throwable suspenso = capturar(vinculo(Papel.COLABORADOR,
                    EnumSet.allOf(Permissao.class), EstadoDoMembro.SUSPENSO));

            assertThat(suspenso.getClass()).isEqualTo(semVinculo.getClass());
            assertThat(suspenso.getMessage())
                    .as("mensagens diferentes diriam quais identificadores são lojas de verdade, "
                            + "e quais delas o portador já teve acesso")
                    .isEqualTo(semVinculo.getMessage());
        }

        private static Throwable capturar(Membro vinculo) {
            try {
                servicoCom(vinculo).doPortador(LOJA, USUARIO);
                throw new AssertionError("devia ter recusado");
            } catch (AcessoNegado negado) {
                return negado;
            }
        }
    }

    @Test
    @DisplayName("não existe caminho que receba usuarioId de fora do token")
    void o_usuario_vem_do_token_e_de_mais_nenhum_lugar() {
        assertThat(ConsultarContextoDeAcesso.class.getDeclaredMethods())
                .as("a segurança desta rota não vem de exigir permissão — vem de o sujeito da "
                        + "resposta ser sempre o portador. Um método a mais aqui, recebendo um "
                        + "usuarioId afirmado por quem chama, desfaz a ADR-045 inteira")
                .hasSize(1);
    }
}
