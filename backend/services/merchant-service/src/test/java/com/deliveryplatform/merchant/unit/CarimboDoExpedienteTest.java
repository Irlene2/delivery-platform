package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.domain.model.Disponibilidade;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.Faixa;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
import com.deliveryplatform.merchant.domain.model.Pausa;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O carimbo, e sobretudo <b>o carimbo com a loja fechada</b> — a decisão da
 * ADR-049.
 *
 * <p>A F.1 provou o caso da loja <i>aberta</i>: o expediente é o dia operacional
 * do início da faixa, e não do instante. Este arquivo cobre o outro lado, que
 * nenhum documento decidia até 29/09: <b>a Marli abre às 18h e às 10h vê que não
 * tem calabresa.</b>
 *
 * <p>Se alguém trocar a regra da loja fechada por
 * {@code diaOperacional(instante)}, ficam vermelhos três casos — medido na
 * G-C1, com a troca feita de propósito e desfeita: o
 * {@code de_madrugada_depois_de_fechar}, que é o único em que as duas regras
 * discordam para a pizzaria de todo dia; o {@code so_abre_as_segundas}; e o
 * {@code sem_horario_nao_tem_carimbo}, porque o instante sempre tem dia e a
 * loja sem horário deixaria de dar vazio.
 *
 * <p><b>Não usa o relógio.</b> Todo instante é literal, no fuso da loja. Um
 * teste de dia operacional que chame {@code Instant.now()} passa vinte horas por
 * dia e falha nas outras quatro.
 */
class CarimboDoExpedienteTest {

    /** 18:00–02:00, todo dia. A pizzaria que fecha depois da meia-noite. */
    private static final Estabelecimento PIZZARIA = lojaCom(Faixa.de("18:00", "02:00"));

    /** 22:00–06:00, todo dia. O turno que atravessa a hora de corte (ADR-046). */
    private static final Estabelecimento MADRUGADA = lojaCom(Faixa.de("22:00", "06:00"));

    // ── a loja aberta: o que a F.1 já decidiu, e que continua valendo ───────

    @Nested
    @DisplayName("dentro do horário — o expediente em curso")
    class Aberta {

        @Test
        @DisplayName("20:00 de terça, pizzaria aberta desde as 18:00 → terça")
        void durante_o_expediente() {
            assertThat(carimbo(PIZZARIA, "2026-09-29T20:00"))
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }

        @Test
        @DisplayName("00:30 de quarta, ainda no turno que abriu terça 18:00 → terça")
        void depois_da_meia_noite_ainda_e_o_mesmo_turno() {
            assertThat(carimbo(PIZZARIA, "2026-09-30T00:30"))
                    .as("o dia operacional do INÍCIO da faixa, não do instante — "
                            + "pelo instante daria quarta, e a calabresa que acabou "
                            + "às 23h voltaria na abertura de quarta")
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }

        @Test
        @DisplayName("04:30 no turno 22h–06h → o dia em que o turno começou")
        void o_turno_que_atravessa_a_hora_de_corte() {
            assertThat(carimbo(MADRUGADA, "2026-09-30T04:30"))
                    .as("é o defeito que a F.1 consertou no produtor, chegando "
                            + "agora pelo lado de quem carimba")
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }

        @Test
        @DisplayName("pausa não muda o carimbo — ela acontece dentro de um expediente")
        void pausa_nao_muda_nada() {
            // O caso que impede alguém de trocar inicioDaFaixaEm por abertaEm no
            // expedienteParaCarimbo: pausada, a loja não está aberta, e o carimbo
            // cairia na próxima abertura — amanhã.
            Estabelecimento pausada = lojaCom(
                    horarioDe(Faixa.de("18:00", "02:00")),
                    Pausa.ate(LojaDeTeste.emSaoPaulo("2026-09-29T21:00"), "faltou gás"));

            assertThat(carimbo(pausada, "2026-09-29T20:00"))
                    .as("a varredura da F pergunta dentroDoHorario e não estaAberta, "
                            + "pelo mesmo motivo: pausar às 20h e retomar às 20h30 "
                            + "é o mesmo expediente")
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }
    }

    // ── a loja fechada: a decisão desta rodada ──────────────────────────────

    @Nested
    @DisplayName("fora do horário — o expediente da PRÓXIMA abertura")
    class Fechada {

        @Test
        @DisplayName("10:00 de terça, abre às 18:00 → terça. É o caso da Marli")
        void de_manha_antes_de_abrir() {
            assertThat(carimbo(PIZZARIA, "2026-09-29T10:00"))
                    .as("ela diz 'acabou a calabresa' preparando a massa, e o que ela "
                            + "quer é não oferecer hoje à noite. Com o expediente da "
                            + "ÚLTIMA abertura, a abertura das 18h reativaria")
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }

        @Test
        @DisplayName("03:00 de quarta, fechou às 02:00, abre 18:00 → QUARTA, não terça")
        void de_madrugada_depois_de_fechar() {
            assertThat(carimbo(PIZZARIA, "2026-09-30T03:00"))
                    .as("é o único caso em que a regra escolhida difere de "
                            + "diaOperacional(instante), que aqui daria terça — e daria "
                            + "errado: a abertura de quarta traria quarta, quarta é "
                            + "depois de terça, e o item voltaria ao cardápio")
                    .isEqualTo(LocalDate.parse("2026-09-30"));
        }

