package com.deliveryplatform.order.infrastructure.client;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.order.application.port.out.AutorizacaoIndisponivel;
import com.deliveryplatform.order.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.config.AutorizacaoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cópia deliberada do adaptador do {@code catalog} (a ADR-001 proíbe importá-lo), com o recorte
 * de permissões do pedido. As decisões estão escritas lá; o resumo:
 *
 * <ul>
 *   <li>o token sai do contexto de segurança e é reenviado intacto (ADR-045);</li>
 *   <li>200 → contexto; 403 → vazio; qualquer outro, inclusive 401, →
 *       {@link AutorizacaoIndisponivel};</li>
 *   <li>permissão desconhecida é descartada, nunca concedida;</li>
 *   <li>resposta sobre outra loja é tratada como indisponibilidade.</li>
 * </ul>
 */
@Component
public class AutorizacaoComercialHttp implements AutorizacaoComercialPort {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoComercialHttp.class);

    private static final String CAMINHO =
            "/internal/merchants/{estabelecimentoId}/me/contexto-de-acesso";

    private final RestClient http;

    public AutorizacaoComercialHttp(AutorizacaoProperties propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propriedades.tempoDeConexao());
        fabrica.setReadTimeout(propriedades.tempoDeLeitura());

        this.http = RestClient.builder()
                .baseUrl(propriedades.merchantUri())
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public Optional<ContextoDeAcesso> contexto(UUID estabelecimentoId) {
        String token = tokenDoPortador();

        try {
            return http.get()
                    .uri(CAMINHO, estabelecimentoId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange((requisicao, resposta) -> {
                        int status = resposta.getStatusCode().value();
                        if (status == 200) {
                            return Optional.of(traduzir(
                                    resposta.bodyTo(RespostaDoMerchant.class), estabelecimentoId));
                        }
                        if (status == 403) {
                            return Optional.<ContextoDeAcesso>empty();
                        }
                        throw new AutorizacaoIndisponivel(
                                "o merchant respondeu " + status + " para a autorização", null);
                    });
        } catch (AutorizacaoIndisponivel jaTraduzida) {
            throw jaTraduzida;
        } catch (RestClientException semResposta) {
            throw new AutorizacaoIndisponivel("o merchant não respondeu", semResposta);
        }
    }

    private static String tokenDoPortador() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao instanceof JwtAuthenticationToken comJwt) {
            return comJwt.getToken().getTokenValue();
        }
        throw new AcessoNegado();
    }

    private static ContextoDeAcesso traduzir(RespostaDoMerchant corpo, UUID estabelecimentoId) {
        if (corpo == null) {
            throw new AutorizacaoIndisponivel("o merchant respondeu 200 com corpo vazio", null);
        }
        if (!estabelecimentoId.equals(corpo.estabelecimentoId())) {
            throw new AutorizacaoIndisponivel(
                    "o merchant respondeu sobre outro estabelecimento", null);
        }

        Set<PermissaoDoPedido> conhecidas = EnumSet.noneOf(PermissaoDoPedido.class);
        List<String> recebidas = corpo.permissoes() == null ? List.of() : corpo.permissoes();
        for (String nome : recebidas) {
            PermissaoDoPedido.de(nome).ifPresentOrElse(
                    conhecidas::add,
                    // As de produto, equipe e jornada chegam sempre, e não são do pedido:
                    // debug, e não info, para não encher o registro a cada requisição.
                    () -> log.debug("permissão fora do recorte do pedido descartada: {}", nome));
        }
        return new ContextoDeAcesso(corpo.usuarioId(), estabelecimentoId, conhecidas);
    }

    record RespostaDoMerchant(UUID usuarioId, UUID estabelecimentoId, List<String> permissoes) {
    }
}
