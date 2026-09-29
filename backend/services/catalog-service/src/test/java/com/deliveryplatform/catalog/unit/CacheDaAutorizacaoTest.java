package com.deliveryplatform.catalog.unit;

import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.application.port.out.PermissaoDoCatalogo;
import com.deliveryplatform.catalog.config.CacheDaAutorizacaoProperties;
import com.deliveryplatform.catalog.infrastructure.cache.CacheDaAutorizacao;
import com.deliveryplatform.catalog.infrastructure.cache.ChaveDeAutorizacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * O armazenamento sozinho, com prazos curtos — JUnit puro, sem Spring.
 *
 * <p>Os prazos de verdade são 60 s e 10 s, e um teste que os respeitasse levaria
 * um minuto. Aqui eles são 400 ms e 80 ms, e o que se afirma é a
 * <b>proporção</b>: o negativo sai primeiro, o positivo continua. Trocar os dois
 * no {@code Expiry} faria este arquivo ficar vermelho, que é o que ele existe
 * para fazer.
 */
class CacheDaAutorizacaoTest {

    private static final UUID LOJA = UUID.randomUUID();
    private static final String MARLI = UUID.randomUUID().toString();
    private static final String BIA = UUID.randomUUID().toString();

    private static CacheDaAutorizacao cacheCom(Duration positivo, Duration negativo, int teto) {
        return new CacheDaAutorizacao(
                new CacheDaAutorizacaoProperties(positivo, negativo, teto));
    }

    private static CacheDaAutorizacao cacheCurto() {
        return cacheCom(Duration.ofMillis(400), Duration.ofMillis(80), 10_000);
    }

    private static Optional<ContextoDeAcesso> comAcesso(String portador) {
        return Optional.of(new ContextoDeAcesso(
                UUID.fromString(portador), LOJA, EnumSet.of(PermissaoDoCatalogo.VER_PRODUTO)));
    }

    // ── o que entra, sai ────────────────────────────────────────────────────

    @Test
    void guarda_e_devolve() {
        CacheDaAutorizacao cache = cacheCurto();
        ChaveDeAutorizacao chave = new ChaveDeAutorizacao(MARLI, LOJA);

        cache.guardar(chave, comAcesso(MARLI), cache.geracao());

        assertThat(cache.procurar(chave)).isPresent();
        assertThat(cache.procurar(chave).orElseThrow()).isPresent();
    }

    @Test
    @DisplayName("chave ausente é diferente de chave com resposta vazia")
    void ausente_nao_e_o_mesmo_que_vazio() {
        CacheDaAutorizacao cache = cacheCurto();
        ChaveDeAutorizacao semVinculo = new ChaveDeAutorizacao(BIA, LOJA);

        // Nunca consultado: o Optional de fora é vazio.
        assertThat(cache.procurar(semVinculo)).isEmpty();

        cache.guardar(semVinculo, Optional.empty(), cache.geracao());

        // Consultado e sem vínculo: o de fora está presente, o de dentro é vazio.
        // Confundir os dois faria o cache negativo não existir — cada requisição
        // de quem não tem acesso iria ao merchant, que é o que a ADR-011 quer
        // evitar ("senão uma varredura bate direto no merchant-service").
        assertThat(cache.procurar(semVinculo)).isPresent();
        assertThat(cache.procurar(semVinculo).orElseThrow()).isEmpty();
    }

    // ── os dois prazos ──────────────────────────────────────────────────────

    @Test
    @DisplayName("o negativo expira antes do positivo — 10 s contra 60 s, em miniatura")
    void o_negativo_sai_primeiro() {
        CacheDaAutorizacao cache = cacheCurto();
        ChaveDeAutorizacao comVinculo = new ChaveDeAutorizacao(MARLI, LOJA);
        ChaveDeAutorizacao semVinculo = new ChaveDeAutorizacao(BIA, LOJA);

        long geracao = cache.geracao();
        cache.guardar(comVinculo, comAcesso(MARLI), geracao);
        cache.guardar(semVinculo, Optional.empty(), geracao);

        await().atMost(Duration.ofSeconds(2)).until(() -> cache.procurar(semVinculo).isEmpty());

        assertThat(cache.procurar(comVinculo))
                .as("o positivo dura cinco vezes mais; se os dois prazos estiverem "
                        + "trocados, é aqui que aparece")
                .isPresent();
    }

    @Test
    void o_positivo_tambem_expira() {
        CacheDaAutorizacao cache = cacheCurto();
        ChaveDeAutorizacao chave = new ChaveDeAutorizacao(MARLI, LOJA);

        cache.guardar(chave, comAcesso(MARLI), cache.geracao());

        await().atMost(Duration.ofSeconds(3)).until(() -> cache.procurar(chave).isEmpty());
    }

    @Test
    @DisplayName("ler não renova o prazo — senão o usuário mais ativo nunca revalidaria")
    void a_leitura_nao_renova() {
        CacheDaAutorizacao cache = cacheCom(Duration.ofMillis(300), Duration.ofMillis(80), 10_000);
        ChaveDeAutorizacao chave = new ChaveDeAutorizacao(MARLI, LOJA);
        cache.guardar(chave, comAcesso(MARLI), cache.geracao());

        // O `until` lê sem parar. Se o expireAfterRead renovasse o prazo, a
        // entrada nunca expiraria e este await estouraria — e a "janela de
        // tolerância" da ADR-011 deixaria de existir exatamente para quem mais
        // usa o sistema.
        await().atMost(Duration.ofSeconds(3)).until(() -> cache.procurar(chave).isEmpty());
    }

