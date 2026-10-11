package com.deliveryplatform.catalog.domain.exception;

/**
 * 404 — a opção não existe neste produto, ou o grupo que a URL nomeia não é deste
 * produto (ADR-063 §4).
 *
 * <p>É o {@code ProdutoNaoEncontrado} um nível abaixo, e responde a mesma figura: o
 * identificador veio da URL, a loja e o produto já foram conferidos, e o que falta
 * é a opção. <b>Até a E-3b isto era {@code RegraDoCatalogoViolada} e saía 400</b> —
 * "você pediu errado" para um identificador que simplesmente não está aqui.
 *
 * <p>Mora em {@code domain/exception}, e não ao lado do {@code ProdutoNaoEncontrado}
 * em {@code application/exception}, porque quem a lança é o agregado: o
 * {@code Produto.marcarOpcao} é quem procura a opção dentro da raiz.
 *
 * <p><b>Id desconhecido continua estourando</b> — engolir faria a reativação varrer,
 * não achar e reportar sucesso. Só o tipo mudou.
 *
 * <p>A mensagem é <b>fixa</b>: não diz se faltou o grupo ou a opção.
 */
public class OpcaoNaoEncontrada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OpcaoNaoEncontrada() {
        super("opção não encontrada neste produto");
    }
}
