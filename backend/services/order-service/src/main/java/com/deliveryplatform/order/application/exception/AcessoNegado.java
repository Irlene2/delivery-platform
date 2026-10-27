package com.deliveryplatform.order.application.exception;

/**
 * Uma recusa só para "loja inexistente", "sem vínculo", "vínculo suspenso", "sem a permissão"
 * e "merchant fora do ar" — a mesma regra do {@code catalog}, pelo mesmo motivo: respostas
 * diferentes fariam da rota um scanner de lojas.
 */
public class AcessoNegado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AcessoNegado() {
        super("sem acesso a este estabelecimento");
    }
}
