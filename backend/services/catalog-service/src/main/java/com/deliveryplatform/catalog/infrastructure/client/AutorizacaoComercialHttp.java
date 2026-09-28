package com.deliveryplatform.catalog.infrastructure.client;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoIndisponivel;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.config.AutorizacaoProperties;
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
 * A primeira chamada entre serviços deste repositório.
 *
 * <p>Até esta classe existir não havia <b>nenhum</b> cliente HTTP de saída em
 * código de produção — só dublês em teste. Tempo limite, tradução de status e
 * propagação de credencial nascem aqui, e cada um deles tem uma decisão escrita
 * abaixo.
 *
 * <h2>O token sai do contexto de segurança, e não da assinatura</h2>
 *
 * <p>A ADR-038 diz no próprio título: <i>"O {@code sub} vira {@code UUID} na
 * borda, e o caso de uso não conhece o token"</i>. Passar o token como argumento
 * pela porta faria o caso de uso conhecê-lo, e desfaria a assinatura
 * {@code contexto(UUID estabelecimentoId)} que a ADR-045 acabou de fixar. Então
 * ele é lido aqui, do {@link SecurityContextHolder}, e reenviado <b>intacto</b>.
 *
 * <p>O preço está assumido e é o que a ADR-045 já registrou: <b>este adaptador
 * só funciona dentro de uma requisição de usuário</b>. Fora dela não há
 * portador, e a resposta é negar. É a limitação da decisão aparecendo no lugar
 * certo — no código que depende dela — em vez de só no documento.
 *
 * <h2>Três status, e o 401 não é o que parece</h2>
 *
 * <table>
 *   <tr><th>resposta do merchant</th><th>o que este adaptador faz</th></tr>
 *   <tr><td><b>200</b></td><td>traduz e devolve o contexto</td></tr>
 *   <tr><td><b>403</b></td><td>{@code Optional.empty()} — não há vínculo ativo, e isso <i>é</i> uma resposta</td></tr>
 *   <tr><td>qualquer outra</td><td>{@link AutorizacaoIndisponivel}</td></tr>
 * </table>
 *
 * <p><b>O 401 cai no terceiro caso de propósito.</b> Ele significa que o
 * {@code merchant} recusou um token que <i>este</i> serviço acabou de aceitar —
 * ou seja, os dois discordam sobre o que é um token válido. Isso é emissor ou
 * audiência divergentes entre ambientes, não uma resposta sobre vínculo.
 * Tratá-lo como "sem acesso" faria um erro de configuração se disfarçar de
 * regra de negócio — e, quando o cache entrar, ficaria guardado por 10 s,
 * escondendo o defeito por mais tempo ainda.
 *
 * <h2>Por que {@code RestClient} construído à mão, sem dependência nova</h2>
 *
 * <p>A classe {@code RestClient} vem do {@code spring-web}, que o
 * {@code spring-boot-starter-web} já traz. O que <b>não</b> está no classpath é
 * o módulo de autoconfiguração do Boot 4 ({@code spring-boot-starter-restclient}),
 * que daria um {@code RestClient.Builder} pronto e propriedades de tempo limite.
 *
 * <p>Ele não entra porque este adaptador precisa de tempo limite <b>próprio</b>,
 * e não do global: a chamada de autorização acontece em toda requisição e falha
 * rápido de propósito. Com o construtor autoconfigurado, seria um bean
 * compartilhado que este cliente teria de sobrescrever de qualquer jeito.
 * Menos peças, e nenhuma dependência nova.
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
                    // exchange, e não retrieve: o retrieve lança por status e
                    // tentaria converter o corpo de erro no tipo de sucesso.
                    // Aqui cada status tem tratamento próprio, e nenhum deles é
                    // exceção genérica.
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
            // Tempo esgotado, conexão recusada, DNS. Negar é obrigatório
            // (ADR-011: "Fail-closed que abre sob pressão não é fail-closed"),
            // e distinguir é o que permitirá, na G-B4, não cachear isto.
            throw new AutorizacaoIndisponivel("o merchant não respondeu", semResposta);
        }
    }

    /**
     * O portador, lido do contexto de segurança da requisição em curso.
     *
     * <p>Sem autenticação no contexto, nega. Não é um caso que se espere em
     * produção — significa que a porta foi chamada fora de uma requisição de
     * usuário —, e negar é o que a falha fechada manda fazer quando não se sabe.
     */
    private static String tokenDoPortador() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao instanceof JwtAuthenticationToken comJwt) {
            return comJwt.getToken().getTokenValue();
        }
        throw new AcessoNegado();
    }

    /**
     * A tradução que perdoa — e que nunca concede.
     *
     * <p>Permissão que este serviço não conhece é <b>descartada</b>, com
     * registro. É o que a ADR-027 exige do consumidor para que acrescentar valor
     * a enum continue sendo mudança compatível: sem isso, o dia em que o
     * {@code merchant} ganhasse uma permissão nova seria o dia em que ninguém
     * conseguiria ver o cardápio.
     *
     * <p>O {@code papel} vem no corpo e não é lido, pela mesma razão: o
     * {@code catalog} não tem regra que o consulte, e o
     * {@code estabelecimento.md} diz que <i>"toda autorização real é feita pela
     * lista de permissões"</i>. Campo ignorado na leitura é a forma mais barata
     * de tolerância que existe.
     */
    private static ContextoDeAcesso traduzir(RespostaDoMerchant corpo, UUID estabelecimentoId) {
        if (corpo == null) {
            throw new AutorizacaoIndisponivel("o merchant respondeu 200 com corpo vazio", null);
        }
        if (!estabelecimentoId.equals(corpo.estabelecimentoId())) {
            // Não é paranoia barata: é o único jeito de descobrir que a
            // resposta veio sobre outra loja — por cache de proxy, por
            // requisição trocada, por defeito do produtor. Autorizar com o
            // contexto da loja errada é a falha mais grave que esta porta pode
            // ter, e ela seria invisível.
            throw new AutorizacaoIndisponivel(
                    "o merchant respondeu sobre outro estabelecimento", null);
        }

        Set<PermissaoDoCatalogo> conhecidas = EnumSet.noneOf(PermissaoDoCatalogo.class);
        List<String> recebidas = corpo.permissoes() == null ? List.of() : corpo.permissoes();

        for (String nome : recebidas) {
            PermissaoDoCatalogo.de(nome).ifPresentOrElse(
                    conhecidas::add,
                    () -> log.info("permissão desconhecida descartada na autorização: {}", nome));
        }

        return new ContextoDeAcesso(corpo.usuarioId(), estabelecimentoId, conhecidas);
    }

    /**
     * O corpo do {@code merchant}, no recorte que este serviço lê.
     *
     * <p>Sem {@code papel}: o campo existe no fio e não tem leitor aqui.
     */
    record RespostaDoMerchant(UUID usuarioId, UUID estabelecimentoId, List<String> permissoes) {
    }
}
