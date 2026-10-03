package com.deliveryplatform.merchant.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Qual expediente carimbar, agora, nesta loja.
 *
 * <h2>Quem pergunta, e por quê</h2>
 *
 * <p>O {@code catalog}, quando a Marli diz "acabou a calabresa". O
 * {@code ESGOTADO_HOJE} nasce com um carimbo — {@code marcadoEm} e
 * {@code expedienteDeReferencia} —, e sem o segundo ele nunca reativa: o
 * construtor da {@code Disponibilidade} do catálogo recusa o par pela metade.
 *
 * <p><b>E o catálogo não pode calcular.</b> A ADR-046 §6 escreve que o dia
 * operacional <i>"continua sendo o único lugar que a calcula"</i>, no
 * {@code merchant}, porque é aqui que mora o {@code fusoHorario}. A classe
 * {@code Disponibilidade} do catálogo não tem fuso nenhum, e isso é estrutural
 * de propósito.
 *
 * <h2>Três respostas, e nenhuma é nula</h2>
 *
 * <ul>
 *   <li><b>A loja está dentro do horário</b> — devolve o dia operacional do
 *       <i>início da faixa</i> que a contém. É o mesmo que a varredura da F.1
 *       usa, e pelo mesmo motivo;</li>
 *   <li><b>a loja está fechada</b> — devolve o dia operacional da
 *       <b>próxima abertura</b>. É a decisão da ADR-049, e é o que faz a
 *       calabresa que acabou às 10h continuar acabada às 18h;</li>
 *   <li><b>a loja não abre por horário</b> — estoura
 *       {@link com.deliveryplatform.merchant.application.exception.SemExpedientePorHorario},
 *       que vira 409. Não há expediente para carimbar, e devolver uma data
 *       inventada seria pior do que recusar.</li>
 * </ul>
 *
 * <p>O {@code usuarioId} não é um parâmetro de leitura: é a prova. A invariante
 * 9 manda confrontar o identificador da URL com o usuário autenticado, e aqui
 * esse confronto é <b>vínculo ativo</b> — sem permissão específica, porque a
 * resposta é uma data derivada do horário de funcionamento, e o chamador que a
 * usa já confere a permissão do ato que ele vai praticar.
 */
public interface ConsultarExpedienteCorrente {

    LocalDate de(UUID estabelecimentoId, UUID usuarioId);
}
