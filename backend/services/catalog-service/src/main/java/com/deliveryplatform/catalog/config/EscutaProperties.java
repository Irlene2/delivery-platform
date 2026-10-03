package com.deliveryplatform.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * O nome da exchange que este serviço escuta.
 *
 * <p>Um campo só, e ele existe por uma razão: <b>o {@code catalog} redeclara a
 * exchange do {@code merchant}</b>, e o AMQP recusa redeclaração com argumentos
 * diferentes. Se o nome fosse constante nos dois lados, mudá-lo num deles
 * derrubaria o outro sem dizer por quê.
 *
 * <p>O prefixo é {@code delivery.escuta} e não {@code delivery.outbox}: este
 * serviço não tem outbox. O valor padrão é o mesmo — {@code delivery.eventos} —
 * e continuar igual é a condição para as duas declarações casarem.
 *
 * <p>Não é segredo. A credencial do broker mora em {@code spring.rabbitmq.*} e
 * vem do ambiente.
 */
@ConfigurationProperties(prefix = "delivery.escuta")
public record EscutaProperties(

        @DefaultValue("delivery.eventos") String exchange
) {
}
