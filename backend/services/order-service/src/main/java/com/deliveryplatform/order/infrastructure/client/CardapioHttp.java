package com.deliveryplatform.order.infrastructure.client;

import com.deliveryplatform.order.application.port.out.CardapioIndisponivel;
import com.deliveryplatform.order.application.port.out.CardapioPort;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.config.CatalogoProperties;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lê o cardápio público da loja no {@code catalog} — a mesma rota que a tela do cliente usa,
 * sem token, porque é pública (ADR-077). O pedido não confia no que a tela mostrou: pergunta
 * de novo, na hora de criar.
 *
 * <p>Qualquer coisa que não seja 200 com corpo é {@link CardapioIndisponivel} (503 na borda):
 * sem preço do servidor, o pedido não nasce.
 */
@Component
public class CardapioHttp implements CardapioPort {

    private static final String CAMINHO = "/api/v1/storefront/{estabelecimentoId}/menu";

    private final RestClient http;

    public CardapioHttp(CatalogoProperties propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propriedades.tempoDeConexao());
        fabrica.setReadTimeout(propriedades.tempoDeLeitura());

        this.http = RestClient.builder()
                .baseUrl(propriedades.catalogUri())
                .requestFactory(fabrica)
                .build();
    }

    @Override
    public Map<UUID, ProdutoDoCardapio> publicadosDe(UUID estabelecimentoId) {
        Cardapio corpo;
        try {
            corpo = http.get()
                    .uri(CAMINHO, estabelecimentoId)
                    .exchange((requisicao, resposta) -> {
                        int status = resposta.getStatusCode().value();
                        if (status != 200) {
                            throw new CardapioIndisponivel(
                                    "o catalog respondeu " + status + " para o cardápio", null);
                        }
                        return resposta.bodyTo(Cardapio.class);
                    });
        } catch (CardapioIndisponivel jaTraduzida) {
            throw jaTraduzida;
        } catch (RestClientException semResposta) {
            throw new CardapioIndisponivel("o catalog não respondeu", semResposta);
        }
        if (corpo == null || corpo.produtos() == null) {
            throw new CardapioIndisponivel("o catalog respondeu 200 sem produtos", null);
        }

        Map<UUID, ProdutoDoCardapio> publicados = new HashMap<>();
        for (ProdutoLido p : corpo.produtos()) {
            if (p == null || p.id() == null || p.nome() == null || p.precoBase() == null) {
                continue;
            }
            publicados.put(p.id(),
                    new ProdutoDoCardapio(p.id(), p.nome(), Money.de(p.precoBase()), p.vendavel()));
        }
        return publicados;
    }

    /** O recorte do corpo que o pedido lê. O resto (seções, descrição, ordem) é da tela. */
    record Cardapio(List<ProdutoLido> produtos) {
    }

    record ProdutoLido(UUID id, String nome, BigDecimal precoBase, boolean vendavel) {
    }
}
