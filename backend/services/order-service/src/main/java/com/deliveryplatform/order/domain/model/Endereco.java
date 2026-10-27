package com.deliveryplatform.order.domain.model;

/**
 * Para onde vai a entrega. Texto livre nesta entrega: a taxa é fixa por loja, e o
 * bairro não decide nada ainda (a área por bairro é a ADR-020, fora do recorte).
 */
public record Endereco(String logradouro, String bairro, String complemento) {

    public Endereco {
        logradouro = Textos.obrigatorio(logradouro, "endereço", 200);
        bairro = Textos.obrigatorio(bairro, "bairro", 80);
        complemento = Textos.opcional(complemento, "complemento", 80);
    }
}
