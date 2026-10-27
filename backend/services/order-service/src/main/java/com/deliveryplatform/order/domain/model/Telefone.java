package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada.Motivo;

import java.util.regex.Pattern;

/**
 * O telefone do cliente, em E.164 — a mesma normalização do {@code identity} (ADR-036),
 * copiada e não compartilhada: tipo de valor de domínio não entra no {@code :value-types}
 * (ADR-040). O DDD sem o +55 recebe o +55.
 */
public record Telefone(String numero) {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");
    private static final Pattern CARACTERES_DE_FORMATACAO = Pattern.compile("[\\s()\\-]");
    private static final Pattern NAO_DIGITO = Pattern.compile("\\D");

    public Telefone {
        if (numero == null || !E164.matcher(numero).matches()) {
            throw new RegraDoPedidoViolada(Motivo.TELEFONE_INVALIDO, "telefone inválido: " + numero);
        }
    }

    public static Telefone de(String bruto) {
        if (bruto == null) {
            throw new RegraDoPedidoViolada(Motivo.TELEFONE_INVALIDO, "telefone ausente");
        }
        String semFormatacao = CARACTERES_DE_FORMATACAO.matcher(bruto).replaceAll("");
        if (semFormatacao.startsWith("+")) {
            return new Telefone(semFormatacao);
        }
        String digitos = NAO_DIGITO.matcher(semFormatacao).replaceAll("");
        if (digitos.length() == 10 || digitos.length() == 11) {
            return new Telefone("+55" + digitos);
        }
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            return new Telefone("+" + digitos);
        }
        return new Telefone(semFormatacao);
    }

    /** Os dígitos sem o "+", que é o que o link {@code wa.me} espera. */
    public String digitos() {
        return numero.substring(1);
    }
}
