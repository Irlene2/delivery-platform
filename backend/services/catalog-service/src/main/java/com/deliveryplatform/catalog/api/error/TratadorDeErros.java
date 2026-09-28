package com.deliveryplatform.catalog.api.error;

import com.deliveryplatform.catalog.application.exception.AcessoNegado;
import com.deliveryplatform.catalog.application.port.out.AutorizacaoIndisponivel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Dois tratadores, e os dois respondem <b>a mesma coisa</b>.
 *
 * <p>Isso não é redundância: é onde a distinção entre "negado" e "não
 * respondeu" encontra a borda. Para o cliente as duas são 403 com o mesmo
 * corpo — falha fechada, e nenhuma pista de qual das duas foi. Para quem opera,
 * a segunda é um registro em nível de alerta, porque significa que o
 * {@code merchant} está fora e <b>o catálogo inteiro parou de autorizar</b>.
 *
 * <p>A ADR-011 já tinha assumido esse custo com todas as letras: <i>"se o
 * merchant-service cair por mais de um minuto, a plataforma inteira para de
 * autorizar. A resposta a isso é disponibilidade — réplicas, health check,
 * alerta — não relaxar a regra"</i>. O alerta é esta linha de registro.
 *
 * <p>O {@code catalog} tem uma exceção de domínio escrita —
 * {@code RegraDoCatalogoViolada} — e ela <b>não tem tratador aqui</b>, pelo
 * mesmo motivo que o {@code merchant} não escreveu os sete dele: nenhuma
 * requisição a alcança, porque a única rota que existe é uma leitura. Cada
 * tratador nasce com a rota que o alcança.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    private static final String RECUSA = "sem acesso a este estabelecimento";

    /**
     * 403 para as quatro recusas, com o mesmo corpo.
     *
     * <p><b>403 e não 404</b>, mesmo quando a loja não existe: a diferença
     * entre as duas respostas é um scanner de estabelecimentos escrito em
     * códigos de status (M7).
     */
    @ExceptionHandler(AcessoNegado.class)
    public ProblemDetail acessoNegado(AcessoNegado excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, RECUSA);
    }

    /**
     * O mesmo 403 — e um registro que diz que não foi o usuário, foi o sistema.
     *
     * <p>Se esta linha aparecer em rajada, nenhum comerciante está conseguindo
     * abrir o cardápio, e a causa não está no catálogo.
     */
    @ExceptionHandler(AutorizacaoIndisponivel.class)
    public ProblemDetail autorizacaoIndisponivel(AutorizacaoIndisponivel excecao) {
        log.warn("autorização indisponível: {}", excecao.getMessage(), excecao);
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, RECUSA);
    }
}
