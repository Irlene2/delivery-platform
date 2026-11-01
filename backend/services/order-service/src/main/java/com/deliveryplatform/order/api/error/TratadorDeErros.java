package com.deliveryplatform.order.api.error;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.exception.EntregadorForaDaEquipe;
import com.deliveryplatform.order.application.exception.RecusaAoEntregador;
import com.deliveryplatform.order.application.exception.ItemForaDoCardapio;
import com.deliveryplatform.order.application.exception.LojaFechada;
import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.port.out.AutorizacaoIndisponivel;
import com.deliveryplatform.order.application.port.out.CardapioIndisponivel;
import com.deliveryplatform.order.application.port.out.SituacaoIndisponivel;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.TransicaoInvalida;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Exceção carrega motivo e dados; a frase é escrita aqui (ADR-063). Toda frase de 400 e 409
 * diz o que fazer, porque é ela que aparece na tela — do cliente, no checkout, ou do operador,
 * no painel.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    private static final String RECUSA = "sem acesso a este estabelecimento";

    private static final Locale PORTUGUES = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final ZoneId fuso;
    private final Clock relogio;

    /** O fuso das frases é o mesmo do "hoje" da lista do painel ({@code delivery.pedidos.fuso}). */
    public TratadorDeErros(@Value("${delivery.pedidos.fuso:America/Sao_Paulo}") String fuso,
                           Clock relogio) {
        this.fuso = ZoneId.of(fuso);
        this.relogio = relogio;
    }

    /**
     * 409: a loja está fechada (ADR-077, emenda de 01/11/2026). Estado do mundo, não erro de
     * quem pede — a frase diz quando voltar.
     */
    @ExceptionHandler(LojaFechada.class)
    public ProblemDetail lojaFechada(LojaFechada excecao) {
        String frase = excecao.proximaAbertura()
                .map(quando -> "a loja está fechada agora e abre " + quando(quando)
                        + "; volte nesse horário para fazer o pedido")
                .orElse("a loja não está recebendo pedidos agora");
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, frase);
    }

    @ExceptionHandler(SituacaoIndisponivel.class)
    public ProblemDetail situacaoIndisponivel(SituacaoIndisponivel excecao) {
        log.warn("situação da loja indisponível: {}", excecao.getMessage(), excecao);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "não foi possível confirmar se a loja está aberta agora; o pedido não foi feito. "
                        + "Tente de novo em instantes");
    }

    /** "hoje às 18:00", "amanhã às 11:00", "na segunda-feira às 18:00". */
    String quando(Instant instante) {
        ZonedDateTime local = instante.atZone(fuso);
        LocalDate hoje = LocalDate.now(relogio.withZone(fuso));
        String hora = " às " + HORA.format(local);
        if (local.toLocalDate().equals(hoje)) {
            return "hoje" + hora;
        }
        if (local.toLocalDate().equals(hoje.plusDays(1))) {
            return "amanhã" + hora;
        }
        DayOfWeek dia = local.getDayOfWeek();
        String artigo = dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY ? "no " : "na ";
        return artigo + dia.getDisplayName(TextStyle.FULL, PORTUGUES) + hora;
    }

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

    @ExceptionHandler(EntregadorForaDaEquipe.class)
    public ProblemDetail entregadorForaDaEquipe(EntregadorForaDaEquipe excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "este entregador não está na equipe da loja; peça para ele abrir a área de "
                        + "entregas uma vez e informar o nome");
    }

    @ExceptionHandler(RecusaAoEntregador.class)
    public ProblemDetail recusaAoEntregador(RecusaAoEntregador excecao) {
        String frase = switch (excecao.motivo()) {
            case STATUS -> "pelo app de entregas você marca só a saída e a entrega; o resto é "
                    + "com a loja";
            case PIX -> "o Pix cai na conta da loja e é conferido por ela; registre só dinheiro "
                    + "ou cartão";
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
