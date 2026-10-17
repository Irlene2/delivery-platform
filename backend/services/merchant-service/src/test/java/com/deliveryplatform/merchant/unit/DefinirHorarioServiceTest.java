package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.exception.HorarioRecusado;
import com.deliveryplatform.merchant.application.port.in.DefinirHorario.FaixaPedida;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.application.usecase.DefinirHorarioService;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import com.deliveryplatform.merchant.support.EquipeDeTeste;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O caso de uso do horário (ADR-073): autoriza antes de buscar, exige
 * {@code CONFIGURAR_LOJA}, e traduz as duas recusas do domínio num ponto só.
 */
@ExtendWith(MockitoExtension.class)
class DefinirHorarioServiceTest {

    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final Map<DayOfWeek, List<FaixaPedida>> SEXTA = Map.of(DayOfWeek.FRIDAY,
            List.of(new FaixaPedida(LocalTime.of(18, 0), LocalTime.of(23, 0))));

    @Mock MembroRepositorio membros;
    @Mock EstabelecimentoRepositorio estabelecimentos;

    private DefinirHorarioService servico() {
        return new DefinirHorarioService(membros, estabelecimentos);
    }

    private static Membro vinculoCom(Permissao permissao) {
        return Membro.reconstituir(UUID.randomUUID(), AUTOR, LOJA, Papel.COLABORADOR,
                EnumSet.of(permissao), EstadoDoMembro.ATIVO, EquipeDeTeste.AGORA, EquipeDeTeste.AGORA);
    }

    @Test
    @DisplayName("10 · sem vínculo é AcessoNegado, e a loja nem é buscada — autoriza antes de buscar")
    void autoriza_antes_de_buscar() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico().definir(LOJA, AUTOR, SEXTA))
                .isInstanceOf(AcessoNegado.class);
        verifyNoInteractions(estabelecimentos);
    }

    @Test
    @DisplayName("11 · exige CONFIGURAR_LOJA: outra permissão qualquer não serve")
    void exige_configurar_loja() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA))
                .thenReturn(Optional.of(vinculoCom(Permissao.GERENCIAR_EQUIPE)));

        assertThatThrownBy(() -> servico().definir(LOJA, AUTOR, SEXTA))
                .isInstanceOf(AcessoNegado.class);
        verifyNoInteractions(estabelecimentos);
    }

    @Test
    @DisplayName("11b · com CONFIGURAR_LOJA, grava e devolve o horário como ficou")
    void com_a_permissao_grava() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA))
                .thenReturn(Optional.of(vinculoCom(Permissao.CONFIGURAR_LOJA)));
        when(estabelecimentos.buscarPorId(LOJA)).thenReturn(Optional.of(LojaDeTeste.pizzaria()));
        when(estabelecimentos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));

        var horario = servico().definir(LOJA, AUTOR, SEXTA);

        assertThat(horario.horarioDeFuncionamento()).containsOnlyKeys(DayOfWeek.FRIDAY);
    }

    @Test
    @DisplayName("14 · início igual ao fim sai como HorarioRecusado, e não IllegalArgumentException")
    void traduz_inicio_igual_ao_fim() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA))
                .thenReturn(Optional.of(vinculoCom(Permissao.CONFIGURAR_LOJA)));
        var ambigua = Map.of(DayOfWeek.MONDAY,
                List.of(new FaixaPedida(LocalTime.of(10, 0), LocalTime.of(10, 0))));

        assertThatThrownBy(() -> servico().definir(LOJA, AUTOR, ambigua))
                .isInstanceOf(HorarioRecusado.class)
                .satisfies(e -> assertThat(((HorarioRecusado) e).motivo())
                        .isEqualTo(HorarioRecusado.Motivo.INICIO_IGUAL_AO_FIM))
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("faixa de início igual ao fim é ambígua — nem turno vazio nem "
                        + "vinte e quatro horas: 10:00");
    }

    @Test
    @DisplayName("14b · a faixa repetida sai como HorarioRecusado, com a mensagem do domínio na causa")
    void traduz_faixa_repetida() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA))
                .thenReturn(Optional.of(vinculoCom(Permissao.CONFIGURAR_LOJA)));
        when(estabelecimentos.buscarPorId(LOJA)).thenReturn(Optional.of(LojaDeTeste.pizzaria()));
        var almoco = new FaixaPedida(LocalTime.of(11, 0), LocalTime.of(14, 0));
        var repetida = Map.of(DayOfWeek.MONDAY, List.of(almoco, almoco));

        assertThatThrownBy(() -> servico().definir(LOJA, AUTOR, repetida))
                .isInstanceOf(HorarioRecusado.class)
                .satisfies(e -> assertThat(((HorarioRecusado) e).motivo())
                        .isEqualTo(HorarioRecusado.Motivo.FAIXA_REPETIDA))
                .hasRootCauseMessage("faixa repetida em MONDAY: [11:00–14:00, 11:00–14:00]");
    }
}
