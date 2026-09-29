package com.deliveryplatform.catalog.infrastructure.messaging;

import com.deliveryplatform.catalog.config.EscutaProperties;
import com.deliveryplatform.catalog.infrastructure.cache.CacheDaAutorizacao;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O que o {@code catalog} declara no broker — e aqui se resolve a contradição
 * que estava escrita em dois lugares do repositório.
 *
 * <h2>A exchange é declarada de novo, e isso é correto</h2>
 *
 * <p>O {@code merchant} declara <i>"a exchange, e nada mais"</i>, deixando fila
 * e binding para quem consome. Mas o consumidor não pode <b>depender</b> de o
 * produtor ter subido antes: ligar uma fila a uma exchange que ainda não existe
 * falha com {@code NOT_FOUND}, e o {@code catalog} ficaria sem escutar até
 * alguém reiniciá-lo na ordem certa.
 *
 * <p>Declarar duas vezes é inofensivo <b>se os argumentos forem iguais</b> —
 * o AMQP recusa redeclaração divergente. Por isso os mesmos três: nome vindo da
 * propriedade, durável, sem auto-delete. <b>Se estes três divergirem do
 * {@code merchant}, o serviço não sobe</b>, e é essa a rede.
 *
 * <h2>{@code topic} com fila exclusiva, e a ADR-011 dizia {@code fanout}</h2>
 *
 * <p>A ADR-011 escreve: <i>"O evento vai para um exchange fanout, e cada
 * instância se liga a ele com uma fila exclusiva e temporária, não a uma fila
 * compartilhada"</i>. O {@code merchant} publica num {@code topic} desde a C-B.
 *
 * <p><b>A propriedade que a ADR protege é a segunda metade da frase</b>, não a
 * primeira. O que mata o cache é fila <i>compartilhada</i>: ela entrega para uma
 * instância só, e as outras ficam com permissão velha sem erro nenhum. Uma
 * {@link AnonymousQueue} ligada a um {@code topic} pela chave certa entrega a
 * cada instância exatamente como a {@code fanout} entregaria — e ainda deixa o
 * consumidor escolher o recorte, que a {@code fanout} não deixa. A emenda desta
 * rodada corrige a palavra na ADR.
 *
 * <h2>{@code AnonymousQueue}, e o que cada propriedade dela compra</h2>
 *
 * <p>Nome gerado no cliente ({@code spring.gen-…}), <b>exclusiva</b>, <b>auto-delete</b>, <b>não
 * durável</b>. Duas instâncias do {@code catalog} ganham duas filas e as duas
 * recebem cada evento — que é a condição para N caches em processo serem
 * invalidados.
 *
 * <p>E as três propriedades têm o mesmo dono: a fila <b>morre com a conexão</b>.
 * É o que se quer — uma fila durável por instância sobreviveria à instância e
 * acumularia mensagens para sempre, num broker, para um consumidor que não
 * existe mais. O preço é que <b>os eventos do intervalo de queda se perdem</b>,
 * e é por isso que existe o {@link #esquecerTudoAoReconectar}.
 */
@Configuration
@EnableConfigurationProperties(EscutaProperties.class)
public class ConfiguracaoDaEscuta {

    /** Os mesmos três argumentos do {@code merchant}. Divergir aqui derruba o serviço. */
    @Bean
    public TopicExchange eventosDoDelivery(EscutaProperties propriedades) {
        return new TopicExchange(propriedades.exchange(), true, false);
    }

    /**
     * Uma fila por instância, e não uma para o serviço.
     *
     * <p>É a linha que a ADR-011 chama de <i>"a primeira coisa a quebrar quando
     * houver duas [instâncias], e a falha é silenciosa"</i>.
     */
    @Bean
    public AnonymousQueue filaDeVinculo() {
        return new AnonymousQueue();
    }

    /**
     * {@code merchant.vinculo.#}, e não a chave exata.
     *
     * <p>O {@code #} cobre as versões que vierem: no dia em que existir um
     * {@code merchant.vinculo.alterado.v2}, este consumidor já o recebe. Ele não
     * saberá lê-lo — e aí o que decide é a tolerância do ouvinte, não o binding.
     * Assinar só a {@code v1} faria o consumidor ficar em silêncio, com o cache
     * cheio e nenhum erro, que é a pior das duas falhas.
     */
    @Bean
    public Binding vinculoAlteradoChegaAoCatalogo(AnonymousQueue filaDeVinculo,
                                                  TopicExchange eventosDoDelivery) {
        return BindingBuilder.bind(filaDeVinculo).to(eventosDoDelivery).with("merchant.vinculo.#");
    }

    /**
     * Esquecer tudo quando a conexão com o broker se refaz.
     *
     * <p>A fila é temporária: se a conexão cai e volta, ela foi recriada
     * <b>vazia</b>, e os eventos publicados no intervalo não estão em lugar
     * nenhum. O cache, que vive na memória do processo, não caiu junto — e
     * continuaria respondendo com o que sabia antes da queda, por até 60 s,
     * sem erro nenhum aparecendo.
     *
     * <p>Esvaziar na reconexão custa algumas consultas ao {@code merchant} e
     * fecha essa janela inteira. <b>É o mesmo princípio da falha ao aplicar uma
     * invalidação:</b> quando não se sabe o que está velho, esquece-se tudo.
     *
     * <p>O {@code onCreate} dispara também na primeira conexão, com o cache
     * vazio — o {@code esvaziar} não registra nada quando não há o que esquecer.
     */
    @Bean
    public ConnectionListener esquecerTudoAoReconectar(ConnectionFactory fabrica,
                                                      CacheDaAutorizacao cache) {
        ConnectionListener ouvinte = new ConnectionListener() {
            @Override
            public void onCreate(org.springframework.amqp.rabbit.connection.Connection conexao) {
                cache.esvaziar("conexão com o broker (re)estabelecida — a fila temporária "
                        + "nasceu vazia e os eventos do intervalo se perderam");
            }
        };
        // Registro explícito: a fábrica autoconfigurada não descobre ouvintes
        // por tipo de bean. Sem esta linha o bean existe e nunca é chamado —
        // que é a forma de peça que este repositório já viu quatro vezes.
        fabrica.addConnectionListener(ouvinte);
        return ouvinte;
    }
}
