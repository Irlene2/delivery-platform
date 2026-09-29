package com.deliveryplatform.catalog.infrastructure.messaging.listener;

import com.deliveryplatform.catalog.infrastructure.cache.CacheDaAutorizacao;
import com.deliveryplatform.catalog.infrastructure.cache.ChaveDeAutorizacao;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

/**
 * O <b>primeiro consumidor de evento do repositório</b>, e o padrão que os
 * outros sete vão copiar.
 *
 * <h2>Ele não guarda o que já processou, e isso é exceção escrita</h2>
 *
 * <p>A invariante 7 do {@code CLAUDE.md} diz: <i>"Quem publica precisa de
 * outbox. Quem consome precisa de {@code processed_messages}. Sem exceção."</i>
 * Este consumidor é a exceção, e ela nasce escrita na ADR-048 porque <b>a
 * invariante, aplicada aqui, produziria exatamente a falha que a ADR-011 manda
 * evitar</b>:
 *
 * <ul>
 *   <li>o cache é <b>por instância</b>, e por isso cada instância tem de
 *       processar cada mensagem;</li>
 *   <li>uma {@code processed_messages} no banco do serviço é <b>compartilhada
 *       entre as instâncias</b>: a segunda veria o {@code eventId} que a
 *       primeira gravou e <b>pularia a própria invalidação</b>, ficando com
 *       permissão velha e sem erro nenhum;</li>
 *   <li>e não há <i>"transação do efeito"</i> (ADR-021) da qual participar — o
 *       efeito é memória.</li>
 * </ul>
 *
 * <p>O critério da exceção é estreito e está na ADR-048: <b>efeito puramente em
 * memória, do processo, e naturalmente idempotente</b>. Remover uma entrada de
 * cache duas vezes é o mesmo que remover uma. Nenhum outro consumidor deste
 * sistema se encaixa — os do {@code order}, do {@code settlement} e do
 * {@code delivery} escrevem em banco, e para esses a invariante vale inteira.
 *
 * <h2>Pelo mesmo motivo, ele não lê {@code eventId} nem {@code occurredAt}</h2>
 *
 * <p>O {@code contracts/eventos.md} impõe duas cláusulas ao consumidor:
 * idempotência por {@code eventId} (cláusula 3) e descarte de evento velho por
 * {@code occurredAt} (cláusula 4). <b>As duas ficam vazias para quem só
 * invalida:</b> remover é idempotente, e remover uma entrada por causa de um
 * evento antigo apenas força uma consulta a mais. Elas voltam a valer, inteiras,
 * para o primeiro consumidor que <i>escrever</i> alguma coisa.
 *
 * <h2>Nunca recusa a mensagem. Quando falha, esquece tudo</h2>
 *
 * <p>Não há retentativa e não há fila morta, e a ADR-026 ganha exceção escrita
 * pelo mesmo motivo que a invariante 7:
 *
 * <ul>
 *   <li>a fila é <b>temporária</b> — uma fila morta ligada a ela morreria
 *       junto, e reprocessar para uma instância que já não existe não é
 *       reprocessar;</li>
 *   <li>reter uma mensagem por 21 segundos (o teto da ADR-026) é reter uma
 *       <b>revogação</b> por 21 segundos;</li>
 *   <li>e existe uma resposta melhor do que repetir: <b>esquecer o cache
 *       inteiro</b>. Se não se consegue remover uma entrada, remover todas custa
 *       algumas consultas ao {@code merchant} e não deixa <i>ninguém</i> com
 *       acesso que já foi retirado.</li>
 * </ul>
 *
 * <p>Falhar para o lado seguro, aqui, é <b>esquecer</b>. E a mensagem é sempre
 * confirmada: devolvê-la à fila faria a mesma falha acontecer de novo, em laço,
 * enquanto o cache já está vazio e correto.
 */
@Component
public class OuvinteDeVinculoAlterado {

    private static final Logger log = LoggerFactory.getLogger(OuvinteDeVinculoAlterado.class);

    private final CacheDaAutorizacao cache;
    private final ObjectMapper json;

