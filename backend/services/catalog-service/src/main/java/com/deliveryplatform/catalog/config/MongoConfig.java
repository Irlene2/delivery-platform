package com.deliveryplatform.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * O gerente de transação do Mongo, que o Spring Boot <b>não</b> autoconfigura.
 *
 * <p>Sem este bean não existe gerente de transação nenhum no contexto do
 * {@code catalog}, e um {@code @Transactional} numa escrita do Mongo não abre
 * transação: ou estoura por não achar gerente, ou — pior, se algum dia houver
 * um segundo gerente por outro motivo — abre a transação errada.
 *
 * <p>A ADR-008 existe inteira por causa disso: o replica set de nó único está
 * no compose para que a gravação do documento e a linha do outbox sejam
 * <b>um</b> ato. Sem gerente de transação, o replica set é decoração.
 *
 * <p><b>O catálogo ainda não tem outbox</b> — ele não publica nada até o marco
 * 7, e o gatilho está escrito na ADR-021. Mas a transação nasce aqui, e não
 * junto com o outbox, por um motivo: a ADR-008 pediu explicitamente um
 * <i>"teste de fumaça que abra uma transação no startup de cada serviço
 * documental"</i>, e essa peça é o que o {@code TransacaoDoMongoIT} exercita.
 * Descobrir que a transação não funciona no dia em que o outbox precisar dela é
 * descobrir tarde.
 */
@Configuration
public class MongoConfig {

    @Bean
    MongoTransactionManager transacaoDoMongo(MongoDatabaseFactory fabrica) {
        return new MongoTransactionManager(fabrica);
    }
}
