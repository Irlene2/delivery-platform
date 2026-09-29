package com.deliveryplatform.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Os três números que a ADR-011 fixou e que a G-B3 não tinha onde pôr.
 *
 * <table>
 *   <tr><th>parâmetro</th><th>ADR-011</th><th>por quê, nas palavras dela</th></tr>
 *   <tr><td>prazo positivo</td><td>60 s</td><td><i>"o pior caso de acesso indevido depois de uma revogação cujo evento se perdeu"</i></td></tr>
 *   <tr><td>prazo negativo</td><td>10 s</td><td><i>"Curto porque acesso recém-concedido não pode demorar"</i></td></tr>
 *   <tr><td>máximo de entradas</td><td>10 000</td><td><i>"Teto de memória previsível; descarte por menos-recentemente-usado"</i></td></tr>
 * </table>
 *
 * <p><b>Configuráveis, e não constantes.</b> Ao contrário da
 * {@code HORA_DE_CORTE} da ADR-025 — que é regra de negócio e por isso não é
 * campo —, estes três são de ajuste operacional: num incidente alguém pode
 * querer baixar o prazo positivo sem recompilar.
 *
 * <p>Nenhum deles é segredo.
 */
@ConfigurationProperties(prefix = "delivery.autorizacao.cache")
public record CacheDaAutorizacaoProperties(

        @DefaultValue("60s") Duration prazoPositivo,

        @DefaultValue("10s") Duration prazoNegativo,

        @DefaultValue("10000") int maximoDeEntradas
) {
}
