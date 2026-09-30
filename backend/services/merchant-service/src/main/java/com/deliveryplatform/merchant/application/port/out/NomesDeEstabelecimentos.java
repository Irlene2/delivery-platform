package com.deliveryplatform.merchant.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Os nomes de um punhado de lojas — <b>uma projeção, não o agregado</b>.
 *
 * <h2>Por que uma porta nova, e não um método no `EstabelecimentoRepositorio`</h2>
 *
 * <p>O que este caso de uso precisa do {@code Estabelecimento} é <b>um campo</b>:
 * o nome. Carregar o agregado inteiro — identificação, documento, fuso,
 * disponibilidade, áreas — para ler uma {@code String} é o mesmo desperdício que
 * o {@code equipeParaAlteracao} evita ao não ser usado para exibir lista.
 *
 * <p>E há um motivo mais forte: <b>agregado se carrega inteiro e se grava
 * inteiro</b>. Uma porta de leitura que devolva agregado convida quem a chama a
 * alterá-lo e salvá-lo, e aí a raiz foi carregada por um caminho que não é o
 * dela. Uma projeção não tem esse risco porque não tem comportamento.
 *
 * <p>É {@code interface} de um método de propósito: o dublê de teste é uma
 * lambda, e um dublê pequeno é um dublê que não esconde nada.
 *
 * <h2>O que ela promete</h2>
 *
 * <p>Devolve <b>só o que achou</b>. Identificador que não existe simplesmente
 * não volta — e quem chama decide o que fazer com a diferença, que neste
 * sistema é descartar a linha.
 */
@FunctionalInterface
public interface NomesDeEstabelecimentos {

    List<NomeDaLoja> de(Collection<UUID> estabelecimentoIds);

    /** O par que a tela precisa: qual loja, e como ela se chama. */
    record NomeDaLoja(UUID estabelecimentoId, String nome) {
    }
}
