package com.deliveryplatform.merchant.application.exception;

/**
 * O identificador não é de loja nenhuma — só na vitrine, que é pública.
 *
 * <p><b>404, e a ADR-056 não é contrariada:</b> lá, o 403 único existe para que um portador de
 * token não descubra quais lojas existem a partir de rotas que exigem vínculo. A vitrine é o
 * link que a loja divulga para qualquer pessoa, e o identificador é um UUID aleatório — quem o
 * tem, recebeu da loja.
 */
public class LojaNaoEncontrada extends RuntimeException {

    public LojaNaoEncontrada() {
        super("nenhuma loja com este identificador");
    }
}
