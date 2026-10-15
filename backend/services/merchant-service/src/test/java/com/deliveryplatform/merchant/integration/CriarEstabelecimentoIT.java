package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.in.LojaDoUsuario;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import com.deliveryplatform.merchant.infrastructure.outbox.OutboxJpaEntity;
import com.deliveryplatform.merchant.infrastructure.outbox.OutboxSpringDataRepository;
import com.deliveryplatform.merchant.support.IdentityDeMentira;
import com.deliveryplatform.merchant.support.Infraestrutura;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rota de criação contra o serviço de verdade (ADR-060). O 201 não é o que
 * importa: <b>o que prova que o marco 1 ganhou a loja é lê-la de volta pela rota que
 * o painel usa</b>, e encontrar no outbox o vínculo que nasceu com ela.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.outbox.habilitado=false")
class CriarEstabelecimentoIT extends Infraestrutura {

    private static final IdentityDeMentira IDENTITY = IdentityDeMentira.subir();

    static final String CORPO = """
            {
              "identificacao": {
                "nome": "%s",
                "documento": "%s",
                "telefone": "(81) 98765-4321",
                "enderecoTextual": "Rua da Aurora, 10",
                "bairro": "Boa Vista",
                "fusoHorario": "America/Recife"
              },
              "politicaDeTroco": { "fundoMaximoDeTroco": 50.00, "aceitaPedidoSemTrocoDisponivel": false },
              "tipoDeOperacao": "PRODUCAO",
              "metodosPorModalidade": { "ENTREGA": ["DINHEIRO", "PIX"], "RETIRADA": ["CARTAO"] }
            }
            """;

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
    OutboxSpringDataRepository outbox;

    private RestTestClient cliente() {
        return RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private RestTestClient.ResponseSpec criar(String token, String corpo) {
        var requisicao = cliente().post().uri("/api/v1/me/estabelecimentos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo);
        if (token != null) {
            requisicao = requisicao.header("Authorization", "Bearer " + token);
        }
        return requisicao.exchange();
    }

    @Test
    void criar_e_ler_de_volta_pela_rota_do_painel_e_o_vinculo_esta_no_outbox() {
        UUID portador = UUID.randomUUID();
        String token = IDENTITY.tokenDe(portador);

        LojaDoUsuario criada = criar(token, CORPO.formatted("Cantina do Zé", "12.345.678/0001-95"))
                .expectStatus().isCreated()
                .expectHeader().doesNotExist("Location")
                .expectBody(LojaDoUsuario.class).returnResult().getResponseBody();

        assertThat(criada).isNotNull();
        assertThat(criada.nome()).isEqualTo("Cantina do Zé");
        assertThat(criada.papel()).isEqualTo(Papel.ADMINISTRADOR);

        LojaDoUsuario[] minhas = cliente().get().uri("/api/v1/me/estabelecimentos")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LojaDoUsuario[].class).returnResult().getResponseBody();
        assertThat(minhas).isNotNull().hasSize(1);
        assertThat(minhas[0].estabelecimentoId()).isEqualTo(criada.estabelecimentoId());
        assertThat(minhas[0].papel()).isEqualTo(Papel.ADMINISTRADOR);
        assertThat(minhas[0].permissoes()).contains(Permissao.VER_PRODUTO, Permissao.GERENCIAR_EQUIPE);

        Membro dono = membros.buscarPorUsuarioELoja(portador, criada.estabelecimentoId()).orElseThrow();
        List<OutboxJpaEntity> doVinculo = outbox.findAll().stream()
                .filter(linha -> dono.getId().equals(linha.getAgregadoId()))
                .toList();
        assertThat(doVinculo).hasSize(1);
        assertThat(doVinculo.get(0).getTipo()).isEqualTo("VinculoAlterado");
    }

    @Test
    void a_mesma_pessoa_cria_duas_lojas_com_o_mesmo_documento() {
        String token = IDENTITY.tokenDe(UUID.randomUUID());

        criar(token, CORPO.formatted("Loja Um", "123.456.789-09")).expectStatus().isCreated();
        criar(token, CORPO.formatted("Loja Dois", "123.456.789-09")).expectStatus().isCreated();
    }

    /**
     * O 400 por motivo (W-E): o `detail` é a frase que o tratador escreve, nunca a
     * mensagem do agregado. Cinco dos seis motivos são alcançáveis pela borda; o de
     * texto obrigatório o `@NotBlank` do DTO recusa antes, e mora no teste de unidade.
     */
    static Stream<Arguments> recusas() {
        String valido = CORPO.formatted("Loja", "123.456.789-09");
        return Stream.of(
                Arguments.of("documento", CORPO.formatted("Loja", "123"),
                        "documento inválido: informe um CPF ou um CNPJ"),
                Arguments.of("telefone", valido.replace("(81) 98765-4321", "não é telefone"),
                        "telefone inválido"),
                Arguments.of("fuso", valido.replace("America/Recife", "Europe/Lisbon"),
                        "fuso horário inválido"),
                Arguments.of("modalidade sem forma de pagar",
                        valido.replace("{ \"ENTREGA\": [\"DINHEIRO\", \"PIX\"], \"RETIRADA\": [\"CARTAO\"] }", "{ }"),
                        "escolha ao menos uma modalidade, e uma forma de pagar para cada uma"),
                Arguments.of("fundo de troco", valido.replace("50.00", "-1.00"),
                        "o fundo máximo de troco não pode ser negativo"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("recusas")
    void o_400_diz_a_frase_do_tratador_e_nao_a_do_agregado(String motivo, String corpo, String frase) {
        String resposta = criar(IDENTITY.tokenDe(UUID.randomUUID()), corpo)
                .expectStatus().isBadRequest()
                .expectBody(String.class).returnResult().getResponseBody();

        assertThat(resposta).contains("\"detail\":\"" + frase + "\"");
        // A mensagem do agregado não vai à tela: nem o código da invariante, nem o valor digitado.
        assertThat(resposta).doesNotContain("M12").doesNotContain("Europe/Lisbon").doesNotContain("não é telefone");
    }

    @Test
    void corpo_sem_identificacao_e_400_antes_do_agregado() {
        criar(IDENTITY.tokenDe(UUID.randomUUID()), """
                { "tipoDeOperacao": "PRODUCAO", "metodosPorModalidade": { "RETIRADA": ["DINHEIRO"] } }
                """)
                .expectStatus().isBadRequest();
    }

    @Test
    void sem_token_e_401() {
        criar(null, CORPO.formatted("Loja", "123.456.789-09")).expectStatus().isUnauthorized();
    }
}
