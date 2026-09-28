package com.deliveryplatform.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * O que este serviço exige de um token — cópia deliberada da
 * {@code JwtProperties} do {@code merchant}, porque a ADR-001 proíbe importá-la.
 *
 * <p>O {@code catalog} valida; quem emite é o {@code identity} (ADR-037). A
 * chave pública vem do JWKS pela rede, e o segredo nunca passa por aqui.
 *
 * <p><b>Emissor e audiência não são decoração.</b> O padrão do Resource Server
 * valida <b>só tempo</b>: um token expirado é recusado, e um token perfeitamente
 * válido emitido por outro ambiente, não.
 *
 * <p><b>E aqui eles ganharam um segundo dono.</b> Este serviço encaminha o
 * token que recebeu para o {@code merchant} (ADR-045), e o {@code merchant} o
 * valida de novo com os mesmos dois campos. Se os dois serviços discordarem
 * sobre emissor ou audiência, o {@code catalog} aceita e o {@code merchant}
 * recusa — e o adaptador trata esse 401 como <b>indisponibilidade</b>, e não
 * como "sem acesso", exatamente para que essa divergência de configuração
 * apareça como problema de sistema em vez de se disfarçar de regra de negócio.
 */
@ConfigurationProperties(prefix = "delivery.jwt")
public record JwtProperties(String issuer, String audience) {
}
