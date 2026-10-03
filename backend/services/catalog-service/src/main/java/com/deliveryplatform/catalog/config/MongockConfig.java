package com.deliveryplatform.catalog.config;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.context.annotation.Configuration;

/**
 * A anotação que faltava havia quatro meses.
 *
 * <h2>O que estava errado</h2>
 *
 * <p>O Mongock está no classpath deste serviço <b>desde o primeiro commit</b>,
 * pelo {@code delivery.mongo-conventions}. O {@code application.yml} tem o
 * bloco {@code mongock.migration-scan-package} apontando para o pacote certo —
 * e esse bloco chegou a ter um defeito encontrado e corrigido (estava aninhado
 * dentro de {@code spring:}). Alguém depurou a configuração de uma peça que
 * <b>nunca executou uma linha</b>.
 *
 * <p>O motivo é este arquivo não existir. O jar do {@code mongock-springboot}
 * <b>não traz</b> {@code META-INF/spring/…AutoConfiguration.imports}: sem
 * {@code @EnableMongock} em algum lugar, o runner não é registrado, nenhum
 * {@code changeUnit} roda, e o serviço sobe achando que migrou.
 *
 * <p><i>Peça que nunca rodou não é peça, é intenção</i> — e esta era a forma
 * mais avançada disso: não uma pasta vazia, mas uma peça vestida de
 * funcionando, com YAML, comentário e correção de bug.
 *
 * <h2>Por que aqui e não na classe de aplicação</h2>
 *
 * <p>O padrão da documentação do Mongock põe a anotação junto do
 * {@code @SpringBootApplication}. Aqui ela fica numa configuração própria por
 * dois motivos: a classe de aplicação continua sendo três linhas sem decisão
 * dentro, e o javadoc acima precisa morar em algum lugar que alguém leia.
 *
 * <p>Se por algum motivo a anotação não surtir efeito numa {@code @Configuration}
 * — o teste dirá, porque ele confere o índice e não o contexto —, ela volta
 * para a {@code CatalogServiceApplication}. O que não pode é ninguém notar.
 */
@Configuration
@EnableMongock
public class MongockConfig {
}
