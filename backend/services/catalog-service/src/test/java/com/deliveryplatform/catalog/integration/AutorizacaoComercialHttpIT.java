package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.AutorizacaoIndisponivel;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.infrastructure.client.AutorizacaoComercialHttp;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.MerchantDeMentira;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O adaptador sozinho, contra um {@code merchant} de mentira — e é aqui que a
 * distinção entre <b>negado</b> e <b>não respondeu</b> fica visível.
 *
 * <p>Pela rota, os dois casos são 403 e não se distinguem (é o que M7 quer). A
 * diferença vive na porta: {@code Optional.empty()} contra
 * {@link AutorizacaoIndisponivel}. Sem este arquivo, a decisão mais consequente
 * da rodada não teria nenhuma asserção que a prove — e ela só vai render fruto
 * na G-B4, quando um dos dois puder ser cacheado e o outro não.
 *
 * <p>O tempo de leitura é encurtado para 300 ms aqui, porque o teste do
 * silêncio espera por ele de verdade. Com os 2 s do padrão, este arquivo
 * levaria dois segundos a mais por nada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "delivery.autorizacao.tempo-de-conexao=300ms",
        "delivery.autorizacao.tempo-de-leitura=300ms"
})
class AutorizacaoComercialHttpIT extends Infraestrutura {

    private static final MerchantDeMentira MERCHANT = MerchantDeMentira.subir();

    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final String TOKEN = "um.token.qualquer";

    @DynamicPropertySource
    static void apontarParaOMerchantDeMentira(DynamicPropertyRegistry registry) {
        registry.add("delivery.autorizacao.merchant-uri", MERCHANT::uri);
    }

    @AfterAll
    static void derrubar() {
        MERCHANT.close();
    }

    /**
     * O adaptador, e não a porta. Desde a G-B4 a porta injetada é o decorador
     * com cache (@Primary), e ele guardaria a resposta de um caso para o
     * seguinte. Este arquivo testa o que o HTTP faz; o cache tem os seus
     * ({@code CacheDaAutorizacaoTest}, {@code InvalidacaoPorEventoIT}).
     */
    @Autowired AutorizacaoComercialHttp autorizacao;

