package com.deliveryplatform.merchant.application.exception;

import java.util.Objects;

/**
 * O domínio recusou o horário que o corpo descrevia — 400 (ADR-073 §4).
 *
 * <p>No molde do {@link CadastroDeLojaRecusado}: o domínio recusa com
 * {@code IllegalArgumentException}, e um tratador global dessa exceção faria de todo
 * defeito do serviço um erro do cliente. Então a tradução nasce em <b>um</b> ponto só,
 * o {@code DefinirHorarioService}, em volta das duas construções que podem recusar.
 *
 * <p><b>Carrega um {@link Motivo}, e não uma frase</b> (W-E): quem escreve o
 * {@code detail} é o tratador. A mensagem do domínio fica na <b>causa</b>, para o log e
 * para os testes.
 */
public class HorarioRecusado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * As duas recusas que o domínio tem, e só elas — medido na E-4. Sobreposição de
     * faixas e faixa que cruza a meia-noite são <b>aceitas</b> de propósito.
     */
    public enum Motivo {
        /** {@code Faixa}: início igual ao fim — nem turno vazio nem vinte e quatro horas. */
        INICIO_IGUAL_AO_FIM,
        /** {@code Disponibilidade}: a mesma faixa duas vezes no mesmo dia. */
        FAIXA_REPETIDA
    }

    private final Motivo motivo;

    public HorarioRecusado(Motivo motivo, Throwable causa) {
        super(Objects.requireNonNull(motivo, "motivo") + ": " + causa.getMessage(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
