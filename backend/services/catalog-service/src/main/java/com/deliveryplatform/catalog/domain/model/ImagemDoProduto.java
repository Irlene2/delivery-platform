package com.deliveryplatform.catalog.domain.model;

import com.deliveryplatform.catalog.domain.exception.RegraDoCatalogoViolada;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A foto de um produto (ADR-077, emenda de 01/11/2026). <b>Fora do documento do produto</b>:
 * o {@code catalogo.md} §9 manda que binário nunca entre nele — a listagem e o cardápio leem o
 * produto inteiro, e uma foto de 300 KB em cada um multiplicaria o corpo de toda leitura. O
 * produto guarda só a referência ({@code imagemRef}), como o documento previa para o MinIO.
 *
 * <p>A tela reduz a foto antes de enviar (lado maior de 800 px); o limite aqui é o que a rota
 * aceita mesmo de quem não passou pela tela.
 */
public record ImagemDoProduto(UUID produtoId, UUID estabelecimentoId, String tipo, byte[] conteudo) {

    public static final int TAMANHO_MAXIMO = 600 * 1024;

    public static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

    public ImagemDoProduto {
        Objects.requireNonNull(produtoId, "produtoId");
        Objects.requireNonNull(estabelecimentoId, "estabelecimentoId");
        if (tipo == null || !TIPOS.contains(tipo)
                || conteudo == null || conteudo.length == 0 || conteudo.length > TAMANHO_MAXIMO) {
            throw new RegraDoCatalogoViolada(RegraDoCatalogoViolada.Motivo.IMAGEM_INVALIDA,
                    "imagem recusada: tipo " + tipo + ", "
                            + (conteudo == null ? 0 : conteudo.length) + " bytes");
        }
        conteudo = conteudo.clone();
    }

    @Override
    public byte[] conteudo() {
        return conteudo.clone();
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof ImagemDoProduto i && produtoId.equals(i.produtoId)
                && estabelecimentoId.equals(i.estabelecimentoId) && tipo.equals(i.tipo)
                && Arrays.equals(conteudo, i.conteudo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produtoId, estabelecimentoId, tipo) * 31 + Arrays.hashCode(conteudo);
    }

    @Override
    public String toString() {
        return "ImagemDoProduto[" + produtoId + ", " + tipo + ", " + conteudo.length + " bytes]";
    }
}
