package com.deliveryplatform.order.domain.exception;

import java.util.List;

/**
 * Uma regra do pedido recusada — o pedido malformado, que é culpa de quem pediu (400).
 *
 * <p>O padrão é o do {@code catalog} (ADR-063): a exceção carrega o <b>motivo</b> e os
 * <b>dados</b>, e quem escreve a frase para a pessoa é o tratador de erros da borda.
 * A mensagem daqui é para o log, não para a tela.
 */
public class RegraDoPedidoViolada extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        TEXTO_OBRIGATORIO,
        TEXTO_LONGO_DEMAIS,
        TELEFONE_INVALIDO,
        SEM_ITENS,
        ITENS_DEMAIS,
        QUANTIDADE_INVALIDA,
        ENDERECO_OBRIGATORIO,
        ENDERECO_NA_RETIRADA,
        MODALIDADE_NAO_ACEITA,
        TROCO_SEM_DINHEIRO,
        TROCO_MENOR_QUE_O_TOTAL,
        TAXA_NEGATIVA,
        NENHUMA_MODALIDADE,
        MOTIVO_OBRIGATORIO,
        VALOR_RECEBIDO_OBRIGATORIO,
        VALOR_RECEBIDO_MENOR_QUE_O_TOTAL,
        PIX_NAO_CONFERIDO,
        ENTREGADOR_NA_RETIRADA,
        INTEGRIDADE
    }

    private final Motivo motivo;
    private final List<String> dados;

    public RegraDoPedidoViolada(Motivo motivo, String mensagem, String... dados) {
        super(mensagem);
        this.motivo = motivo;
        this.dados = List.of(dados);
    }

    /** Uma invariante de construção quebrada: defeito de quem chamou, não de quem pediu (500). */
    public static RegraDoPedidoViolada integridade(String mensagem) {
        return new RegraDoPedidoViolada(Motivo.INTEGRIDADE, mensagem);
    }

    public Motivo motivo() {
        return motivo;
    }

    public List<String> dados() {
        return dados;
    }
}
