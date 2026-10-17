package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.domain.model.Disponibilidade;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.Faixa;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
import com.deliveryplatform.merchant.domain.model.Pausa;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code Estabelecimento.comHorario} (ADR-073 §1): o horário semanal substituído inteiro,
 * pelo mesmo construtor e pelas mesmas recusas — e o que o domínio aceita de propósito.
 */
class HorarioDaLojaTest {

    private static Estabelecimento lojaCom(Disponibilidade disponibilidade) {
        return Estabelecimento.reconstituir(UUID.randomUUID(),
                LojaDeTeste.identificacao(FusoHorario.PADRAO), LojaDeTeste.operacao(),
                LojaDeTeste.troco(), disponibilidade, LojaDeTeste.areas());
    }

    private static Map<DayOfWeek, List<Faixa>> horario(Object... diaEFaixas) {
        Map<DayOfWeek, List<Faixa>> mapa = new EnumMap<>(DayOfWeek.class);
        for (int i = 0; i < diaEFaixas.length; i += 2) {
            @SuppressWarnings("unchecked")
            List<Faixa> faixas = (List<Faixa>) diaEFaixas[i + 1];
            mapa.put((DayOfWeek) diaEFaixas[i], faixas);
        }
        return mapa;
    }

    // ── A · a raiz ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("1 · a loja semHorario recebe faixas e passa a ter horário — e a pausa sobrevive")
    void sai_do_semHorario_e_preserva_a_pausa() {
        Pausa reforma = Pausa.indefinida("reforma");
        Estabelecimento antes = lojaCom(new Disponibilidade(Map.of(), reforma));
        assertThat(antes.getDisponibilidade().horarioDeFuncionamento()).isEmpty();

        Estabelecimento depois = antes.comHorario(
                horario(DayOfWeek.FRIDAY, List.of(Faixa.de("18:00", "23:00"))));

        assertThat(depois.getDisponibilidade().horarioDeFuncionamento())
                .isEqualTo(Map.of(DayOfWeek.FRIDAY, List.of(Faixa.de("18:00", "23:00"))));
        assertThat(depois.getDisponibilidade().pausa()).isEqualTo(reforma);
        assertThat(depois.getId()).isEqualTo(antes.getId());
    }

    @Test
    @DisplayName("2 · o mapa vazio volta ao semHorario() — o estado nomeado, não 'um mapa vazio'")
    void mapa_vazio_e_o_semHorario() {
        Estabelecimento comHorario = lojaCom(LojaDeTeste.disponibilidade());

        Estabelecimento depois = comHorario.comHorario(Map.of());

        assertThat(depois.getDisponibilidade()).isEqualTo(Disponibilidade.semHorario());
    }

    @Test
    @DisplayName("3 · substituir é substituir: o dia que saiu não está lá")
    void substituir_e_substituir() {
        Estabelecimento loja = lojaCom(Disponibilidade.semHorario()).comHorario(horario(
                DayOfWeek.MONDAY, List.of(Faixa.de("11:00", "14:00")),
                DayOfWeek.TUESDAY, List.of(Faixa.de("18:00", "23:00"))));

        Estabelecimento depois = loja.comHorario(
                horario(DayOfWeek.TUESDAY, List.of(Faixa.de("19:00", "22:00"))));

        assertThat(depois.getDisponibilidade().horarioDeFuncionamento())
                .containsOnlyKeys(DayOfWeek.TUESDAY)
                .doesNotContainKey(DayOfWeek.MONDAY);
        assertThat(depois.getDisponibilidade().faixasDe(DayOfWeek.TUESDAY))
                .containsExactly(Faixa.de("19:00", "22:00"));
    }

    @Test
    @DisplayName("4 · duas chamadas iguais deixam a loja igual")
    void idempotente() {
        Estabelecimento loja = lojaCom(Disponibilidade.semHorario());
        Map<DayOfWeek, List<Faixa>> sexta = horario(DayOfWeek.FRIDAY, List.of(Faixa.de("18:00", "23:00")));

        Estabelecimento uma = loja.comHorario(sexta);
        Estabelecimento duas = uma.comHorario(sexta);

        assertThat(duas.getDisponibilidade()).isEqualTo(uma.getDisponibilidade());
        assertThat(duas.getId()).isEqualTo(uma.getId());
        assertThat(duas.getAreasDeEntrega()).isEqualTo(uma.getAreasDeEntrega());
    }

    @Test
    @DisplayName("5 · dia com lista vazia é descartado na normalização")
    void dia_vazio_e_descartado() {
        Estabelecimento depois = lojaCom(Disponibilidade.semHorario()).comHorario(horario(
                DayOfWeek.MONDAY, List.of(),
                DayOfWeek.FRIDAY, List.of(Faixa.de("18:00", "23:00"))));

        assertThat(depois.getDisponibilidade().horarioDeFuncionamento()).containsOnlyKeys(DayOfWeek.FRIDAY);
    }

    // ── A.1 · as duas recusas que existem — e só elas ───────────────────────

    @Test
    @DisplayName("6 · faixa com início igual ao fim é recusada")
    void inicio_igual_ao_fim() {
        assertThatThrownBy(() -> Faixa.de("10:00", "10:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("faixa de início igual ao fim é ambígua — nem turno vazio nem "
                        + "vinte e quatro horas: 10:00");
    }

    @Test
    @DisplayName("7 · a mesma faixa repetida no mesmo dia é recusada")
    void faixa_repetida() {
        Estabelecimento loja = lojaCom(Disponibilidade.semHorario());

        assertThatThrownBy(() -> loja.comHorario(horario(DayOfWeek.MONDAY,
                List.of(Faixa.de("11:00", "14:00"), Faixa.de("11:00", "14:00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("faixa repetida em MONDAY: [11:00–14:00, 11:00–14:00]");
    }

    // ── A.2 · o que o domínio permite de propósito ──────────────────────────

    /** "Sobreposição é redundância, não contradição" — sem este caso, alguém a "conserta". */
    @Test
    @DisplayName("8 · faixas que se sobrepõem no mesmo dia são aceitas")
    void sobreposicao_e_aceita() {
        Estabelecimento depois = lojaCom(Disponibilidade.semHorario()).comHorario(horario(
                DayOfWeek.MONDAY, List.of(Faixa.de("11:00", "15:00"), Faixa.de("14:00", "18:00"))));

        assertThat(depois.getDisponibilidade().faixasDe(DayOfWeek.MONDAY))
                .containsExactly(Faixa.de("11:00", "15:00"), Faixa.de("14:00", "18:00"));
    }

    /** É a faixa que cruza a meia-noite, e pertence ao dia de início. */
    @Test
    @DisplayName("9 · fim antes do início é aceito")
    void fim_antes_do_inicio_e_aceito() {
        Estabelecimento depois = lojaCom(Disponibilidade.semHorario()).comHorario(
                horario(DayOfWeek.TUESDAY, List.of(Faixa.de("18:00", "02:00"))));

        assertThat(depois.getDisponibilidade().faixasDe(DayOfWeek.TUESDAY))
                .containsExactly(Faixa.de("18:00", "02:00"));
        assertThat(depois.estaAberta(LojaDeTeste.emSaoPaulo("2026-10-14T00:30"))).isTrue();
    }
}
