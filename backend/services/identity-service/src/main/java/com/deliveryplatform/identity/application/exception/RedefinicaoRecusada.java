package com.deliveryplatform.identity.application.exception;

/**
 * A recuperação de senha recusada — código errado, expirado, tentativas estouradas, nenhum código
 * pedido ou telefone sem conta. <b>Uma recusa só, sem dizer qual</b>, pelo mesmo motivo da
 * {@link CadastroRecusado}: a diferença entre as respostas diria quais telefones têm conta.
 */
public class RedefinicaoRecusada extends RuntimeException {

    public RedefinicaoRecusada() {
        super("redefinição de senha recusada");
    }
}
