package com.deliveryplatform.catalog.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * O MongoDB que o {@code catalog-service} precisa, subido uma vez para a
 * execução inteira.
 *
 * <p>Mesmo padrão do {@code Infraestrutura} do {@code merchant}: campo
 * estático com {@code start()} no bloco estático, e não {@code @Container}.
 * {@code @Container} amarra o ciclo de vida à classe de teste, e cada IT
 * subiria o seu Mongo. Aqui sobe no primeiro que precisar e fica até o fim da
 * JVM — o Ryuk derruba.
 *
 * <h2>{@code withReplicaSet()} é obrigatório, e a ADR-008 dizia o contrário</h2>
 *
 * <p>A ADR-008 afirma que <i>"o {@code MongoDBContainer} do Testcontainers já
 * sobe em replica set por padrão"</i>. Isso valia para
 * {@code org.testcontainers.containers.MongoDBContainer}, que na Testcontainers
 * 2 está <b>depreciada</b>. A classe atual, no pacote
 * {@code org.testcontainers.mongodb}, tem o replica set <b>desligado</b> por
 * padrão: o campo interno só é ligado por {@link MongoDBContainer#withReplicaSet()}.
 *
 * <p>Sem essa chamada, tudo passa — até o primeiro {@code @Transactional},
 * que é a falha que a ADR-008 inteira existe para impedir. A emenda desta
 * rodada corrige o texto da ADR.
 *
 * <h2>Por que RabbitMQ de mentira e não contêiner</h2>
 *
 * <p>O {@code application.yml} declara {@code spring.rabbitmq.username:
 * ${RABBITMQ_USERNAME}} <b>sem valor padrão</b>. Um marcador que não resolve
 * derruba o contexto no startup, antes de qualquer teste — e o
 * {@code catalog} ainda não fala com o broker: a autoconfiguração do AMQP cria
 * a fábrica de conexão de forma preguiçosa e não conecta no boot.
 *
 * <p>Então aqui entram dois valores quaisquer, só para o marcador resolver. O
 * contêiner de verdade chega na G-B4, com o primeiro consumidor — e nesse dia
 * estes dois valores saem.
 */
public abstract class Infraestrutura {

    protected static final MongoDBContainer MONGO =
            new MongoDBContainer("mongo:8").withReplicaSet();

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void apontarParaOsConteineres(DynamicPropertyRegistry registry) {
        // spring.mongodb.uri, e não spring.data.mongodb.uri: no Boot 4 a chave antiga
        // não é lida, e o contexto cairia em localhost:27017 — o Mongo do compose.
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("catalog_db"));

        // Só para o marcador resolver. Ver o javadoc.
        registry.add("spring.rabbitmq.username", () -> "nao-usado-na-gb1");
        registry.add("spring.rabbitmq.password", () -> "nao-usado-na-gb1");
    }
}
