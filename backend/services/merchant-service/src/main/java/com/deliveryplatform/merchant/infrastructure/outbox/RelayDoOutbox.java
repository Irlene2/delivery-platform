package com.deliveryplatform.merchant.infrastructure.outbox;

import com.deliveryplatform.merchant.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Tira do outbox o que ainda não saiu e publica.
 *
 * <p><b>O que ele garante e o que ele não garante.</b> Garante que nada que foi
 * gravado deixa de ser publicado: enquanto {@code publicado_em} for nulo, a
 * linha volta no próximo lote. Não garante que nada seja publicado duas vezes —
 * publicar no broker e marcar a linha não são atômicos entre si, e se a
 * publicação vai e o commit não, a mensagem sai de novo. A entrega é <b>pelo
 * menos uma vez</b>, está escrita assim na ADR-043 §3, e o contrato obriga o
 * consumidor a ser idempotente por {@code eventoId}.
 *
 * <p><b>A falha de um evento não segura a fila.</b> Cada linha é publicada e
 * marcada dentro do laço; uma que estoure tem o motivo registrado e as outras do
 * lote seguem. Sem isso, uma mensagem malformada bloquearia todas as posteriores
 * — que é o modo de falha clássico de outbox e o mais difícil de diagnosticar,
 * porque o sintoma aparece num evento que não tem defeito nenhum.
 *
 * <p>Desligável por propriedade. Os testes que não têm nada a ver com publicação
 * desligam o relay em vez de conviver com um agendador rodando por baixo — um
 * {@code @Scheduled} ativo é a fonte mais comum de teste que passa noventa por
 * cento das vezes.
 */
