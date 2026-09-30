package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.in.LojaDoUsuario;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
import com.deliveryplatform.merchant.domain.model.Identificacao;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rota que o front chama depois de entrar, atravessando a cadeia inteira: a
 * cadeia de filtros, o JWKS pela rede, emissor e audiência, o {@code sub}
 * convertido na borda, as duas consultas e a serialização.
 *
 * <p>O arranjo é o do {@code EquipeControllerIT}: mesmo {@code webEnvironment},
 * mesmo {@code IdentityDeMentira}, dados semeados pelos repositórios. O banco é
 * compartilhado entre classes ({@code Infraestrutura}), e por isso cada caso usa
 * pessoas e lojas novas — nenhuma asserção olha a tabela inteira.
 *
 * <p>Os dois casos que mais importam:
 *
 * <ul>
 *   <li><b>sem vínculo nenhum</b> devolve {@code 200} com {@code []}, e não 403
 *       nem 404 — é o estado de quem acabou de se cadastrar, e a primeira tela
 *       que o front mostra a essa pessoa;</li>
 *   <li><b>vínculo suspenso</b> é o único caso que falha sozinho se alguém
 *       tirar o {@code filter(Membro::ativo)} do caso de uso.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.outbox.habilitado=false")
class MinhasLojasControllerIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();

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

    @Autowired
    MembroRepositorio membros;

    @Autowired
    EstabelecimentoRepositorio lojas;

    private UUID pizzaria;
    private UUID padaria;

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @BeforeEach
    void duasLojas() {
        pizzaria = lojas.salvar(LojaDeTeste.pizzaria()).getId();
        padaria = lojas.salvar(Estabelecimento.novo(
                new Identificacao("Padaria da Bia", LojaDeTeste.documento(),
                        LojaDeTeste.telefone(), "Rua do Sol, 20", "Pina", FusoHorario.PADRAO),
                LojaDeTeste.operacao(), LojaDeTeste.troco(),
                LojaDeTeste.disponibilidade(), LojaDeTeste.areas())).getId();
    }

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec pedirMinhasLojas(String token) {
        var requisicao = cliente().get().uri("/api/v1/me/estabelecimentos");
        if (token != null) {
            requisicao = requisicao.header("Authorization", "Bearer " + token);
        }
        return requisicao.exchange();
    }

    private List<LojaDoUsuario> lojasDe(UUID usuario) {
        LojaDoUsuario[] corpo = pedirMinhasLojas(IDENTITY.tokenDe(usuario))
                .expectStatus().isOk()
                .expectBody(LojaDoUsuario[].class)
                .returnResult().getResponseBody();
        assertThat(corpo).as("200 sempre traz corpo — uma lista, mesmo vazia").isNotNull();
        return List.of(corpo);
    }

    private Membro vinculo(UUID usuario, UUID loja, EstadoDoMembro estado, EnumSet<Permissao> permissoes) {
        return membros.salvar(Membro.reconstituir(UUID.randomUUID(), usuario, loja,
                Papel.COLABORADOR, permissoes, estado, agora(), agora()));
    }

    // ── o caminho feliz ─────────────────────────────────────────────────────

    @Test
    @DisplayName("vínculo ativo em duas lojas: duas linhas, com nome, papel e permissões")
    void duas_lojas_ativas() {
        UUID marli = UUID.randomUUID();
        membros.salvar(Membro.fundador(marli, pizzaria, agora()));
        vinculo(marli, padaria, EstadoDoMembro.ATIVO, EnumSet.of(Permissao.VER_PEDIDO));

        List<LojaDoUsuario> resposta = lojasDe(marli);

        assertThat(resposta)
                .extracting(LojaDoUsuario::nome)
                .as("ordenadas por nome, e com o nome que só o Estabelecimento tem")
                .containsExactly("Padaria da Bia", "Pizzaria da Marli");
        assertThat(resposta.get(1).estabelecimentoId()).isEqualTo(pizzaria);
        assertThat(resposta.get(1).papel()).isEqualTo(Papel.ADMINISTRADOR);
    }

    @Test
    @DisplayName("as permissões são as GRAVADAS no vínculo, e não as deduzidas do papel")
    void permissoes_sao_as_gravadas() {
        UUID bia = UUID.randomUUID();
        vinculo(bia, padaria, EstadoDoMembro.ATIVO, EnumSet.of(Permissao.VER_PEDIDO));

        assertThat(lojasDe(bia).getFirst().permissoes())
                .as("um colaborador com uma só permissão recebe exatamente essa — "
                        + "estabelecimento.md §2: papel não é lista de permissões")
                .containsExactly(Permissao.VER_PEDIDO);
    }

    // ── os vazios, que são resposta ─────────────────────────────────────────

    @Test
    @DisplayName("sem vínculo nenhum: 200 e [] — o estado de quem acabou de se cadastrar")
    void sem_vinculo_e_lista_vazia() {
        assertThat(lojasDe(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("vínculo SUSPENSO na única loja: 200 e [] — sem o filtro, a loja apareceria")
    void suspenso_nao_aparece() {
        UUID afastada = UUID.randomUUID();
        vinculo(afastada, pizzaria, EstadoDoMembro.SUSPENSO, EnumSet.of(Permissao.VER_PEDIDO));

        assertThat(lojasDe(afastada))
                .as("o repositório devolve o vínculo suspenso; quem o tira da lista "
                        + "é o filter(Membro::ativo) do caso de uso")
                .isEmpty();
    }

    // ── o portador, e só ele ────────────────────────────────────────────────

    @Test
    @DisplayName("a pessoa A não vê a loja da pessoa B")
    void a_nao_ve_a_loja_de_b() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        vinculo(a, pizzaria, EstadoDoMembro.ATIVO, EnumSet.of(Permissao.VER_PEDIDO));
        vinculo(b, padaria, EstadoDoMembro.ATIVO, EnumSet.of(Permissao.VER_PEDIDO));

        assertThat(lojasDe(a))
                .extracting(LojaDoUsuario::estabelecimentoId)
                .as("um caso de uso que ignorasse o sub passaria em todos os outros casos")
                .containsExactly(pizzaria);
    }

    // ── o token ─────────────────────────────────────────────────────────────

    @Test
    void sem_token_e_401() {
        pedirMinhasLojas(null).expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("token de outro emissor é 401 — /me passa pelos mesmos validadores que /merchants")
    void token_de_outro_emissor_e_401() {
        pedirMinhasLojas(IDENTITY.tokenComEmissor("http://homologacao", UUID.randomUUID()))
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("sub que não é UUID é 403, e não 500 — o SujeitoDoToken, como nas outras rotas")
    void sub_que_nao_e_uuid_e_403() {
        pedirMinhasLojas(IDENTITY.tokenComSujeito("nao-sou-um-uuid"))
                .expectStatus().isForbidden();
    }
}
