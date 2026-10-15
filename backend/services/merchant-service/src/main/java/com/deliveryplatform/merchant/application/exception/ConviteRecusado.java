package com.deliveryplatform.merchant.application.exception;

import java.util.Objects;

/**
 * O pedido de convite que o agregado recusou — 400 (ADR-069).
 *
 * <p>No padrão do {@link CadastroDeLojaRecusado}: nasce de <b>um</b> ponto só, a emissão no
 * {@code GerenciarEquipeService.convidar}, e carrega um {@link Motivo} e não uma frase —
 * quem escreve o {@code detail} é o tratador. A mensagem do domínio fica na causa, para o
 * log.
 *
 * <p><b>Por que tipo próprio, e não um tratador das exceções do domínio.</b> A
 * {@code PermissaoNaoPossuida} significa coisas diferentes conforme quem a lança: na
 * emissão, quem pede está oferecendo o que não tem, e é um pedido a corrigir (400); no
 * aceite, quem convidou perdeu a permissão depois, e quem aceita não tem o que corrigir
 * (409, {@link ConviteSemAutoridade}). Um tratador do tipo do domínio não saberia qual dos
 * dois responder.
 */
public class ConviteRecusado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        /** {@code Telefone.de}: o número não é um telefone. */
        TELEFONE,
        /** A2 na emissão: oferecer uma permissão que quem convida não tem. */
        PERMISSAO_QUE_NAO_TEM
    }

    private final Motivo motivo;

    public ConviteRecusado(Motivo motivo, Throwable causa) {
        super(Objects.requireNonNull(motivo, "motivo") + ": " + causa.getMessage(), causa);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
