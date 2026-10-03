package com.deliveryplatform.catalog.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * O MongoDB e o RabbitMQ que o {@code catalog-service} precisa, subidos uma vez
 * para a execução inteira.
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
 * <h2>O RabbitMQ entrou na G-B3, e não foi por causa de mensagem</h2>
 *
 * <p>Até aqui este serviço subia com credenciais de mentira, porque o
 * {@code application.yml} declara {@code spring.rabbitmq.username:
 * ${RABBITMQ_USERNAME}} <b>sem valor padrão</b> e um marcador que não resolve
 * derruba o contexto. Funcionava, porque ninguém falava com o broker.
 *
 * <p><b>Deixou de funcionar quando nasceu a primeira rota.</b> O
 * {@code spring-boot-starter-amqp} registra um indicador de saúde, o indicador
 * não acha broker nenhum, e {@code /actuator/health} responde <b>503</b>. O
 * {@code merchant} só passa no teste equivalente porque tem RabbitMQ de verdade
 * desde a C-B.
 *
 * <p>As saídas eram três, e duas são ruins: não escrever o teste de saúde — e
 * a primeira rota do catálogo ficaria sem a única asserção que prova que o
 * serviço se declara são corretamente; ou desligar o indicador — que é
 * exatamente o que o {@code merchant} fez na C-A e teve de desfazer na C-B,
 * porque um serviço que se declara são sem conseguir falar com a
 * infraestrutura é a pior forma de indisponibilidade, a que o orquestrador não
 * vê.
 *
 * <p>Então o contêiner sobe aqui, uma rodada antes de haver consumidor. A G-B4
 * o usa de verdade.
 */
public abstract class Infraestrutura {

    protected static final MongoDBContainer MONGO =
            new MongoDBContainer("mongo:8").withReplicaSet();

    /** A mesma imagem do {@code merchant}: duas versões de broker em teste é uma diferença que ninguém lembra de ter. */
    protected static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:4-alpine");

    static {
        MONGO.start();
        RABBIT.start();
    }

    @DynamicPropertySource
    static void apontarParaOsConteineres(DynamicPropertyRegistry registry) {
        // spring.mongodb.uri, e não spring.data.mongodb.uri: no Boot 4 a chave antiga
        // não é lida, e o contexto cairia em localhost:27017 — o Mongo do compose.
        registry.add("spring.mongodb.uri", () -> MONGO.getReplicaSetUrl("catalog_db"));

        // addresses, e não host/port: o application.yml define
        // spring.rabbitmq.addresses, e com ela definida o Spring Boot ignora
        // host e port — o teste falaria com localhost:5672 em vez do contêiner.
        registry.add("spring.rabbitmq.addresses",
                () -> RABBIT.getHost() + ":" + RABBIT.getAmqpPort());
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }
}
