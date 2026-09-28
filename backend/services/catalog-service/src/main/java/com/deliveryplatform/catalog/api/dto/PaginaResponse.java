package com.deliveryplatform.catalog.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A página, no formato que este repositório escolheu — e não no do Spring Data.
 *
 * <p><b>Por que não devolver o {@code Page} direto.</b> O JSON do
 * {@code PageImpl} é uma serialização de <i>classe</i>, não um contrato: ele
 * carrega {@code pageable}, {@code sort}, {@code first}, {@code last},
 * {@code numberOfElements} e {@code empty}, muda entre versões do Spring Data,
 * e o próprio Spring avisa em log que serializá-lo diretamente é instável.
 * Congelar isso num contrato (ADR-039) seria congelar o formato interno de uma
 * biblioteca.
 *
 * <p>Quatro campos, e cada um responde uma pergunta que a tela faz: o que veio,
 * em que página estou, de que tamanho, e quantos existem ao todo. O total é o
 * que permite desenhar a paginação sem uma segunda consulta.
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long total
) {

    public static <D, R> PaginaResponse<R> de(Page<D> pagina, Function<D, R> conversao) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversao).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements());
    }
}