    public OuvinteDeVinculoAlterado(CacheDaAutorizacao cache, ObjectMapper json) {
        this.cache = cache;
        this.json = json;
    }

    /**
     * A fila vem do bean, porque o nome dela é gerado no arranque, por instância.
     *
     * <p>{@code AnonymousQueue} não tem nome fixo — é isso que a torna exclusiva
     * por instância. O SpEL resolve o nome real no arranque.
     *
     * <p>O {@code Channel} e a etiqueta de entrega estão na assinatura porque o
     * {@code application.yml} deste serviço declara
     * {@code acknowledge-mode: manual}. Com ele, quem não confirma deixa a
     * mensagem pendurada para sempre.
     */
    @RabbitListener(queues = "#{filaDeVinculo.name}")
    public void aoReceber(String corpo,
                          Channel canal,
                          @Header(AmqpHeaders.DELIVERY_TAG) long etiqueta) {
        try {
            aplicar(corpo);
        } catch (RuntimeException naoDeuParaAplicar) {
            // Sem o corpo no registro: o envelope pode ter dado pessoal, e a
            // regra do CLAUDE.md sobre log vale para a mensagem como vale para
            // a linha de outbox. O que fica é o suficiente para investigar.
            log.warn("não foi possível aplicar um VinculoAlterado: {}",
                    naoDeuParaAplicar.toString());
            cache.esvaziar("falha ao aplicar uma invalidação");
        } finally {
            confirmar(canal, etiqueta);
        }
    }

    /**
     * Lê os dois campos de que a invalidação precisa, e ignora o resto.
     *
     * <p>Campo novo no envelope ou no payload não quebra nada: o que não é lido
     * não existe para este consumidor. É a tolerância mais barata que há, e é a
     * que o {@code merchant} precisa para poder acrescentar campo sem versão
     * nova.
     *
     * <p><b>Mas valor desconhecido em enum não é o mesmo caso.</b> A ADR-027 §2
     * — <i>"Valor novo em enum é incompatível por padrão"</i> — não se aplica
     * aqui porque este consumidor <b>não lê enum nenhum</b>: nem {@code papel},
     * nem {@code estado}, nem {@code permissoes}. Ele só precisa saber
     * <i>de quem</i> e <i>de qual loja</i> o vínculo mudou.
     */
    private void aplicar(String corpo) {
        JsonNode envelope = json.readTree(corpo);
        JsonNode payload = envelope.path("payload");

        UUID usuarioId = uuidObrigatorio(payload, "usuarioId");
        UUID estabelecimentoId = uuidObrigatorio(payload, "estabelecimentoId");

        cache.invalidar(ChaveDeAutorizacao.doEvento(usuarioId, estabelecimentoId));

        // Sem identificador de pessoa no registro (CLAUDE.md). A loja basta
        // para correlacionar com o que quer que esteja acontecendo nela.
        log.debug("autorização invalidada por evento, loja {}", estabelecimentoId);
    }

    private static UUID uuidObrigatorio(JsonNode payload, String campo) {
        JsonNode valor = payload.path(campo);
        if (valor.isMissingNode() || valor.isNull() || !valor.isString()) {
            throw new IllegalArgumentException("payload sem " + campo);
        }
        return UUID.fromString(valor.asString());
    }

    /**
     * Confirma sempre — inclusive quando falhou.
     *
     * <p>{@code basicAck} com {@code multiple = false}: confirma <b>esta</b>
     * mensagem. Com {@code true} confirmaria todas as anteriores não
     * confirmadas, e numa falha isso esconderia mensagens que ninguém aplicou.
     *
     * <p>Se o próprio {@code ack} falhar, o canal já está quebrado e o contêiner
     * vai reconectar — e a reconexão esvazia o cache, que é a resposta certa.
     */
    private static void confirmar(Channel canal, long etiqueta) {
        try {
            canal.basicAck(etiqueta, false);
        } catch (IOException canalQuebrado) {
            log.warn("não foi possível confirmar a mensagem: {}", canalQuebrado.toString());
        }
    }
}
