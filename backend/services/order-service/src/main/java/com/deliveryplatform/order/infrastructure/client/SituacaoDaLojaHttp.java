package com.deliveryplatform.order.infrastructure.client;

import com.deliveryplatform.order.application.port.out.SituacaoDaLojaPort;
import com.deliveryplatform.order.application.port.out.SituacaoIndisponivel;
import com.deliveryplatform.order.config.AutorizacaoProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.UUID;

/**
 * Pergunta ao {@code merchant}, pela rota pública da vitrine e sem token, se a loja está
 * aberta. O mesmo endereço e os mesmos 300 ms da autorização (ADR-011).
 *
 * <p>404 é "não é loja": volta como fechada, sem próxima abertura. Qualquer outra coisa que não
 * seja 200 com corpo é {@link SituacaoIndisponivel}.
 */
@Component
public class SituacaoDaLojaHttp implements SituacaoDaLojaPort {

    private static final String CAMINHO = "/api/v1/storefront/{estabelecimentoId}/status";

    private final RestClient http;

    public SituacaoDaLojaHttp(AutorizacaoProperties propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propriedades.tempoDeConexao());
        fabrica.setReadTimeout(propriedades.tempoDeLeitura());

        this.http = RestClient.builder()
                .baseUrl(propriedades.merchantUri())
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public SituacaoDaLoja daLoja(UUID estabelecimentoId) {
        Lida corpo;
        try {
            corpo = http.get()
                    .uri(CAMINHO, estabelecimentoId)
                    .exchange((requisicao, resposta) -> {
                        int status = resposta.getStatusCode().value();
                        if (status == 404) {
                            return new Lida(false, null);
                        }
                        if (status != 200) {
                            throw new SituacaoIndisponivel(
                                    "o merchant respondeu " + status + " para a situação da loja", null);
                        }
                        return resposta.bodyTo(Lida.class);
                    });
        } catch (SituacaoIndisponivel jaTraduzida) {
            throw jaTraduzida;
        } catch (RestClientException semResposta) {
            throw new SituacaoIndisponivel("o merchant não respondeu", semResposta);
        }
        if (corpo == null) {
            throw new SituacaoIndisponivel("o merchant respondeu 200 sem corpo", null);
        }
        return corpo.aberta() ? SituacaoDaLoja.abertaAgora() : SituacaoDaLoja.fechada(corpo.proximaAbertura());
    }

    /** O recorte do corpo que o pedido lê; o nome é da tela. */
    record Lida(boolean aberta, Instant proximaAbertura) {
    }
}
