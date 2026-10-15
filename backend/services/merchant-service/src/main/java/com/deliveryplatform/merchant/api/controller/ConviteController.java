package com.deliveryplatform.merchant.api.controller;

import com.deliveryplatform.merchant.api.dto.AceiteResponse;
import com.deliveryplatform.merchant.api.dto.ConvitePublicoResponse;
import com.deliveryplatform.merchant.api.seguranca.SujeitoDoToken;
import com.deliveryplatform.merchant.application.port.in.AceitarConvite;
import com.deliveryplatform.merchant.application.port.in.ConsultarConvite;
import com.deliveryplatform.merchant.domain.model.Membro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O convite visto de quem o recebeu (ADR-069 §2 a §5). <b>Recurso próprio, fora de
 * {@code /merchants/{id}/}</b>: quem tem o link ainda não é da loja, e o identificador que
 * endereça é o token — não o estabelecimento.
 *
 * <p>O {@code GET} é <b>público</b> (a cadeia libera só ele, e só um segmento); o
 * {@code POST .../aceite}, sob o mesmo prefixo, exige sessão e <b>nenhuma permissão</b> —
 * exigir permissão seria exigir que já fosse da loja.
 */
@RestController
@RequestMapping("/api/v1/convites/{token}")
public class ConviteController {

    private final ConsultarConvite consulta;
    private final AceitarConvite aceite;

    public ConviteController(ConsultarConvite consulta, AceitarConvite aceite) {
        this.consulta = consulta;
        this.aceite = aceite;
    }

    /**
     * O mínimo que convence: o nome da loja, o papel e até quando vale — e <b>nada que
     * identifique pessoa</b>, nem o próprio token. Quem convidou não aparece: o nome mora no
     * {@code identity}, e a rota que o traria não existe (ADR-069 §2, com gatilho).
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A loja, o papel e a validade do convite"),
            @ApiResponse(responseCode = "404", description = "Convite inexistente, expirado, já aceito ou cancelado — a mesma resposta", content = @Content)
    })
    @SecurityRequirements
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "O convite, para quem recebeu o link",
            description = "Público: quem recebe um convite ainda não tem sessão. Devolve só "
                    + "o nome da loja, o papel e a validade.")
    public ConvitePublicoResponse ver(@PathVariable String token) {
        ConsultarConvite.ConviteParaQuemRecebe convite = consulta.porToken(token);
        return new ConvitePublicoResponse(convite.nomeDaLoja(), convite.papel(), convite.expiraEm());
    }

    /**
     * Aceita: o vínculo nasce {@code COLABORADOR}, para o portador da sessão (B2). <b>O
     * telefone do convite não é conferido</b> — o token autoriza, o telefone endereça.
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O vínculo que nasceu"),
            @ApiResponse(responseCode = "401", description = "Sem sessão", content = @Content),
            @ApiResponse(responseCode = "404", description = "Convite inexistente, expirado, já aceito ou cancelado — a mesma resposta", content = @Content),
            @ApiResponse(responseCode = "409", description = "Quem convidou não pode mais conceder o que ofereceu (A2), ou quem aceita já é da equipe", content = @Content)
    })
    @PostMapping(path = "/aceite", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Aceita o convite",
            description = "Exige sessão e nenhuma permissão. A A2 é conferida de novo aqui: "
                    + "se quem convidou perdeu o que ofereceu, o aceite é recusado.")
    public AceiteResponse aceitar(@PathVariable String token,
                                  @AuthenticationPrincipal Jwt sessao) {
        Membro vinculo = aceite.aceitar(token, SujeitoDoToken.de(sessao));
        return new AceiteResponse(vinculo.getEstabelecimentoId(), vinculo.getPapel(),
                vinculo.getPermissoes());
    }
}
