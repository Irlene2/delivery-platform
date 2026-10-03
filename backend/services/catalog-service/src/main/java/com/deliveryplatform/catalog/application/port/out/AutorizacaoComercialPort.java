package com.deliveryplatform.catalog.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * A porta que a ADR-011 desenhou em agosto e que ninguém tinha implementado.
 *
 * <p>Assinatura final, depois da emenda da ADR-045:
 *
 * <pre>
 * Optional&lt;ContextoDeAcesso&gt; contexto(UUID estabelecimentoId);
 * </pre>
 *
 * <p><b>Não recebe {@code usuarioId}, e a ausência é a decisão.</b> O chamador
 * não <i>afirma</i> quem é o usuário: ele <i>prova</i>, encaminhando o token que
 * recebeu. Quem é o portador sai do token, dentro do adaptador — e é por isso
 * que o caso de uso não precisa conhecê-lo, o que a ADR-038 pede já no título:
 * <i>"o caso de uso não conhece o token"</i>.
 *
 * <h2>Vazio e exceção significam coisas diferentes, e os dois negam</h2>
 *
 * <table>
 *   <tr><th>o que aconteceu</th><th>o que a porta faz</th></tr>
 *   <tr>
 *     <td>o {@code merchant} respondeu <b>403</b>: não há vínculo ativo</td>
 *     <td>{@link Optional#empty()} — <b>é uma resposta</b>. O produtor sabe e
 *         disse; um dia isto vai poder ser cacheado por 10 s</td>
 *   </tr>
 *   <tr>
 *     <td>tempo esgotado, 5xx, conexão recusada</td>
 *     <td>{@link AutorizacaoIndisponivel} — <b>não é resposta</b>, é ausência
 *         de resposta, e nunca poderá ser cacheada</td>
 *   </tr>
 * </table>
 *
 * <p>Os dois caminhos terminam em 403 para quem chamou o {@code catalog}: falha
 * fechada, sem janela de graça, como a ADR-011 exige. <b>O que muda é o que se
 * pode guardar</b> — e é por isso que a diferença existe antes de haver cache.
 * Enfiar as duas no mesmo {@code Optional.empty()} agora garantiria que, no dia
 * em que o cache entrasse, uma queda de dois segundos do {@code merchant} viraria
 * dez segundos de recusa para todo mundo.
 *
 * <h2>Sem cache nesta rodada, e não é esquecimento</h2>
 *
 * <p>A ADR-011 pede Caffeine em processo, 60 s positivo e 10 s negativo,
 * invalidado por {@code VinculoAlteradoV1}. O consumidor desse evento é a G-B4.
 * A ADR-043 é explícita: <i>"o cache sem o evento é um cache que não invalida, e
 * um cache de autorização que não invalida é uma falha de segurança com nome de
 * otimização"</i>.
 *
 * <p>E há um argumento que fecha a questão: a ADR-011 aceita os 60 s como
 * <i>"o pior caso de acesso indevido depois de uma revogação cujo evento se
 * perdeu"</i>. Sem consumidor, <b>toda</b> revogação é esse pior caso — o que
 * era exceção tolerada vira o comportamento normal. O cache entra na G-B4,
 * junto com o evento que o invalida.
 */
public interface AutorizacaoComercialPort {

    /** Vazio quando não há vínculo ativo — nunca nulo, nunca exceção de "não achei". */
    Optional<ContextoDeAcesso> contexto(UUID estabelecimentoId);
}
