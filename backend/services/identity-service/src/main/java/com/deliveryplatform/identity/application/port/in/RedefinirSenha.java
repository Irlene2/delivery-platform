package com.deliveryplatform.identity.application.port.in;

import java.util.Optional;

/**
 * Esqueci minha senha (02/11/2026): o mesmo código de verificação do cadastro, ao contrário —
 * só existe para telefone que <b>já tem</b> conta.
 */
public interface RedefinirSenha {

    /**
     * Cria o código para o telefone, se ele tiver conta. Telefone sem conta não ganha código, e a
     * resposta para quem pediu é a mesma (ADR-042 §5).
     *
     * @return o código criado, para a bandeira de desenvolvimento mostrar na tela; vazio quando
     *     o telefone não tem conta
     */
    Optional<String> solicitarCodigo(String telefoneBruto);

    /** Troca a senha de quem provou que atende o telefone. */
    void redefinir(String telefoneBruto, String codigo, String novaSenha);
}