@Component
@ConditionalOnProperty(name = "delivery.outbox.habilitado", havingValue = "true", matchIfMissing = true)
public class RelayDoOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelayDoOutbox.class);

    private final OutboxSpringDataRepository linhas;
    private final RabbitTemplate rabbit;
    private final OutboxProperties propriedades;
    private final Clock relogio;

    public RelayDoOutbox(OutboxSpringDataRepository linhas,
                         RabbitTemplate rabbit,
                         OutboxProperties propriedades,
                         Clock relogio) {
        this.linhas = linhas;
        this.rabbit = rabbit;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    /**
     * @return quantas linhas foram publicadas <b>e confirmadas pelo broker</b>
     *         neste lote — só para o teste poder afirmar o efeito sem dormir
     *         esperando o agendador.
     */
    @Scheduled(
            fixedDelayString = "${delivery.outbox.intervalo-ms:1000}",
            initialDelayString = "${delivery.outbox.atraso-inicial-ms:2000}")
    @Transactional
    public int publicarPendentes() {
        List<OutboxJpaEntity> lote = linhas.travarLotePendente(propriedades.tamanhoDoLote());
        if (lote.isEmpty()) {
            return 0;
        }

        Instant agora = Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);

        // Manda tudo primeiro, espera depois.
        //
        // Esperar a confirmação de cada mensagem antes de mandar a próxima faria
        // o pior caso do lote ser cem vezes o tempo limite — e este método está
        // dentro de uma transação, segurando os cadeados das cem linhas. Mandando
        // primeiro, o pior caso é UM tempo limite para o lote inteiro.
        List<Envio> enviados = new ArrayList<>(lote.size());
        for (OutboxJpaEntity linha : lote) {
            CorrelationData correlacao = new CorrelationData(linha.getId().toString());
            try {
                rabbit.send(propriedades.exchange(), linha.getChaveDeRota(),
                        mensagem(linha), correlacao);
                enviados.add(new Envio(linha, correlacao));
            } catch (RuntimeException naoSaiu) {
                linha.falhou(naoSaiu.getClass().getSimpleName() + ": " + naoSaiu.getMessage());
                log.warn("outbox: falha ao publicar {} id={} tentativa={}",
                        linha.getTipo(), linha.getId(), linha.getTentativas());
            }
        }

        int publicadas = 0;
        for (Envio envio : enviados) {
            if (confirmado(envio)) {
                envio.linha().publicado(agora);
                publicadas++;
            }
        }
        return publicadas;
    }

    /** Uma linha e a correlação com que ela foi enviada. */
    private record Envio(OutboxJpaEntity linha, CorrelationData correlacao) {
    }

    /**
     * Espera o broker dizer que recebeu — e que havia para onde entregar.
     *
     * <p><b>Isto não existia, e a ausência era o defeito mais caro do outbox.</b>
     * Todos os serviços declaram {@code publisher-confirm-type: correlated} desde
     * a C-B, e ninguém nunca esperou uma confirmação: a linha era marcada como
     * publicada assim que o {@code send} devolvia, ou seja, assim que os bytes
     * saíam pelo socket.
     *
     * <p>E havia uma consequência pior do que "talvez não tenha chegado".
     * <b>Mensagem sem fila de destino é descartada pelo broker em silêncio</b> —
     * o AMQP não avisa, a não ser que se peça, e pedir é o {@code mandatory}.
     * Como nenhuma fila estava ligada à {@code delivery.eventos} até a G-B4,
     * <b>todo {@code VinculoAlteradoV1} publicado desde a C-B foi descartado</b>,
     * e o outbox marcou cada um como entregue.
     *
     * <h2>Os três desfechos</h2>
     *
     * <table>
     *   <tr><th>o que o broker fez</th><th>o que se conclui</th></tr>
     *   <tr><td>{@code ack}, sem devolução</td><td>chegou numa fila: <b>publicado</b></td></tr>
     *   <tr><td>{@code ack}, <b>com</b> devolução</td><td>o broker aceitou e não tinha para onde mandar: <b>pendente</b></td></tr>
     *   <tr><td>{@code nack}, ou nada dentro do prazo</td><td>não se sabe: <b>pendente</b></td></tr>
     * </table>
     *
     * <p>A segunda linha é a que engana: <b>o {@code ack} vem mesmo quando a
     * mensagem é descartada.</b> Confirmação diz "recebi", não "entreguei" — e a
     * diferença entre as duas é exatamente a devolução.
     *
     * <p>Em todos os casos duvidosos a linha fica <b>pendente</b>, e o relay
     * tenta de novo no lote seguinte. A entrega continua sendo pelo menos uma
     * vez (ADR-043 §3): o que muda é que agora ela é pelo menos uma vez de
     * verdade, em vez de no máximo uma.
     */
    private boolean confirmado(Envio envio) {
        OutboxJpaEntity linha = envio.linha();
        try {
            CorrelationData.Confirm confirmacao = envio.correlacao().getFuture()
                    .get(propriedades.tempoDeConfirmacaoMs(), TimeUnit.MILLISECONDS);

            if (!confirmacao.isAck()) {
                linha.falhou("o broker recusou: " + confirmacao.getReason());
            } else if (envio.correlacao().getReturned() != null) {
                // Sem consumidor não há binding, e sem binding a exchange
                // descarta. Marcar como publicada aqui seria o outbox mentindo.
                linha.falhou("sem fila de destino para " + linha.getChaveDeRota());
            } else {
                return true;
            }
        } catch (TimeoutException semResposta) {
            linha.falhou("o broker não confirmou dentro do prazo");
        } catch (ExecutionException falhou) {
            linha.falhou("falha ao confirmar: " + falhou.getCause().getClass().getSimpleName());
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            linha.falhou("interrompido antes da confirmação");
        }

        log.warn("outbox: {} id={} continua pendente, tentativa={} — {}",
                linha.getTipo(), linha.getId(), linha.getTentativas(), linha.getUltimoErro());
        return false;
    }

    private Message mensagem(OutboxJpaEntity linha) {
        return MessageBuilder
                .withBody(linha.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setContentEncoding(StandardCharsets.UTF_8.name())
                // PERSISTENT: o broker grava antes de confirmar. Um outbox que
                // entrega para uma fila volátil trocou a durabilidade do banco
                // pela memória do broker, e passa a garantir o que o banco já
                // garantia sem ele.
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                // O consumidor decide o que fazer sem abrir o corpo, e um
                // roteador pode descartar versão que não conhece.
                .setHeader("tipo", linha.getTipo())
                .setHeader("versao", linha.getVersao())
                // Chave de idempotência também no cabeçalho, além do payload:
                // deduplicar é responsabilidade de infraestrutura do consumidor,
                // e infraestrutura não deveria precisar desserializar domínio.
                .setMessageId(linha.getId().toString())
                .setTimestamp(java.util.Date.from(linha.getOcorridoEm()))
                .build();
    }
}
