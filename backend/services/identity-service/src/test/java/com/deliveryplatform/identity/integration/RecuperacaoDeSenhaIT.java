package com.deliveryplatform.identity.integration;

import com.deliveryplatform.identity.api.dto.CodigoDeDemonstracaoResponse;
import com.deliveryplatform.identity.api.dto.LoginRequest;
import com.deliveryplatform.identity.api.dto.PasswordResetRequest;
import com.deliveryplatform.identity.api.dto.VerificationCodeRequest;
import com.deliveryplatform.identity.application.port.out.CodificadorDeSenha;
import com.deliveryplatform.identity.application.port.out.UsuarioRepositorio;
import com.deliveryplatform.identity.domain.model.Telefone;
import com.deliveryplatform.identity.domain.model.Usuario;
import com.deliveryplatform.identity.support.GeradorDeChaveDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Esqueci minha senha, de ponta a ponta (02/11/2026): pedir o código, errar, acertar, e entrar
 * com a senha nova — a antiga para de valer. Com a bandeira de desenvolvimento, para ler o código
 * da resposta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "delivery.cadastro.codigo-na-resposta=true")
@Testcontainers
class RecuperacaoDeSenhaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void chaveDeAssinatura(DynamicPropertyRegistry registry) {
        registry.add("delivery.jwt.private-key-path", GeradorDeChaveDeTeste::caminhoDaChaveUnica);
    }

    @LocalServerPort
    int porta;

    @Autowired
    UsuarioRepositorio usuarios;

    @Autowired
    CodificadorDeSenha codificador;

    RestTestClient http;

    @BeforeEach
    void construirCliente() {
        http = RestTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
    }

    private String pedirCodigo(String telefone) {
        CodigoDeDemonstracaoResponse resposta = http.post().uri("/api/v1/auth/password-reset/code")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new VerificationCodeRequest(telefone))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(CodigoDeDemonstracaoResponse.class).returnResult().getResponseBody();
        assertThat(resposta).isNotNull();
        return resposta.codigoDeDemonstracao();
    }

    private int entrar(String telefone, String senha) {
        return http.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(telefone, senha))
                .exchange()
                .expectBody()
                .returnResult()
                .getStatus()
                .value();
    }

    @Test
    void troca_a_senha_com_o_codigo_e_a_antiga_para_de_valer() {
        String telefone = "11933330001";
        usuarios.salvar(Usuario.novo("Marli", Telefone.de(telefone), Instant.now(),
                codificador.codificar("senha-antiga-1")));

        String codigo = pedirCodigo(telefone);
        assertThat(codigo).hasSize(6);

        http.post().uri("/api/v1/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PasswordResetRequest(telefone, "000000".equals(codigo) ? "111111" : "000000",
                        "senha-nova-123"))
                .exchange()
                .expectStatus().isBadRequest();

        http.post().uri("/api/v1/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PasswordResetRequest(telefone, codigo, "senha-nova-123"))
                .exchange()
                .expectStatus().isNoContent();

        assertThat(entrar(telefone, "senha-nova-123")).isEqualTo(200);
        assertThat(entrar(telefone, "senha-antiga-1")).isEqualTo(401);
    }

    @Test
    void telefone_sem_conta_recebe_o_mesmo_202_e_nenhum_codigo() {
        assertThat(pedirCodigo("11933339999")).isNull();
    }

    @Test
    void sem_codigo_pedido_e_a_recusa_do_codigo() {
        http.post().uri("/api/v1/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PasswordResetRequest("11933338888", "123456", "senha-nova-123"))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
