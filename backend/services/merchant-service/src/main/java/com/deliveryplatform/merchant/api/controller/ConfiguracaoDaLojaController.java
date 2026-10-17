package com.deliveryplatform.merchant.api.controller;

import com.deliveryplatform.merchant.api.dto.FaixaDeHorarioRequest;
import com.deliveryplatform.merchant.api.dto.HorarioRequest;
import com.deliveryplatform.merchant.api.dto.HorarioResponse;
import com.deliveryplatform.merchant.api.seguranca.SujeitoDoToken;
import com.deliveryplatform.merchant.application.port.in.DefinirHorario;
import com.deliveryplatform.merchant.application.port.in.DefinirHorario.FaixaPedida;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A configuração da loja, sob {@code /settings} — o prefixo que o gateway já roteia para
 * o {@code merchant} ({@code merchant-settings}, ADR-012). Hoje uma rota só: o horário
 * (ADR-073). As outras partes da H1.2 entram aqui, sob a mesma {@code CONFIGURAR_LOJA}.
 *
 * <p>O {@code estabelecimentoId} da URL <b>autoriza</b>: o caso de uso o confronta com o
 * vínculo do {@code sub} antes de buscar a loja (invariante 9).
 */
@RestController
@RequestMapping("/api/v1/merchants/{estabelecimentoId}/settings")
public class ConfiguracaoDaLojaController {

    private final DefinirHorario horarios;

    public ConfiguracaoDaLojaController(DefinirHorario horarios) {
        this.horarios = horarios;
    }

    /**
     * Substitui o horário semanal inteiro. <b>Não publica evento</b>: a abertura do
     * expediente é da varredura (ADR-073 §3), que nota a loja na passada seguinte.
     */
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "O horário como ficou gravado"),
            @ApiResponse(responseCode = "400", description = "Corpo malformado, dia que não é dia da semana, identificador da loja malformado, ou faixa que o domínio recusa — início igual ao fim, ou a mesma faixa duas vezes no mesmo dia", content = @Content),
            @ApiResponse(responseCode = "401", description = "Sem token, ou token inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem vínculo, sem CONFIGURAR_LOJA ou loja inexistente — a mesma recusa", content = @Content)
    })
    @PutMapping(path = "/horario", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Define o horário semanal da loja",
            description = "Exige CONFIGURAR_LOJA. Substitui o horário inteiro; mapa vazio "
                    + "volta a loja a \"nunca abre por horário\".")
    public HorarioResponse definirHorario(
            @PathVariable UUID estabelecimentoId,
            @AuthenticationPrincipal Jwt token,
            @Valid @RequestBody HorarioRequest pedido) {

        Map<DayOfWeek, List<FaixaPedida>> horario = new EnumMap<>(DayOfWeek.class);
        pedido.horarioDeFuncionamento().forEach((dia, faixas) -> horario.put(dia,
                faixas.stream().map(ConfiguracaoDaLojaController::paraComando).toList()));

        return HorarioResponse.de(
                horarios.definir(estabelecimentoId, SujeitoDoToken.de(token), horario));
    }

    private static FaixaPedida paraComando(FaixaDeHorarioRequest faixa) {
        return new FaixaPedida(faixa.inicio(), faixa.fim());
    }
}
