package com.deliveryplatform.merchant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Se a documentação viva deste serviço responde sem token.
 *
 * <h2>Fechada por padrão, e o padrão é o que vai para produção</h2>
 *
 * <p>{@code false} quando a propriedade não existe. Um valor padrão aberto
 * significaria que esquecer a configuração <b>expõe</b> — e a configuração se
 * esquece. Aqui esquecer fecha.
 *
 * <p>É a lição do {@code /actuator/gateway}, que esteve exposto até a G-B5
 * tirá-lo: alguém o abriu como diagnóstico e a linha ficou. A diferença é que lá
 * o padrão era a exposição.
 *
 * <p><b>Ela não liga a documentação</b> — o springdoc gera o documento de
 * qualquer jeito, e é dele que sai o contrato commitado (ADR-039). Ela liga
 * apenas o <b>acesso sem token</b> a {@code /swagger-ui/**} e
 * {@code /v3/api-docs/**}.
 *
 * @param abertas ligue só no seu computador. ADR-050
 */
@ConfigurationProperties(prefix = "delivery.docs")
public record DocsProperties(boolean abertas) {
}
