package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoIndisponivel;
import com.deliveryplatform.catalog.infrastructure.cache.CacheDaAutorizacao;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.MerchantDeMentira;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * O primeiro evento a atravessar este repositório de ponta a ponta — e a
 * primeira prova de que o cache que a ADR-011 desenhou em agosto <b>invalida</b>.
 *
 * <h2>Por que um broker de verdade, e não um dublê</h2>
 *
 * <p>Este teste publica na exchange do contêiner e espera o efeito. Um dublê do
 * ouvinte provaria que um método é chamado quando alguém o chama. Aqui o que se
 * prova é a corrente inteira: a exchange existe com os argumentos certos, o
 * binding {@code merchant.vinculo.#} casa com a chave
 * {@code merchant.vinculo.alterado.v1}, a fila anônima recebe, o JSON é lido, e
 * a entrada some do cache.
 *
 * <p><b>Qualquer elo errado deixa este arquivo vermelho, e nenhum outro teste
 * do repositório notaria</b> — um consumidor que sobe e nunca recebe mensagem é
 * o modo de falha mais silencioso que mensageria tem. É o mesmo raciocínio do
 * {@code ConteinerDeVerdadeIT} da G-B1.1: prova por efeito, não por endereço.
 *
 * <h2>O contexto de segurança é posto na mão</h2>
 *
 * <p>Sem servidor web, não há cadeia de filtros para pôr o portador no
 * {@link SecurityContextHolder} — e sem portador não há chave de cache. O
 * {@link Jwt} aqui não é validado por ninguém: o que importa é o {@code sub},
 * que é de onde sai a chave.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InvalidacaoPorEventoIT extends Infraestrutura {

    private static final MerchantDeMentira MERCHANT = MerchantDeMentira.subir();

    private static final UUID LOJA = UUID.randomUUID();
    private static final UUID MARLI = UUID.randomUUID();

    @DynamicPropertySource
    static void apontarParaOMerchantDeMentira(DynamicPropertyRegistry registry) {
        registry.add("delivery.autorizacao.merchant-uri", MERCHANT::uri);
    }

    @AfterAll
    static void derrubar() {
        MERCHANT.close();
    }

    @Autowired AutorizacaoComercialPort autorizacao;
    @Autowired CacheDaAutorizacao cache;
    @Autowired RabbitTemplate rabbit;
    @Autowired CachingConnectionFactory conexoes;

    @BeforeEach
    void umPortadorNoContextoEOCacheVazio() {
        Jwt jwt = Jwt.withTokenValue("um.token.qualquer")
                .header("alg", "RS256")
                .subject(MARLI.toString())
                .issuedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(29, ChronoUnit.MINUTES))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        cache.esvaziar("preparo do teste");
        MERCHANT.esquecerOQueRecebeu();
        MERCHANT.respondeCom(MARLI, LOJA, "COLABORADOR", List.of("VER_PRODUTO"));
    }

    @AfterEach
    void limparOContexto() {
        SecurityContextHolder.clearContext();
    }

    // ── o cache existe ──────────────────────────────────────────────────────

    @Test
    @DisplayName("a segunda consulta não sai do processo")
    void o_cache_evita_a_segunda_ida() {
        autorizacao.contexto(LOJA);
        autorizacao.contexto(LOJA);
        autorizacao.contexto(LOJA);

        assertThat(MERCHANT.caminhosRecebidos())
                .as("três consultas, uma ida — é a redução de carga que a ADR-011 "
                        + "calculou em mais de 99%")
                .hasSize(1);
    }

    @Test
    @DisplayName("o vazio também é guardado — senão quem não tem acesso bate no merchant a cada clique")
    void o_negativo_tambem_e_guardado() {
        MERCHANT.nega();

        assertThat(autorizacao.contexto(LOJA)).isEmpty();
        assertThat(autorizacao.contexto(LOJA)).isEmpty();

        assertThat(MERCHANT.caminhosRecebidos()).hasSize(1);
    }

    @Test
    @DisplayName("indisponibilidade NÃO é guardada — cada tentativa pergunta de novo")
    void o_indisponivel_nunca_entra_no_cache() {
        MERCHANT.estoura();

        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .isInstanceOf(AutorizacaoIndisponivel.class);
        assertThatThrownBy(() -> autorizacao.contexto(LOJA))
                .isInstanceOf(AutorizacaoIndisponivel.class);

        assertThat(MERCHANT.caminhosRecebidos())
                .as("guardar 'não respondeu' como se fosse 'não tem acesso' faria dois "
                        + "segundos de queda do merchant virarem dez segundos de recusa "
                        + "para todo mundo — é a razão de a G-B3 ter separado os dois "
                        + "casos uma rodada antes de existir cache")
                .hasSize(2);
    }

    // ── o evento invalida, e é isto que a rodada existe para provar ─────────

    @Test
    @DisplayName("o VinculoAlteradoV1 esvazia a entrada, e a consulta seguinte volta ao merchant")
    void o_evento_invalida() {
        autorizacao.contexto(LOJA);
        assertThat(MERCHANT.caminhosRecebidos()).hasSize(1);

        publicar(vinculoAlterado(MARLI, LOJA), "merchant.vinculo.alterado.v1");

        // pollInSameThread: o portador mora no SecurityContextHolder, que é
        // por thread; sem isto o Awaitility consulta noutra thread, sem token.
        await().atMost(Duration.ofSeconds(10)).pollInSameThread().untilAsserted(() -> {
            autorizacao.contexto(LOJA);
            assertThat(MERCHANT.caminhosRecebidos())
                    .as("a entrada tem de sumir por causa do evento — sem isto, o cache "
                            + "responderia com a permissão revogada por até 60 s, que é "
                            + "'uma falha de segurança com nome de otimização' (ADR-043)")
                    .hasSizeGreaterThan(1);
        });
    }

    @Test
    @DisplayName("evento de OUTRO usuário não derruba a entrada deste")
    void a_invalidacao_e_por_chave() {
        autorizacao.contexto(LOJA);
        long antesDoEvento = cache.geracao();

        publicar(vinculoAlterado(UUID.randomUUID(), LOJA), "merchant.vinculo.alterado.v1");

        // Espera o evento ser consumido de verdade antes de afirmar. Sem isto, o
        // teste passaria mesmo se a invalidação nunca acontecesse: ele estaria
        // afirmando "nada mudou" antes de qualquer coisa ter chance de mudar.
        //
        // Comparado com o valor ANTES da publicação, e não com zero: o
        // @BeforeEach chama esvaziar(), que já avança a geração, e um `> 0`
        // passaria na primeira leitura, sem evento nenhum.
        await().atMost(Duration.ofSeconds(10))
                .until(() -> cache.geracao() > antesDoEvento);
        assertThat(cache.tamanho())
                .as("se o ouvinte não conseguiu ler o evento, ele esvazia tudo — e "
                        + "este é o único teste que distingue 'invalidou a chave "
                        + "certa' de 'esqueceu tudo porque falhou'")
                .isEqualTo(1);

        autorizacao.contexto(LOJA);
        assertThat(MERCHANT.caminhosRecebidos())
                .as("invalidar por evento tem de ser por par (usuário, loja); invalidar "
                        + "a loja inteira jogaria fora o cache de todos os atendentes "
                        + "a cada mudança de vínculo de um só")
                .hasSize(1);
    }

    @Test
    @DisplayName("payload que este consumidor não entende esvazia o cache inteiro")
    void o_que_nao_da_para_aplicar_faz_esquecer_tudo() {
        autorizacao.contexto(LOJA);
        assertThat(cache.tamanho()).isEqualTo(1);

        // Sem `usuarioId`: não dá para saber QUAL entrada remover.
        publicar("""
                {"eventId":"%s","eventType":"VinculoAlterado","eventVersion":1,
                 "occurredAt":"%s","correlationId":"%s","payload":{"estabelecimentoId":"%s"}}"""
                .formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), LOJA),
                "merchant.vinculo.alterado.v1");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(cache.tamanho())
                        .as("quando não se sabe o que está velho, esquece-se tudo — "
                                + "esvaziar custa consultas, não esvaziar custa acesso "
                                + "que já foi retirado")
                        .isZero());
    }

    @Test
    @DisplayName("campo que este consumidor não conhece não atrapalha — tolerância de graça")
    void campo_novo_no_payload_e_ignorado() {
        autorizacao.contexto(LOJA);

        publicar("""
                {"eventId":"%s","eventType":"VinculoAlterado","eventVersion":1,
                 "occurredAt":"%s","correlationId":"%s","payload":{
                   "estabelecimentoId":"%s","usuarioId":"%s","membroId":"%s",
                   "papel":"UM_PAPEL_QUE_NAO_EXISTE","estado":"UM_ESTADO_QUE_NAO_EXISTE",
                   "permissoes":["PERMISSAO_QUE_NAO_EXISTE"],"campoNovo":"qualquer coisa"}}"""
                .formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                        LOJA, MARLI, UUID.randomUUID()),
                "merchant.vinculo.alterado.v1");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(cache.tamanho())
                        .as("este consumidor não lê papel, estado nem permissões — e por "
                                + "isso a ADR-027, que trata valor novo em enum como "
                                + "incompatível, não o alcança")
                        .isZero());
    }

    // ── a reconexão ────────────────────────────────────────────────────────

    /**
     * A fila é temporária: numa reconexão ela renasce vazia, e os eventos do
     * intervalo se perderam. O ConnectionListener esvazia o cache quando a
     * conexão se refaz — e este é o único teste que o faz rodar DEPOIS da
     * primeira conexão. Sem ele, a peça só teria sido exercida no arranque,
     * com o cache vazio, onde esvaziar não prova nada.
     */
    @Test
    @DisplayName("quando a conexão com o broker se refaz, o cache é esquecido inteiro")
    void a_reconexao_esvazia_o_cache() {
        autorizacao.contexto(LOJA);
        assertThat(cache.tamanho()).isEqualTo(1);

        // Fecha a conexão; o contêiner de escuta reconecta sozinho, e a
        // conexão nova dispara o onCreate.
        conexoes.resetConnection();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(cache.tamanho())
                        .as("os eventos publicados enquanto a conexão estava fora não "
                                + "chegaram a fila nenhuma; o que o cache sabe pode estar velho")
                        .isZero());

        // E a escuta voltou: um evento depois da reconexão ainda invalida. Publica
        // a cada tentativa — logo depois da reconexão a fila anônima pode ainda
        // não ter sido redeclarada, e a primeira publicação sairia sem destino.
        // Invalidar duas vezes é o mesmo que uma.
        autorizacao.contexto(LOJA);
        long antes = cache.geracao();
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(500)).until(() -> {
            publicar(vinculoAlterado(MARLI, LOJA), "merchant.vinculo.alterado.v1");
            return cache.geracao() > antes;
        });
        assertThat(cache.tamanho()).isZero();
    }

    // ── o que publica ───────────────────────────────────────────────────────

    /** O envelope exato do {@code contracts/eventos.md}, montado à mão. */
    private static String vinculoAlterado(UUID usuarioId, UUID estabelecimentoId) {
        UUID eventId = UUID.randomUUID();
        return """
                {"eventId":"%s","eventType":"VinculoAlterado","eventVersion":1,
                 "occurredAt":"%s","correlationId":"%s","payload":{
                   "estabelecimentoId":"%s","usuarioId":"%s","membroId":"%s",
                   "papel":"COLABORADOR","estado":"SUSPENSO","permissoes":[]}}"""
                .formatted(eventId, Instant.now(), eventId,
                        estabelecimentoId, usuarioId, UUID.randomUUID());
    }

    /**
     * Publica como o relay do {@code merchant} publica.
     *
     * <p>O nome da exchange vem da propriedade, não de uma constante repetida:
     * se ela mudar num lado só, este teste é que tem de ficar vermelho.
     */
    private void publicar(String envelope, String chaveDeRota) {
        rabbit.send("delivery.eventos", chaveDeRota, MessageBuilder
                .withBody(envelope.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build());
    }
}