    /**
     * O adaptador lê o portador do contexto de segurança (ADR-038), então o
     * teste precisa pôr um lá. O {@link Jwt} aqui não é validado por ninguém —
     * o que importa é o valor bruto do token, que é o que será encaminhado.
     */
    @BeforeEach
    void umPortadorNoContexto() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject(USUARIO.toString())
                .issuedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(29, ChronoUnit.MINUTES))
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MERCHANT.esquecerOQueRecebeu();
    }

    @AfterEach
    void limparOContexto() {
        SecurityContextHolder.clearContext();
    }

    // ── 200: a resposta ─────────────────────────────────────────────────────

    @Test
    @DisplayName("200 vira contexto, e as permissões conhecidas entram")
    void o_contexto_chega() {
        MERCHANT.respondeCom(USUARIO, LOJA, "COLABORADOR",
                List.of("VER_PRODUTO", "ALTERAR_PRODUTO"));

        ContextoDeAcesso contexto = autorizacao.contexto(LOJA).orElseThrow();

        assertThat(contexto.usuarioId()).isEqualTo(USUARIO);
        assertThat(contexto.estabelecimentoId()).isEqualTo(LOJA);
        assertThat(contexto.permissoes()).containsExactlyInAnyOrder(
                PermissaoDoCatalogo.VER_PRODUTO, PermissaoDoCatalogo.ALTERAR_PRODUTO);
        assertThat(contexto.pode(PermissaoDoCatalogo.VER_PRODUTO)).isTrue();
    }

    @Test
    @DisplayName("o token do portador chega intacto ao merchant — é o que a ADR-045 decidiu")
    void o_token_e_encaminhado() {
        MERCHANT.respondeCom(USUARIO, LOJA, "COLABORADOR", List.of("VER_PRODUTO"));

        autorizacao.contexto(LOJA);

        assertThat(MERCHANT.autorizacoesRecebidas())
                .as("sem esta asserção, um adaptador que mandasse token nenhum passaria em "
                        + "todos os outros testes deste arquivo")
                .containsExactly("Bearer " + TOKEN);
        assertThat(MERCHANT.caminhosRecebidos())
                .containsExactly("/internal/merchants/" + LOJA + "/me/contexto-de-acesso");
    }

    @Test
    @DisplayName("permissão que este serviço não conhece é descartada — nunca concedida")
    void permissao_desconhecida_some() {
        MERCHANT.respondeCom(USUARIO, LOJA, "COLABORADOR",
                List.of("VER_PRODUTO", "PERMISSAO_QUE_AINDA_NAO_EXISTE"));

        ContextoDeAcesso contexto = autorizacao.contexto(LOJA).orElseThrow();

        assertThat(contexto.permissoes())
                .as("a ADR-027 só é verdade se o consumidor tolerar: sem isso, o dia em que o "
                        + "merchant ganhasse uma permissão seria o dia em que ninguém abriria "
                        + "o cardápio")
                .containsExactly(PermissaoDoCatalogo.VER_PRODUTO);
    }

    @Test
    @DisplayName("papel vem no corpo e não é lido — tolerância de campo, de graça")
    void o_papel_e_ignorado() {
        MERCHANT.respondeCom(USUARIO, LOJA, "UM_PAPEL_QUE_NAO_EXISTE", List.of("VER_PRODUTO"));

        assertThat(autorizacao.contexto(LOJA)).isPresent();
    }

    // ── 403: também é uma resposta ──────────────────────────────────────────

    @Test
    @DisplayName("403 vira vazio — o produtor sabe e disse")
    void sem_vinculo_vira_vazio() {
        MERCHANT.nega();

        assertThat(autorizacao.contexto(LOJA)).isEmpty();
    }

    // ── o resto: ausência de resposta ───────────────────────────────────────

    @Test
    @DisplayName("500 não é resposta — e por isso nunca poderá ser cacheado")
    void erro_do_merchant_e_indisponibilidade() {
        MERCHANT.estoura();

        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .isInstanceOf(AutorizacaoIndisponivel.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("silêncio estoura por tempo — o modo de falha mais comum e o menos testado")
    void silencio_estoura_por_tempo() {
        MERCHANT.emSilencio();

        long comecou = System.nanoTime();
        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .isInstanceOf(AutorizacaoIndisponivel.class);
        long levou = (System.nanoTime() - comecou) / 1_000_000;

        assertThat(levou)
                .as("o tempo limite é de 300 ms neste teste; sem tempo limite, esta chamada "
                        + "ficaria presa e o Tomcat do catálogo encheria de requisições "
                        + "esperando um serviço que já caiu")
                .isLessThan(3_000);
    }

    @Test
    @DisplayName("401 é indisponibilidade, e não recusa — os dois serviços discordam do token")
    void o_401_nao_e_sem_acesso() {
        MERCHANT.recusaOToken();

        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .as("o merchant recusou um token que este serviço aceitou: emissor ou "
                        + "audiência divergentes entre ambientes. Tratar como 'sem acesso' "
                        + "faria um erro de configuração se disfarçar de regra de negócio — e, "
                        + "com o cache da G-B4, ficaria escondido por 10 s")
                .isInstanceOf(AutorizacaoIndisponivel.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("resposta sobre outra loja é indisponibilidade — autorizar com ela é o pior caso")
    void resposta_sobre_outra_loja() {
        MERCHANT.respondeCom(USUARIO, UUID.randomUUID(), "COLABORADOR", List.of("VER_PRODUTO"));

        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .isInstanceOf(AutorizacaoIndisponivel.class)
                .hasMessageContaining("outro estabelecimento");
    }

    @Test
    @DisplayName("sem portador no contexto, nega — a porta não chama sem token")
    void sem_contexto_de_seguranca() {
        SecurityContextHolder.clearContext();
        MERCHANT.respondeCom(USUARIO, LOJA, "COLABORADOR", List.of("VER_PRODUTO"));

        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .isInstanceOf(RuntimeException.class);
        assertThat(MERCHANT.autorizacoesRecebidas())
                .as("não é só negar: é não sair requisição nenhuma")
                .isEmpty();
    }

    @Test
    void vazio_nunca_e_nulo() {
        MERCHANT.nega();

        Optional<ContextoDeAcesso> contexto = autorizacao.contexto(LOJA);

        assertThat(contexto).isNotNull().isEmpty();
    }
}
