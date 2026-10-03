package com.deliveryplatform.catalog.contract;

import com.deliveryplatform.catalog.support.IdentityDeMentira;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Congela o contrato HTTP do {@code catalog} (ADR-039). Terceiro arquivo de
 * {@code contracts/openapi/}.
 *
 * <p>Cópia do {@code ContratoOpenApiIT} do {@code merchant} — a ADR-001 proíbe
 * compartilhá-lo —, com o token vindo do {@link IdentityDeMentira}. O
 * {@code /v3/api-docs} deste serviço também exige token: aqui, como lá, não
 * existe rota pública.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoOpenApiIT extends Infraestrutura {

    private static final String SERVICO = "catalog-service";
    private static final String CAMINHO_DO_DOCUMENTO = "/v3/api-docs";

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

    @Test
    void o_contrato_commitado_e_o_que_o_codigo_expoe() throws Exception {
        String gerado = normalizar(buscarDocumento());
        Path arquivo = arquivoDoContrato();

        if (Boolean.getBoolean("openapi.atualizar")) {
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, gerado);
            return;
        }

        assertThat(Files.exists(arquivo))
                .as("%s não existe. Gere com: ./gradlew :services:%s:test -Dopenapi.atualizar=true",
                        arquivo, SERVICO)
                .isTrue();

        assertThat(gerado)
                .as("o contrato commitado divergiu do que o código expõe. Se a mudança de "
                        + "superfície foi intencional, regrave com -Dopenapi.atualizar=true e "
                        + "revise o diff — é ele que torna a mudança visível")
                .isEqualTo(Files.readString(arquivo));
    }

    /**
     * <b>O {@code /v3/api-docs} deste serviço exige token como qualquer outra
     * rota</b>, e não por configuração especial: é que aqui não existe rota
     * pública. A ADR-039 §4 já queria isso — o contrato é publicado pelo
     * repositório, não por um endpoint aberto que descreve a superfície inteira
     * para quem passar.
     */
    private String buscarDocumento() {
        var resposta = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + porta)
                .build()
                .get().uri(CAMINHO_DO_DOCUMENTO)
                .header("Authorization", "Bearer " + IDENTITY.tokenDe(UUID.randomUUID()))
                .exchange()
                .expectBody(String.class)
                .returnResult();

        assertThat(resposta.getStatus())
                .as("o token é válido e o sub não tem vínculo em loja nenhuma — o que não "
                        + "importa aqui: /v3/api-docs exige autenticação, não autorização "
                        + "de estabelecimento")
                .isEqualTo(HttpStatus.OK);

        return resposta.getResponseBody();
    }

    /** Forma canônica: chaves ordenadas e sem {@code servers}. Igual à do identity. */
    private String normalizar(String json) throws Exception {
        ObjectMapper mapeador = JsonMapper.builder()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .build();

        ObjectNode raiz = (ObjectNode) mapeador.readTree(json);
        raiz.remove("servers");

        DefaultPrettyPrinter impressora = new DefaultPrettyPrinter();
        impressora.indentObjectsWith(new DefaultIndenter("  ", "\n"));

        return mapeador.writer(impressora).writeValueAsString(raiz) + "\n";
    }

    private static Path arquivoDoContrato() {
        Path atual = Path.of("").toAbsolutePath();

        while (atual != null && !Files.isDirectory(atual.resolve("contracts"))) {
            atual = atual.getParent();
        }

        if (atual == null) {
            throw new IllegalStateException(
                    "não encontrei o diretório contracts/ subindo a partir de "
                            + Path.of("").toAbsolutePath());
        }

        return atual.resolve("contracts").resolve("openapi").resolve(SERVICO + ".json");
    }
}
