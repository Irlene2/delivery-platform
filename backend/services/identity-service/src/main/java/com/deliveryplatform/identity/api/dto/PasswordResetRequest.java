package com.deliveryplatform.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A senha nova, com o código que provou o telefone. As regras da senha são as do cadastro. */
public record PasswordResetRequest(
        @NotBlank String telefone,
        @NotBlank String codigo,
        @NotBlank @Size(min = 8, max = 72) String novaSenha) {
}
