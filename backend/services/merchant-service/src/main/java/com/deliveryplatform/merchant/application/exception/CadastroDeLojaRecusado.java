package com.deliveryplatform.merchant.application.exception;

import java.util.Objects;

/**
 * O agregado recusou a loja que o corpo descrevia — 400 (ADR-060).
 *
 * <p>Nasce de <b>um</b> ponto só: a construção do agregado no
 * {@code CriarEstabelecimentoService}. É por isso que existe uma exceção própria, e
 * não um tratador de {@code IllegalArgumentException}: um tratador global
 * transformaria em 400 todo defeito do serviço que lançasse o mesmo tipo, e
 * esconderia bug como erro do cliente.
 *
 * <p><b>Carrega um {@link Motivo}, e não uma frase</b> (W-E). Quem escreve o
 * {@code detail} é o tratador, por motivo — a mensagem do agregado tem código de
 * invariante e, às vezes, o valor que a pessoa digitou, e a tela mostra o
 * {@code detail} como veio (ADR-055). A mensagem do agregado fica na <b>causa</b>,
 * para o log e para os testes.
 */
public class CadastroDeLojaRecusado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * O que a construção do agregado pode recusar, a partir de um corpo que passou
     * pelo Bean Validation. É o censo do {@code construir(...)}: cada parte do agregado
     * e o que ela lança.
     */
    public enum Motivo {
        /** {@code Documento}: nem 11 dígitos (CPF) nem 14 (CNPJ). */
        DOCUMENTO,
        /** {@code Telefone.de}. */
        TELEFONE,
        /** {@code FusoHorario.de}: fora do conjunto brasileiro (M16). */
        FUSO_HORARIO,
        /** {@code Identificacao}: nome, endereço ou bairro em branco. */
        TEXTO_OBRIGATORIO,
        /** {@code Operacao}: nenhuma modalidade, ou modalidade sem forma de pagar (M12). */
        MODALIDADE_SEM_FORMA_DE_PAGAR,
        /** {@code PoliticaDeTroco}: fundo máximo negativo. */
        FUNDO_DE_TROCO_NEGATIVO
    }

    private final Motivo motivo;

    public CadastroDeLojaRecusado(Motivo motivo, Throwable causa) {
        super(Objects.requireNonNull(motivo, "motivo") + ": " + causa.getMessage(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
