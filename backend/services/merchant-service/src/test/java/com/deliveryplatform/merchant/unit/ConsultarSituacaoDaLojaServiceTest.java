package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.application.exception.LojaNaoEncontrada;
import com.deliveryplatform.merchant.application.port.in.ConsultarSituacaoDaLoja.SituacaoDaLoja;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.usecase.ConsultarSituacaoDaLojaService;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * A situação da loja para a vitrine (ADR-077, emenda de 01/11/2026). A pizzaria abre terça das
 * 18:00 às 02:00; 03/11/2026 é uma terça.
 */
@ExtendWith(MockitoExtension.class)
class ConsultarSituacaoDaLojaServiceTest {

    private static final UUID LOJA = UUID.randomUUID();

    @Mock EstabelecimentoRepositorio lojas;

    private static Instant local(String quando) {
        return LocalDateTime.parse(quando).atZone(FusoHorario.PADRAO.zona()).toInstant();
    }

    private SituacaoDaLoja em(String quando, Estabelecimento loja) {
        when(lojas.buscarPorId(LOJA)).thenReturn(Optional.of(loja));
        Clock relogio = Clock.fixed(local(quando), FusoHorario.PADRAO.zona());
        return new ConsultarSituacaoDaLojaService(lojas, relogio).daLoja(LOJA);
    }

    @Test
    @DisplayName("1 · dentro da faixa: aberta, e sem próxima abertura")
    void dentro_da_faixa() {
        SituacaoDaLoja s = em("2026-11-03T20:00", LojaDeTeste.pizzaria());

        assertThat(s.aberta()).isTrue();
        assertThat(s.proximaAbertura()).isEmpty();
        assertThat(s.nome()).isEqualTo("Pizzaria da Marli");
    }

    @Test
    @DisplayName("2 · antes da faixa: fechada, e abre às 18:00 do mesmo dia")
    void antes_da_faixa() {
        SituacaoDaLoja s = em("2026-11-03T15:00", LojaDeTeste.pizzaria());

        assertThat(s.aberta()).isFalse();
        assertThat(s.proximaAbertura()).contains(local("2026-11-03T18:00"));
    }

    @Test
    @DisplayName("3 · a faixa que cruza a meia-noite ainda vale à 01:00 de quarta")
    void depois_da_meia_noite() {
        assertThat(em("2026-11-04T01:00", LojaDeTeste.pizzaria()).aberta()).isTrue();
    }

    @Test
    @DisplayName("4 · loja sem horário: fechada, e sem próxima abertura")
    void sem_horario() {
        SituacaoDaLoja s = em("2026-11-03T20:00", LojaDeTeste.semHorario());

        assertThat(s.aberta()).isFalse();
        assertThat(s.proximaAbertura()).isEmpty();
    }

    @Test
    @DisplayName("5 · identificador que não é de loja: LojaNaoEncontrada")
    void inexistente() {
        when(lojas.buscarPorId(LOJA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ConsultarSituacaoDaLojaService(lojas, Clock.systemUTC())
                .daLoja(LOJA)).isInstanceOf(LojaNaoEncontrada.class);
    }
}
