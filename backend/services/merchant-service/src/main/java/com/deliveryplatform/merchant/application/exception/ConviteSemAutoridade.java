package com.deliveryplatform.merchant.application.exception;

/**
 * A A2 no aceite: <b>quem convidou não pode mais conceder o que ofereceu</b> — perdeu a
 * permissão, foi suspenso ou saiu (B2). 409 (ADR-069 §5).
 *
 * <p>O domínio lança isto com as mesmas exceções da emissão — {@code SemAutoridadeSobreMembro}
 * e {@code PermissaoNaoPossuida} —, e lá elas querem dizer outra coisa: quem pede está
 * oferecendo o que não tem, e tem o que corrigir. Aqui quem aceita não tem o que corrigir;
 * o que mudou foi o estado de outra pessoa. Por isso o caso de uso do aceite as traduz
 * nesta, e o domínio não muda.
 *
 * <p><b>Distinta do 404 do convite, e sem custo de M7</b>: ela só acontece depois de o
 * token conferir, então não conta nada a quem está adivinhando.
 */
public class ConviteSemAutoridade extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConviteSemAutoridade(Throwable causa) {
        super("quem convidou não pode mais conceder o que ofereceu: " + causa.getMessage(), causa);
    }
}
