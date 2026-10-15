package com.deliveryplatform.merchant.api.dto;

import com.deliveryplatform.merchant.domain.model.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * O pedido de convite (ADR-069). <b>Só forma aqui</b>: presente e não branco. Se o
 * telefone é telefone, e se quem convida tem o que oferece (A2), quem diz é o agregado —
 * e o 400 dele sai com motivo, pelo {@code ConviteRecusado}.
 *
 * <p>Sem papel: convite nunca concede {@code ADMINISTRADOR}, e quem aceita entra
 * {@code COLABORADOR} com estas permissões (B2).
 */
@Schema(description = "O convite: o telefone de quem vai entrar e as permissões oferecidas.")
public record ConviteRequest(

        @Schema(description = "O telefone da pessoa convidada — o endereço, não a credencial.",
                example = "(81) 98765-4321")
        @NotBlank(message = "telefone é obrigatório")
        String telefone,

        @Schema(description = "As permissões oferecidas. Só as que quem convida tem (A2).")
        @NotNull(message = "permissoes é obrigatório")
        Set<@NotNull Permissao> permissoes
) {
}
