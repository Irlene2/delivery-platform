package com.deliveryplatform.catalog.application.port.out;

/**
 * O {@code merchant} não respondeu — tempo esgotado, 5xx, conexão recusada.
 *
 * <p><b>Não é a mesma coisa que "não há vínculo"</b>, e a diferença não é
 * estética. "Não há vínculo" é uma resposta: o produtor consultou a própria
 * tabela e disse não. Isto é ausência de resposta, e a única coisa que se sabe
 * é que não se sabe.
 *
 * <p>As duas negam, e negam igual: o cliente recebe 403 nos dois casos, porque
 * a ADR-011 não admite janela de graça — <i>"Fail-closed que abre sob pressão
 * não é fail-closed"</i>.
 *
 * <p>O que a separação compra é o futuro: quando o cache entrar, na G-B4, o
 * negativo poderá ficar guardado por 10 s e <b>isto aqui nunca</b>. Cachear
 * indisponibilidade como se fosse negação transformaria uma queda de dois
 * segundos em dez segundos de recusa para todos os usuários da loja.
 *
 * <p>Fica separada da {@code AcessoNegado} pelo mesmo motivo que a
 * {@code DocumentoIlegivel} do mapeador ficou separada da
 * {@code RegraDoCatalogoViolada}: uma diz que o pedido é inválido, a outra diz
 * que <b>o sistema</b> está com um problema. Quem opera precisa da diferença no
 * registro, mesmo quando o cliente não precisa dela na resposta.
 */
public class AutorizacaoIndisponivel extends RuntimeException {

    public AutorizacaoIndisponivel(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
