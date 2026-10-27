package com.deliveryplatform.order.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * A forma do catalog, com três rotas públicas a mais — as da vitrine (ADR-077):
 *
 * <ul>
 *   <li>{@code GET  /api/v1/storefront/*}{@code /orders/settings} — taxa e modalidades;</li>
 *   <li>{@code POST /api/v1/storefront/*}{@code /orders} — o pedido;</li>
 *   <li>{@code GET  /api/v1/storefront/orders/*} — o acompanhamento, pelo código.</li>
 * </ul>
 *
 * <p>Os padrões são exatos, um nível de caminho cada (*), e não {@code /storefront/**}: uma
 * rota nova debaixo de /storefront nasce fechada até alguém abri-la aqui de propósito.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, AutorizacaoProperties.class,
        CatalogoProperties.class, DocsProperties.class})
public class SecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            JwtProperties propriedades,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtIssuerValidator(propriedades.issuer()),
                new JwtClaimValidator<List<String>>(
                        JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(propriedades.audience()))));
        return decoder;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain filtros(HttpSecurity http, DocsProperties docs) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> {
                    rotas.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    rotas.requestMatchers("/actuator/health/**").permitAll();
                    rotas.requestMatchers(HttpMethod.GET, "/api/v1/storefront/*/orders/settings")
                            .permitAll();
                    rotas.requestMatchers(HttpMethod.POST, "/api/v1/storefront/*/orders").permitAll();
                    rotas.requestMatchers(HttpMethod.GET, "/api/v1/storefront/orders/*").permitAll();
                    if (docs.abertas()) {
                        rotas.requestMatchers(
                                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .permitAll();
                    }
                    rotas.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
