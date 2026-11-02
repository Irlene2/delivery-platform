package com.deliveryplatform.identity.api.controller;

import com.deliveryplatform.identity.api.dto.CodigoDeDemonstracaoResponse;
import com.deliveryplatform.identity.api.dto.PasswordResetRequest;
import com.deliveryplatform.identity.api.dto.VerificationCodeRequest;
import com.deliveryplatform.identity.application.port.in.RedefinirSenha;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Esqueci minha senha, em dois passos, sob {@code /api/v1/auth} como o cadastro — o gateway já
 * roteia o prefixo, e nenhuma rota nova é preciso lá (ADR-042 §1).
 *
 * <ol>
 *   <li>{@code POST /password-reset/code} com o telefone: <b>202 sempre</b>, tenha o telefone
 *       conta ou não. Com a bandeira de desenvolvimento, o 202 traz o código — o mesmo oráculo do
 *       cadastro, e pelo mesmo motivo só no ambiente local e de teste.</li>
 *   <li>{@code POST /password-reset} com telefone, código e senha nova: <b>204</b>, ou 400 com a
 *       recusa única do código.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class RecuperacaoDeSenhaController {

    private final RedefinirSenha redefinicao;
    private final boolean codigoNaResposta;

    public RecuperacaoDeSenhaController(
            RedefinirSenha redefinicao,
            @Value("${delivery.cadastro.codigo-na-resposta:false}") boolean codigoNaResposta) {
        this.redefinicao = redefinicao;
        this.codigoNaResposta = codigoNaResposta;
    }

    @PostMapping(path = "/code", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CodigoDeDemonstracaoResponse> solicitarCodigo(
            @Valid @RequestBody VerificationCodeRequest requisicao) {
        var codigo = redefinicao.solicitarCodigo(requisicao.telefone());
        if (!codigoNaResposta) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.accepted()
                .body(new CodigoDeDemonstracaoResponse(codigo.orElse(null)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void redefinir(@Valid @RequestBody PasswordResetRequest requisicao) {
        redefinicao.redefinir(requisicao.telefone(), requisicao.codigo(), requisicao.novaSenha());
    }
}
