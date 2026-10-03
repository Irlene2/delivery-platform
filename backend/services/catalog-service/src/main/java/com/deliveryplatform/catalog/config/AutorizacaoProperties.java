package com.deliveryplatform.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Onde fica o {@code merchant}, e quanto tempo se espera por ele.
 *
 * <p>Nada aqui é segredo: endereço de serviço interno e tempo limite não são
 * credencial. O token que a chamada carrega é o do usuário, e ele vem da
 * requisição — não de configuração.
 *
 * <h2>Os dois tempos, e por que são curtos</h2>
 *
 * <p>Esta chamada acontece <b>em toda requisição</b> que o catálogo atende
 * (até a G-B4 trazer o cache). Um tempo limite generoso aqui não é tolerância:
 * é a fila de conexões do Tomcat do {@code catalog} enchendo com requisições
 * paradas esperando um serviço que já caiu. <b>Falhar rápido e negar é melhor
 * do que segurar e negar</b> — o resultado para o usuário é o mesmo 403, e a
 * diferença é o serviço continuar de pé.
 *
 * <p><b>300 ms, e o número é da ADR-011</b> — que eu não tinha lido quando
 * escrevi 1 s e 2 s na G-B3. O argumento dela: <i>"Além disso, a requisição do
 * usuário já está lenta"</i>. Com o cache da G-B4, esta chamada deixou de ser o
 * caminho comum: esperar por ela é esperar numa exceção, não numa regra.
 *
 * <p><b>O que estes tempos NÃO são:</b> um substituto para o circuit breaker
 * que a ADR-018 menciona (<i>"timeout e circuit breaker no CatalogPort"</i>,
 * do lado do {@code order}). O {@code resilience4j} está no catálogo de versões
 * e ninguém o resolve; o artefato declarado é o {@code -spring-boot3}, feito
 * para o Boot 3. Ligá-lo é decisão própria, com ADR, e não uma linha de passagem
 * nesta rodada. <b>Gatilho:</b> quando a mesma queda derrubar dois serviços em
 * cascata, ou quando alguém medir que o tempo limite sozinho não basta.
 */
@ConfigurationProperties(prefix = "delivery.autorizacao")
public record AutorizacaoProperties(

        /** Endereço do merchant-service, sem barra no fim. */
        @DefaultValue("http://localhost:8082") String merchantUri,

        /** Tempo para abrir a conexão. */
        @DefaultValue("300ms") Duration tempoDeConexao,

        /** Tempo para a resposta chegar depois de a conexão abrir. */
        @DefaultValue("300ms") Duration tempoDeLeitura
) {
}
