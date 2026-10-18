package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.application.usecase.ConsultarHorarioService;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
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

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** A leitura do horário (ADR-073, emenda de 17/10/2026): mesma porta e mesma permissão da escrita. */
@ExtendWith(MockitoExtension.class)
class ConsultarHorarioServiceTest {

    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();

    @Mock MembroRepositorio membros;
    @Mock EstabelecimentoRepositorio estabelecimentos;

    private ConsultarHorarioService servico() {
        return new ConsultarHorarioService(membros, estabelecimentos);
    }

    private static Membro vinculoCom(Permissao permissao) {
        return Membro.reconstituir(UUID.randomUUID(), AUTOR, LOJA, Papel.COLABORADOR,
                EnumSet.of(permissao), EstadoDoMembro.ATIVO, EquipeDeTeste.AGORA, EquipeDeTeste.AGORA);
    }

    @Test
    @DisplayName("1 · sem vínculo é AcessoNegado, e a loja nem é buscada — autoriza antes de buscar")
    void autoriza_antes_de_buscar() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico().daLoja(LOJA, AUTOR)).isInstanceOf(AcessoNegado.class);
        verifyNoInteractions(estabelecimentos);
    }

    @Test
    @DisplayName("2 · exige CONFIGURAR_LOJA: outra permissão qualquer não serve")
    void exige_configurar_loja() {
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA))
                .thenReturn(Optional.of(vinculoCom(Permissao.GERENCIAR_EQUIPE)));

        assertThatThrownBy(() -> servico().daLoja(LOJA, AUTOR)).isInstanceOf(AcessoNegado.class);
        verifyNoInteractions(estabelecimentos);
    }

    @Test
    @DisplayName("4 · devolve a semana como está gravada")
    void devolve_a_semana_gravada() {
        Estabelecimento gravada = Estabelecimento.reconstituir(LOJA,
                LojaDeTeste.identificacao(FusoHorario.PADRAO), LojaDeTeste.operacao(),
                LojaDeTeste.troco(), LojaDeTeste.disponibilidade(), LojaDeTeste.areas());
        when(membros.buscarPorUsuarioELoja(AUTOR, LOJA))
                .thenReturn(Optional.of(vinculoCom(Permissao.CONFIGURAR_LOJA)));
        when(estabelecimentos.buscarPorId(LOJA)).thenReturn(Optional.of(gravada));

        assertThat(servico().daLoja(LOJA, AUTOR)).isEqualTo(LojaDeTeste.disponibilidade());
    }
}
