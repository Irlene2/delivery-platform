package com.deliveryplatform.identity.application.port.in;

import java.util.Optional;

/**
 * Primeiro passo do cadastro (ADR-042 §1).
 *
 * <p><b>A borda não devolve o código, e é assim de propósito.</b> Ele é entregue pelo operador,
 * que o lê da tabela. Devolvê-lo na resposta transformaria a verificação em formalidade:
 * qualquer um provaria posse de qualquer número lendo a própria resposta.
 *
 * <p><b>O caso de uso devolve o código gerado</b> (emenda de 01/11/2026 à ADR-042) só para a
 * borda poder mostrá-lo <b>quando a bandeira de desenvolvimento estiver ligada</b> — na máquina
 * de quem desenvolve e na demonstração, onde não há canal que entregue SMS. Vazio quando o
 * telefone já tem conta.
 *
 * <p>Também não diz, na resposta normal, se o telefone já tem conta. Telefone cadastrado
 * recebe a mesma resposta e não gera código nenhum — ADR-042 §5.
 */
public interface SolicitarCodigoDeVerificacao {

    Optional<String> solicitar(String telefoneBruto);
}
