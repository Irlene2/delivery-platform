package com.deliveryplatform.gateway.integration;

import com.deliveryplatform.gateway.support.GatewayNoAr;
import com.deliveryplatform.gateway.support.UpstreamsDeMentira;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As rotas do gateway, uma a uma, contra o gateway de pé — e a prova de que
 * nenhuma ficou sem caso.
 *
 * <p><b>O que esta classe substitui.</b> O {@code application.yml} carregava
 * quinze linhas de comentário avisando que o namespace
 * {@code spring.cloud.gateway.server.webmvc.routes} nunca tinha sido verificado,
 * que a variante reativa usa outro, e que <i>"o build ficar verde NÃO prova nada
 * aqui"</i> — terminando com uma instrução para um humano rodar {@code curl}.
 *
 * <p>Instrução para um humano é asserção sem executor. Se o prefixo estiver
 * errado, <b>nenhuma</b> rota casa: o gateway devolve 404 do próprio Tomcat em
 * vez de encaminhar, e esta classe inteira fica vermelha — em vez de o serviço
 * subir calado em produção e não rotear nada.
 *
 * <p>Cada caso confere três coisas de uma vez: que a rota casou, que casou com
 * o <b>serviço certo</b> (cada dublê responde o próprio nome, de uma porta
 * própria), e que o caminho <b>atravessou inteiro</b>, sem reescrita nem corte
 * de prefixo.
 *
 * <p><b>A lista das rotas é o {@code application.yml}, e esta classe não é uma
 * segunda</b> (ADR-071). Até a R-3 os casos viviam em {@code @CsvSource} escritos
 * à mão, e rota nova sem caso passava em silêncio. Agora cada caso nomeia o
 * {@code id} da rota que afirma, e {@link #toda_rota_do_gateway_tem_caso_e_todo_caso_tem_rota()}
 * compara esses ids com os que o gateway carregou, nos dois sentidos.
 *
 * <p><b>O que continua sendo cópia:</b> o {@code id} de cada caso é escrito à
 * mão. Um caso pode nomear o {@code id} errado e passar, desde que o serviço
 * certo responda — o teste prova que não há rota sem caso, não que cada caso
 * exercita a rota que nomeia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RoteamentoIT extends GatewayNoAr {

    private static final String LOJA = "7b1f0c22-9d3e-4a51-8f6b-2c0d7e5a1934";

    /** Um caminho, o serviço que tem de responder, e o {@code id} da rota do {@code application.yml}. */
    private record Caso(String caminho, String servico, String rota) {
    }

    private static final List<Caso> RECURSOS = List.of(
            // O vínculo mora no merchant (G-B5), e o predicado exato vem ANTES
            // do /api/v1/me/** genérico: os três casos seguintes pegam, cada um,
            // uma falha diferente — rota abaixo da genérica, rota que engoliu o
            // /me inteiro, e rota com /** no fim.
            new Caso("/api/v1/me/estabelecimentos", "merchant", "me-estabelecimentos"),
            new Caso("/api/v1/me/perfil", "identity", "me"),
            new Caso("/api/v1/me/estabelecimentos/qualquer-coisa", "identity", "me"),
            new Caso("/api/v1/merchants/{loja}/settings", "merchant", "merchant-settings"),
            new Caso("/api/v1/merchants/{loja}/team", "merchant", "merchant-team"),
            new Caso("/api/v1/merchants/{loja}/areas", "merchant", "merchant-areas"),
            new Caso("/api/v1/merchants/{loja}/couriers", "merchant", "merchant-couriers"),
            new Caso("/api/v1/merchants/{loja}/catalog/produtos", "catalog", "catalog"),
            new Caso("/api/v1/merchants/{loja}/shifts", "settlement", "shifts"),
            new Caso("/api/v1/merchants/{loja}/orders", "order", "orders"),
            new Caso("/api/v1/merchants/{loja}/payments", "payment", "payments"),
            new Caso("/api/v1/merchants/{loja}/deliveries", "delivery", "deliveries"),
            new Caso("/api/v1/couriers/me/entregas", "delivery", "courier-self"),
            new Caso("/api/v1/convites/um-token-qualquer", "merchant", "convites"));

    private static final List<Caso> AUTH = List.of(
            new Caso("/api/v1/auth/login", "identity", "auth"),
            new Caso("/api/v1/auth/signup", "identity", "auth"),
            new Caso("/api/v1/auth/verification-code", "identity", "auth"),
            new Caso("/api/v1/auth/qualquer-rota-que-ainda-nao-existe", "identity", "auth"));

    private static final List<Caso> WEBHOOKS = List.of(
            new Caso("/api/v1/webhooks/psp/pix", "payment", "webhook-psp"),
            new Caso("/api/v1/webhooks/channel/whatsapp", "conversation", "webhook-channel"));

    static Stream<Arguments> recursos() {
        return RECURSOS.stream().map(c -> Arguments.of(c.caminho(), c.servico()));
    }

    static Stream<Arguments> auth() {
        return AUTH.stream().map(c -> Arguments.of(c.caminho()));
    }

    static Stream<Arguments> webhooks() {
        return WEBHOOKS.stream().map(c -> Arguments.of(c.caminho(), c.servico()));
    }

    @Autowired
    GatewayMvcProperties gateway;

    private String comToken(String caminho) {
        return cliente().get().uri(caminho)
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(UUID.randomUUID()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();
    }

    @DisplayName("toda rota do gateway tem caso, e todo caso nomeia uma rota que existe")
    @Test
    void toda_rota_do_gateway_tem_caso_e_todo_caso_tem_rota() {
        // A lista vem do que o gateway carregou do application.yml — o mesmo
        // objeto de configuração de onde ele monta as rotas —, e não de um
        // conjunto digitado aqui.
        Set<String> doGateway = new TreeSet<>(gateway.getRoutesMap().keySet());
        gateway.getRoutes().stream().map(RouteProperties::getId).forEach(doGateway::add);

        Set<String> dosCasos = new TreeSet<>();
        Stream.of(RECURSOS, AUTH, WEBHOOKS).flatMap(List::stream)
                .map(Caso::rota).forEach(dosCasos::add);

        assertThat(doGateway).as("o gateway carregou rotas — sem isto a comparação é vazia")
                .isNotEmpty();

        Set<String> semCaso = new TreeSet<>(doGateway);
        semCaso.removeAll(dosCasos);
        assertThat(semCaso)
                .as("a rota %s entrou no gateway e nenhum caso a afirma", semCaso)
                .isEmpty();

        Set<String> semRota = new TreeSet<>(dosCasos);
        semRota.removeAll(doGateway);
        assertThat(semRota)
                .as("o caso afirma a rota %s, que o gateway não tem", semRota)
                .isEmpty();
    }

    @DisplayName("cada recurso chega no serviço que a ADR-012 nomeia")
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("recursos")
    void a_rota_leva_ao_servico_certo(String modelo, String servicoEsperado) {
        String caminho = modelo.trim().replace("{loja}", LOJA);

        String corpo = comToken(caminho);

        assertThat(corpo)
                .as("a rota precisa casar E casar com o dono certo — portas "
                        + "diferentes, nomes diferentes, sem empate possível")
                .contains("\"servico\":\"" + servicoEsperado + "\"")
                .contains("\"caminho\":\"" + caminho + "\"");
    }

    @DisplayName("o prefixo /auth vai ao identity, e sem token")
    @ParameterizedTest(name = "{0}")
    @MethodSource("auth")
    void o_prefixo_auth_atravessa_sem_token(String caminho) {
        String corpo = cliente().get().uri(caminho)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(corpo)
                .as("o gateway abre o prefixo inteiro; quem decide rota a rota é "
                        + "o identity, cuja cadeia é anyRequest().authenticated() "
                        + "por padrão (ADR-044 §2)")
                .contains("\"servico\":\"identity\"");
    }

    @DisplayName("cada webhook vai ao serviço que valida a assinatura dele")
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("webhooks")
    void o_webhook_vai_ao_dono_da_assinatura(String caminho, String servicoEsperado) {
        String corpo = cliente().post().uri(caminho.trim())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult().getResponseBody();

        assertThat(corpo).contains("\"servico\":\"" + servicoEsperado.trim() + "\"");
    }

    @Test
    void o_token_atravessa_para_o_servico() {
        String corpo = comToken("/api/v1/merchants/" + LOJA + "/team");

        assertThat(corpo)
                .as("o serviço revalida o token — é o que impede que alcançar a "
                        + "rede interna, por qualquer caminho, equivalha a estar "
                        + "autorizado (ADR-012). Um gateway que autentica e retira "
                        + "o cabeçalho transforma todo serviço num serviço que "
                        + "confia na rede.")
                .contains("\"autorizacao\":true");
    }

    @Test
    void caminho_que_nenhuma_rota_cobre_nao_vaza_para_servico_nenhum() {
        cliente().get().uri("/api/v1/inventory/skus")
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(UUID.randomUUID()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void o_upstream_do_catalog_e_o_do_merchant_nao_se_confundem() {
        // As duas rotas compartilham o prefixo /merchants/{id}/ e diferem só no
        // segmento seguinte. É onde a ordem dos predicados erra em silêncio: uma
        // rota mal ordenada captura o que era de outra e ninguém percebe até o
        // recurso errado responder.
        assertThat(comToken("/api/v1/merchants/" + LOJA + "/catalog/x"))
                .contains("\"servico\":\"" + UpstreamsDeMentira.CATALOG + "\"");
        assertThat(comToken("/api/v1/merchants/" + LOJA + "/team"))
                .contains("\"servico\":\"" + UpstreamsDeMentira.MERCHANT + "\"");
    }
}
