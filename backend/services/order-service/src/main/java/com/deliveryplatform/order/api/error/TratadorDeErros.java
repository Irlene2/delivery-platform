package com.deliveryplatform.order.api.error;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.exception.ItemForaDoCardapio;
import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.port.out.AutorizacaoIndisponivel;
import com.deliveryplatform.order.application.port.out.CardapioIndisponivel;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.TransicaoInvalida;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Exceção carrega motivo e dados; a frase é escrita aqui (ADR-063). Toda frase de 400 e 409
 * diz o que fazer, porque é ela que aparece na tela — do cliente, no checkout, ou do operador,
 * no painel.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    private static final String RECUSA = "sem acesso a este estabelecimento";

    @ExceptionHandler(AcessoNegado.class)
    public ProblemDetail acessoNegado(AcessoNegado excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, RECUSA);
    }

    @ExceptionHandler(AutorizacaoIndisponivel.class)
    public ProblemDetail autorizacaoIndisponivel(AutorizacaoIndisponivel excecao) {
        log.warn("autorização indisponível: {}", excecao.getMessage(), excecao);
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, RECUSA);
    }

    @ExceptionHandler(PedidoNaoEncontrado.class)
    public ProblemDetail pedidoNaoEncontrado(PedidoNaoEncontrado excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
    }

    @ExceptionHandler(CardapioIndisponivel.class)
    public ProblemDetail cardapioIndisponivel(CardapioIndisponivel excecao) {
        log.warn("cardápio indisponível: {}", excecao.getMessage(), excecao);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "não foi possível confirmar os preços do cardápio agora; o pedido não foi feito. "
                        + "Tente de novo em instantes");
    }

    @ExceptionHandler(ItemForaDoCardapio.class)
    public ProblemDetail itemForaDoCardapio(ItemForaDoCardapio excecao) {
        String frase = switch (excecao.motivo()) {
            case NAO_PUBLICADO -> "um dos produtos do carrinho não está mais no cardápio; "
                    + "atualize a página e monte o pedido de novo";
            case ACABOU -> "«" + excecao.nome() + "» acabou; tire do carrinho para continuar";
        };
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, frase);
    }

    @ExceptionHandler(RegraDoPedidoViolada.class)
    public ProblemDetail regraViolada(RegraDoPedidoViolada excecao) {
        if (excecao.motivo() == RegraDoPedidoViolada.Motivo.INTEGRIDADE) {
            log.warn("invariante de construção quebrada: {}", excecao.getMessage(), excecao);
            return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                    "não foi possível completar a operação");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, fraseDe(excecao));
    }

    @ExceptionHandler(TransicaoInvalida.class)
    public ProblemDetail transicaoInvalida(TransicaoInvalida excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, fraseDe(excecao));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail pedidoMudou(OptimisticLockingFailureException excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "o pedido mudou enquanto você mexia; recarregue e tente de novo");
    }

    static String fraseDe(RegraDoPedidoViolada excecao) {
        List<String> d = excecao.dados();
        return switch (excecao.motivo()) {
            case TEXTO_OBRIGATORIO -> "informe o " + d.get(0);
            case TEXTO_LONGO_DEMAIS -> "o campo «" + d.get(0) + "» aceita no máximo " + d.get(1)
                    + " caracteres";
            case TELEFONE_INVALIDO -> "o telefone não parece válido; use DDD e número, como "
                    + "(21) 99999-0000";
            case SEM_ITENS -> "o carrinho está vazio; escolha ao menos um produto";
            case ITENS_DEMAIS -> "um pedido pode ter no máximo " + d.get(0) + " itens";
            case QUANTIDADE_INVALIDA -> "a quantidade de «" + d.get(0) + "» precisa ser de 1 a "
                    + d.get(1);
            case ENDERECO_OBRIGATORIO -> "para entrega, informe o endereço e o bairro";
            case ENDERECO_NA_RETIRADA -> "na retirada não vai endereço; escolha entrega para "
                    + "informar um";
            case MODALIDADE_NAO_ACEITA -> Modalidade.valueOf(d.get(0)) == Modalidade.ENTREGA
                    ? "esta loja não está fazendo entregas; escolha retirar no balcão"
                    : "esta loja não aceita retirada no balcão; escolha entrega";
            case TROCO_SEM_DINHEIRO -> "troco só existe no pagamento em dinheiro";
            case TROCO_MENOR_QUE_O_TOTAL -> "o troco para " + reais(d.get(0))
                    + " é menor que o total de " + reais(d.get(1));
            case TAXA_NEGATIVA -> "a taxa de entrega não pode ser negativa";
            case NENHUMA_MODALIDADE -> "a loja precisa aceitar entrega, retirada ou as duas";
            case MOTIVO_OBRIGATORIO -> "para cancelar, informe o motivo";
            case VALOR_RECEBIDO_OBRIGATORIO -> "no dinheiro, informe quanto foi recebido para "
                    + "calcular o troco";
            case VALOR_RECEBIDO_MENOR_QUE_O_TOTAL -> "o valor recebido (" + reais(d.get(0))
                    + ") é menor que o total do pedido (" + reais(d.get(1)) + ")";
            case PIX_NAO_CONFERIDO -> "confira o Pix no extrato da conta antes de registrar; "
                    + "comprovante na tela do cliente não vale";
            case ENTREGADOR_NA_RETIRADA -> "este pedido é de retirada e não tem entregador";
            case INTEGRIDADE -> "não foi possível completar a operação";
        };
    }

    static String fraseDe(TransicaoInvalida excecao) {
        return switch (excecao.motivo()) {
            case NAO_PERMITIDA -> "um pedido «" + rotulo(excecao.de()) + "» não pode ir para «"
                    + rotulo(excecao.para()) + "»";
            case FALTA_ENTREGADOR -> "informe o entregador antes de marcar que o pedido saiu";
            case FALTA_PAGAMENTO -> "registre o pagamento antes de concluir o pedido";
            case PAGAMENTO_FORA_DE_HORA -> excecao.de() != null && excecao.de().encerrado()
                    ? "este pedido já foi encerrado («" + rotulo(excecao.de()) + "»)"
                    : "o pagamento é registrado a partir de «Pronto»; este pedido está «"
                            + rotulo(excecao.de()) + "»";
            case PEDIDO_ENCERRADO -> "este pedido já foi encerrado («" + rotulo(excecao.de())
                    + "»)";
        };
    }

    private static String reais(String valor) {
        return "R$ " + valor.replace('.', ',');
    }

    static String rotulo(StatusDoPedido status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case RECEBIDO -> "Recebido";
            case CONFIRMADO -> "Confirmado";
            case EM_PREPARO -> "Em preparo";
            case PRONTO -> "Pronto";
            case SAIU_PARA_ENTREGA -> "Saiu para entrega";
            case AGUARDANDO_CLIENTE -> "Aguardando o cliente";
            case ENTREGUE -> "Entregue";
            case RETIRADO -> "Retirado";
            case CANCELADO -> "Cancelado";
        };
    }
}