    // ── o teto ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("o teto de entradas é respeitado — 10 000 na ADR, 5 aqui")
    void o_teto_descarta() {
        CacheDaAutorizacao cache = cacheCom(Duration.ofMinutes(1), Duration.ofMinutes(1), 5);

        for (int i = 0; i < 200; i++) {
            cache.guardar(new ChaveDeAutorizacao(UUID.randomUUID().toString(), LOJA),
                    Optional.empty(), cache.geracao());
        }

        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(cache.tamanho())
                        .as("sem teto, um cache de autorização cresce com o número de "
                                + "usuários e o processo morre de memória antes de "
                                + "alguém notar")
                        .isLessThanOrEqualTo(5));
    }

    // ── a corrida, que é a parte difícil ────────────────────────────────────

    @Test
    @DisplayName("resposta que chega depois de uma invalidação NÃO é guardada")
    void a_corrida_da_invalidacao() {
        CacheDaAutorizacao cache = cacheCom(Duration.ofMinutes(1), Duration.ofMinutes(1), 10_000);
        ChaveDeAutorizacao chave = new ChaveDeAutorizacao(MARLI, LOJA);

        // A requisição começa: lê a geração e sai para perguntar ao merchant.
        long geracaoDaLeitura = cache.geracao();

        // Enquanto ela está fora, o evento chega e invalida.
        cache.invalidar(chave);

        // A resposta volta — com o contexto de ANTES da revogação.
        boolean guardou = cache.guardar(chave, comAcesso(MARLI), geracaoDaLeitura);

        assertThat(guardou)
                .as("sem a geração, esta linha guardaria por 60 s uma permissão que "
                        + "acabou de ser revogada, DEPOIS de o evento ter sido aplicado — "
                        + "o evento chegaria, seria processado, e não adiantaria nada")
                .isFalse();
        assertThat(cache.procurar(chave)).isEmpty();
    }

    @Test
    @DisplayName("invalidar uma chave descarta leituras em voo de QUALQUER chave")
    void a_geracao_e_global() {
        CacheDaAutorizacao cache = cacheCom(Duration.ofMinutes(1), Duration.ofMinutes(1), 10_000);

        long geracaoDaLeitura = cache.geracao();
        cache.invalidar(new ChaveDeAutorizacao(BIA, LOJA));

        boolean guardou = cache.guardar(
                new ChaveDeAutorizacao(MARLI, LOJA), comAcesso(MARLI), geracaoDaLeitura);

        assertThat(guardou)
                .as("é exagerado de propósito: descartar uma leitura boa custa uma "
                        + "consulta; guardar uma ruim custa acesso indevido")
                .isFalse();
    }

    // ── esquecer tudo ───────────────────────────────────────────────────────

    @Test
    void esvaziar_apaga_tudo_e_avanca_a_geracao() {
        CacheDaAutorizacao cache = cacheCom(Duration.ofMinutes(1), Duration.ofMinutes(1), 10_000);
        long geracaoDaLeitura = cache.geracao();
        cache.guardar(new ChaveDeAutorizacao(MARLI, LOJA), comAcesso(MARLI), geracaoDaLeitura);
        cache.guardar(new ChaveDeAutorizacao(BIA, LOJA), Optional.empty(), geracaoDaLeitura);

        cache.esvaziar("teste");

        assertThat(cache.tamanho()).isZero();
        assertThat(cache.guardar(new ChaveDeAutorizacao(MARLI, LOJA),
                comAcesso(MARLI), geracaoDaLeitura))
                .as("esvaziar também descarta o que estava em voo — senão uma resposta "
                        + "em trânsito repovoaria o cache que acabou de ser esquecido")
                .isFalse();
    }

    @Test
    void invalidar_chave_que_nao_esta_la_nao_estoura() {
        CacheDaAutorizacao cache = cacheCurto();

        cache.invalidar(new ChaveDeAutorizacao(MARLI, LOJA));

        assertThat(cache.tamanho()).isZero();
    }

    // ── a chave ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("a chave do evento e a do token são a mesma quando o sub é UUID")
    void as_duas_formas_da_chave_coincidem() {
        UUID usuario = UUID.randomUUID();

        assertThat(ChaveDeAutorizacao.doEvento(usuario, LOJA))
                .as("se estas duas divergirem, o evento invalida uma entrada que "
                        + "ninguém lê, e quem lê guarda numa que ninguém invalida — "
                        + "o cache pareceria funcionar e nunca seria invalidado")
                .isEqualTo(new ChaveDeAutorizacao(usuario.toString(), LOJA));
    }

    @Test
    void o_sub_que_nao_e_uuid_vira_chave_do_mesmo_jeito() {
        // O token com sub "nao-sou-um-uuid" atravessa a cadeia de filtros deste
        // serviço (teste da G-B3). Converter aqui estouraria antes de negar.
        assertThat(new ChaveDeAutorizacao("nao-sou-um-uuid", LOJA).portador())
                .isEqualTo("nao-sou-um-uuid");
    }
}
