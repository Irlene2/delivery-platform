package com.deliveryplatform.merchant.api.dto;

import com.deliveryplatform.merchant.application.port.in.CriarEstabelecimento.NovoEstabelecimento;
import com.deliveryplatform.merchant.domain.model.MetodoPagamento;
import com.deliveryplatform.merchant.domain.model.Modalidade;
import com.deliveryplatform.merchant.domain.model.TipoDeOperacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * O corpo de {@code POST /api/v1/me/estabelecimentos} — quatro coisas e nada mais
 * (ADR-060 §2): o que o agregado exige para a loja nascer.
 *
 * <p><b>Bean Validation cobre a forma</b>: o que falta, o que vem em branco, e o
 * {@code null} dentro do mapa. <b>O mapa vazio não é forma</b>: é a M12, e vai ao
 * agregado — até a W-E um {@code @NotEmpty} a repetia aqui, e o mapa vazio nunca
 * chegava ao tratador que escreve a frase dela. <b>O que só o agregado sabe</b> —
 * documento com 11 ou 14 dígitos, telefone, fuso do conjunto brasileiro (M16),
 * nenhuma modalidade ou modalidade sem método (M12), fundo negativo — é
 * recusado na construção, e vira 400 por {@code CadastroDeLojaRecusado}. Repetir
 * essas regras aqui seria um segundo lugar onde elas estão escritas.
 */
public record CriarEstabelecimentoRequest(
        @NotNull @Valid IdentificacaoRequest identificacao,
        @NotNull @Valid PoliticaDeTrocoRequest politicaDeTroco,
        @NotNull TipoDeOperacao tipoDeOperacao,
        @NotNull Map<Modalidade, @NotNull Set<@NotNull MetodoPagamento>> metodosPorModalidade) {

    public record IdentificacaoRequest(
            @NotBlank String nome,
            @NotBlank String documento,
            @NotBlank String telefone,
            @NotBlank String enderecoTextual,
            @NotBlank String bairro,
            @Schema(
                    description = "Identificador IANA do conjunto brasileiro (M16, ADR-025). "
                            + "O conjunto é o de FusoHorario.identificadoresAceitos(), e o "
                            + "AllowableValuesDoFusoTest impede as duas listas de divergirem.",
                    defaultValue = "America/Sao_Paulo",
                    allowableValues = {
                            "America/Araguaina", "America/Bahia", "America/Belem",
                            "America/Boa_Vista", "America/Campo_Grande", "America/Cuiaba",
                            "America/Eirunepe", "America/Fortaleza", "America/Maceio",
                            "America/Manaus", "America/Noronha", "America/Porto_Velho",
                            "America/Recife", "America/Rio_Branco", "America/Santarem",
                            "America/Sao_Paulo"
                    })
            @NotBlank String fusoHorario) {
    }

    public record PoliticaDeTrocoRequest(
            @NotNull BigDecimal fundoMaximoDeTroco,
            boolean aceitaPedidoSemTrocoDisponivel) {
    }

    public NovoEstabelecimento paraComando() {
        return new NovoEstabelecimento(
                identificacao.nome(),
                identificacao.documento(),
                identificacao.telefone(),
                identificacao.enderecoTextual(),
                identificacao.bairro(),
                identificacao.fusoHorario(),
                politicaDeTroco.fundoMaximoDeTroco(),
                politicaDeTroco.aceitaPedidoSemTrocoDisponivel(),
                tipoDeOperacao,
                metodosPorModalidade);
    }
}
