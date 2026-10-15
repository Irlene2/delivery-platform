package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.out.ConviteRepositorio;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Convite;
import com.deliveryplatform.merchant.domain.model.EstadoDoConvite;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import com.deliveryplatform.merchant.domain.model.Telefone;
import com.deliveryplatform.merchant.support.EquipeDeTeste;
import com.deliveryplatform.merchant.support.IdentityDeMentira;
import com.deliveryplatform.merchant.support.Infraestrutura;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ver e aceitar o convite (ADR-069 §2 a §5) — o par público.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.outbox.habilitado=false")
class ConviteIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();
    private static final String NAO_VALE = "este convite não vale mais; peça outro a quem te convidou";

    @DynamicPropertySource
    static void apontarParaOIdentityDeMentira(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", IDENTITY::jwksUri);
        registry.add("delivery.jwt.issuer", () -> IdentityDeMentira.EMISSOR);
        registry.add("delivery.jwt.audience", () -> IdentityDeMentira.AUDIENCIA);
    }

    @AfterAll
    static void derrubar() {
        IDENTITY.close();
    }

    @LocalServerPort
    int porta;

    @Autowired MembroRepositorio membros;
    @Autowired EstabelecimentoRepositorio lojas;
    @Autowired ConviteRepositorio convites;

    private UUID loja;
    private UUID marli;
    private Membro junior;

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @BeforeEach
    void umaPizzaria() {
        loja = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        marli = membros.salvar(Membro.fundador(UUID.randomUUID(), loja, agora())).getUsuarioId();
        junior = membros.salvar(Membro.colaborador(
                UUID.randomUUID(), loja, EquipeDeTeste.DO_JUNIOR, agora()));
    }

    // ── B · ver ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("B6/B7 · o público responde sem token nenhum, com a loja, o papel e a validade")
    void ver_sem_token() {
        String token = convidar(marli);

        Map<String, Object> visto = ver(token).expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();

        assertThat(visto.get("nomeDaLoja")).isEqualTo(LojaDeTeste.pizzaria().getIdentificacao().nome());
        assertThat(visto.get("papel")).isEqualTo("COLABORADOR");
        assertThat(visto.get("expiraEm")).isNotNull();
    }

    /** O conjunto de campos, e não um deles: um campo novo acrescentado sem pensar cai aqui. */
    @Test
    @DisplayName("B8 · e não traz mais nada: nem telefone, nem pessoa, nem o próprio token")
    void ver_nao_traz_mais_nada() {
        String token = convidar(marli);

        String corpo = ver(token).expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        Map<String, Object> visto = ver(token).expectBody(Map.class).returnResult().getResponseBody();

        assertThat(visto.keySet()).containsExactlyInAnyOrder("nomeDaLoja", "papel", "expiraEm");
        assertThat(corpo)
                .doesNotContain(token)
                .doesNotContain("98765")
                .doesNotContain(marli.toString())
                .doesNotContain(junior.getId().toString());
    }

    @Test
    @DisplayName("B9 · token inexistente é 404, com a frase")
    void ver_inexistente_e_404() {
        ProblemDetail recusa = ver("nao-existe").expectStatus().isNotFound()
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo(NAO_VALE);
    }

    // ── C · aceitar ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("C10/C12 · aceitar com sessão e sem vínculo nenhum entra na loja, COLABORADOR — pelo banco")
    void aceitar_entra_na_loja() {
        String token = convidar(marli);
        UUID novata = UUID.randomUUID();

        aceitar(token, novata).expectStatus().isOk();

        Membro vinculo = membros.buscarPorUsuarioELoja(novata, loja).orElseThrow();
        assertThat(vinculo.getEstado()).isEqualTo(EstadoDoMembro.ATIVO);
        assertThat(vinculo.getPapel()).isEqualTo(Papel.COLABORADOR);
        assertThat(vinculo.getPermissoes()).containsExactly(Permissao.VER_PEDIDO);
        assertThat(convites.buscarPorToken(token).orElseThrow().getEstado())
                .isEqualTo(EstadoDoConvite.ACEITO);
    }

    @Test
    @DisplayName("C11 · sem sessão é 401, e não 403")
    void aceitar_sem_sessao_e_401() {
        String token = convidar(marli);

        cliente().post().uri("/api/v1/convites/" + token + "/aceite").exchange()
                .expectStatus().isUnauthorized();
    }

    /** Os três num caso só, e a asserção é de igualdade entre os três corpos. */
    @Test
    @DisplayName("C13 · expirado, já usado e inexistente são o mesmo 404, com o mesmo corpo")
    void tres_motivos_uma_resposta() {
        String usado = convidar(marli);
        aceitar(usado, UUID.randomUUID()).expectStatus().isOk();
        String expirado = conviteExpirado();

        String doUsado = recusa(aceitar(usado, UUID.randomUUID()));
        String doExpirado = recusa(aceitar(expirado, UUID.randomUUID()));
        String doInexistente = recusa(aceitar("nao-existe", UUID.randomUUID()));

        assertThat(doUsado).isEqualTo(doExpirado).isEqualTo(doInexistente).contains(NAO_VALE);
        assertThat(aceitar(usado, UUID.randomUUID()).expectBody(String.class).returnResult()
                .getResponseBody())
                .as("a recusa não ecoa o token, nem no instance")
                .doesNotContain(usado);
        assertThat(semInstante(ver(expirado).expectStatus().isNotFound()
                .expectBody(String.class).returnResult().getResponseBody()))
                .as("a leitura pública também não distingue")
                .isEqualTo(semInstante(ver("nao-existe").expectStatus().isNotFound()
                        .expectBody(String.class).returnResult().getResponseBody()));
    }

    /**
     * A regra que a B2 escreveu e que nenhuma rota nunca exerceu: entre a emissão e o
     * aceite, o convidante perde a permissão que concedia — e o aceite é recusado.
     */
    @Test
    @DisplayName("C14 · a A2 é conferida no aceite: o convidante perdeu GERENCIAR_EQUIPE, e o aceite é 409")
    void a2_no_aceite() {
        String token = convidar(junior.getUsuarioId());
        membros.salvar(Membro.reconstituir(junior.getId(), junior.getUsuarioId(), loja,
                Papel.COLABORADOR, EnumSet.of(Permissao.VER_PEDIDO), EstadoDoMembro.ATIVO,
                junior.getCriadoEm(), agora()));
        UUID novata = UUID.randomUUID();

        ProblemDetail recusa = aceitar(token, novata).expectStatus().isEqualTo(409)
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail())
                .isEqualTo("quem te convidou não pode mais conceder este acesso; peça um novo convite");
        assertThat(membros.buscarPorUsuarioELoja(novata, loja)).isEmpty();
        assertThat(convites.buscarPorToken(token).orElseThrow().getEstado())
                .isEqualTo(EstadoDoConvite.PENDENTE);
    }

    /**
     * Não é descuido: o token autoriza, o telefone endereça (B2). Quem aceita não prova o
     * número — conferir exigiria perguntar ao identity, e a porta não existe.
     */
    @Test
    @DisplayName("C15 · o telefone não é conferido: quem tem o token entra, seja qual for o número")
    void telefone_nao_e_conferido() {
        String token = convidar(marli, "(11) 91234-5678");
        UUID outraPessoa = UUID.randomUUID();

        aceitar(token, outraPessoa).expectStatus().isOk();

        assertThat(membros.buscarPorUsuarioELoja(outraPessoa, loja)).isPresent();
    }

    @Test
    @DisplayName("quem já é da equipe e aceita: 409, com a frase")
    void ja_e_da_equipe() {
        String token = convidar(marli);

        ProblemDetail recusa = aceitar(token, junior.getUsuarioId()).expectStatus().isEqualTo(409)
                .expectBody(ProblemDetail.class).returnResult().getResponseBody();

        assertThat(recusa.getDetail()).isEqualTo("você já faz parte da equipe desta loja");
    }

    // ── apoio ───────────────────────────────────────────────────────────────

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private String convidar(UUID quem) {
        return convidar(quem, "(81) 98765-4321");
    }

    @SuppressWarnings("unchecked")
    private String convidar(UUID quem, String telefone) {
        Map<String, Object> emitido = cliente().post().uri("/api/v1/merchants/" + loja + "/team/convites")
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .body(ConvidarIT.CORPO.formatted(telefone, "\"VER_PEDIDO\""))
                .exchange().expectStatus().isCreated()
                .expectBody(Map.class).returnResult().getResponseBody();
        return (String) emitido.get("token");
    }

    /** Um convite gravado já vencido: o relógio passou da validade. */
    private String conviteExpirado() {
        Instant criado = agora().minus(Duration.ofDays(8));
        Convite velho = Convite.reconstituir(UUID.randomUUID(), loja,
                Telefone.de("(81) 98765-4321"), "token-de-oito-dias-atras-" + UUID.randomUUID(),
                EnumSet.of(Permissao.VER_PEDIDO), junior.getId(), EstadoDoConvite.PENDENTE,
                criado, criado.plus(Convite.VALIDADE), null);
        return convites.salvar(velho).getToken();
    }

    private RestTestClient.ResponseSpec ver(String token) {
        return cliente().get().uri("/api/v1/convites/" + token).exchange();
    }

    private RestTestClient.ResponseSpec aceitar(String token, UUID quem) {
        return cliente().post().uri("/api/v1/convites/" + token + "/aceite")
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(quem))
                .exchange();
    }

    private static String recusa(RestTestClient.ResponseSpec resposta) {
        return semInstante(resposta.expectStatus().isNotFound()
                .expectBody(String.class).returnResult().getResponseBody());
    }

    /** O {@code instance} é o caminho de cada pedido — e carrega o token; o resto tem de ser igual. */
    private static String semInstante(String corpo) {
        return corpo.replaceAll("\"instance\":\"[^\"]*\"", "");
    }
}