        @Test
        @DisplayName("meio-dia numa loja 22h–06h → hoje, que é quando ela abre")
        void antes_do_turno_da_madrugada() {
            assertThat(carimbo(MADRUGADA, "2026-09-29T12:00"))
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }

        @Test
        @DisplayName("a próxima abertura pode ser noutra semana")
        void so_abre_as_segundas() {
            Map<DayOfWeek, List<Faixa>> so_segunda = new EnumMap<>(DayOfWeek.class);
            so_segunda.put(DayOfWeek.MONDAY, List.of(Faixa.de("08:00", "18:00")));
            Estabelecimento loja = lojaCom(so_segunda, Pausa.nenhuma());

            assertThat(carimbo(loja, "2026-09-29T12:00"))
                    .as("terça ao meio-dia, e a próxima segunda é 05/10")
                    .isEqualTo(LocalDate.parse("2026-10-05"));
        }

        @Test
        @DisplayName("entre duas aberturas futuras no mesmo dia, vale a mais próxima")
        void a_mais_proxima_vence() {
            Map<DayOfWeek, List<Faixa>> padaria = new EnumMap<>(DayOfWeek.class);
            for (DayOfWeek dia : DayOfWeek.values()) {
                padaria.put(dia, List.of(Faixa.de("06:00", "14:00"), Faixa.de("18:00", "22:00")));
            }
            Estabelecimento loja = lojaCom(padaria, Pausa.nenhuma());

            assertThat(carimbo(loja, "2026-09-29T15:00"))
                    .as("às 15h ela está entre os dois turnos; a próxima é às 18h, "
                            + "do mesmo dia operacional")
                    .isEqualTo(LocalDate.parse("2026-09-29"));
        }
    }

    // ── a loja que não abre ─────────────────────────────────────────────────

    @Test
    @DisplayName("loja sem horário não tem expediente — vazio, e não uma data qualquer")
    void sem_horario_nao_tem_carimbo() {
        assertThat(LojaDeTeste.semHorario()
                .expedienteParaCarimbo(LojaDeTeste.emSaoPaulo("2026-09-29T12:00")))
                .as("horário vazio é válido e significa 'nunca abre por horário'. "
                        + "Uma data inventada aqui seria comparada, amanhã, com a de "
                        + "um evento de abertura que nunca chega")
                .isEmpty();
    }

    // ── a busca sozinha, sem o agregado em volta ────────────────────────────

    @Test
    @DisplayName("proximaAberturaApos devolve o INSTANTE da abertura, não a data")
    void a_proxima_abertura_e_um_instante() {
        Disponibilidade horario = new Disponibilidade(
                horarioDe(Faixa.de("18:00", "02:00")), Pausa.nenhuma());

        assertThat(horario.proximaAberturaApos(
                LojaDeTeste.emSaoPaulo("2026-09-29T10:00"), FusoHorario.PADRAO))
                .as("quem converte em dia operacional é o Estabelecimento, que tem "
                        + "o fuso — esta classe devolve o instante e não sabe de corte")
                .contains(LojaDeTeste.emSaoPaulo("2026-09-29T18:00"));
    }

    @Test
    @DisplayName("com a loja aberta, a próxima abertura é a de DEPOIS — não a atual")
    void aberta_agora_a_proxima_e_a_seguinte() {
        Disponibilidade horario = new Disponibilidade(
                horarioDe(Faixa.de("18:00", "02:00")), Pausa.nenhuma());

        assertThat(horario.proximaAberturaApos(
                LojaDeTeste.emSaoPaulo("2026-09-29T20:00"), FusoHorario.PADRAO))
                .as("às 20h a faixa das 18h já começou; a próxima é amanhã. "
                        + "É por isso que o Estabelecimento pergunta primeiro pelo "
                        + "inicioDaFaixaEm e só cai aqui quando ele vem vazio")
                .contains(LojaDeTeste.emSaoPaulo("2026-09-30T18:00"));
    }

    // ── montagem ────────────────────────────────────────────────────────────

    private static LocalDate carimbo(Estabelecimento loja, String dataHoraLocal) {
        return loja.expedienteParaCarimbo(LojaDeTeste.emSaoPaulo(dataHoraLocal)).orElseThrow();
    }

    private static Map<DayOfWeek, List<Faixa>> horarioDe(Faixa faixa) {
        Map<DayOfWeek, List<Faixa>> todos = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dia : DayOfWeek.values()) {
            todos.put(dia, List.of(faixa));
        }
        return todos;
    }

    private static Estabelecimento lojaCom(Faixa faixa) {
        return lojaCom(horarioDe(faixa), Pausa.nenhuma());
    }

    private static Estabelecimento lojaCom(Map<DayOfWeek, List<Faixa>> horario, Pausa pausa) {
        return Estabelecimento.novo(
                LojaDeTeste.identificacao(FusoHorario.PADRAO),
                LojaDeTeste.operacao(),
                LojaDeTeste.troco(),
                new Disponibilidade(horario, pausa),
                LojaDeTeste.areas());
    }
}
