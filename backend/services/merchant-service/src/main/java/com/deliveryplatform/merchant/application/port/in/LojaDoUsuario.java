package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;

import java.util.List;
import java.util.UUID;

/**
 * Uma loja de que o portador do token faz parte — <b>com o que ele pode fazer
 * nela</b>.
 *
 * <h2>Por que papel e permissões vêm juntos, e não numa segunda chamada</h2>
 *
 * <p>O front precisa de duas coisas ao entrar: qual loja abrir, e que itens
 * mostrar no menu. As duas saem do mesmo vínculo, e o {@code Membro} já tem as
 * duas — o que faltava era o <b>nome</b> da loja, que mora no
 * {@code Estabelecimento}.
 *
 * <p>Separá-las em duas rotas criaria <b>duas verdades sobre o mesmo
 * vínculo</b>: uma listagem que diz "você está nesta loja" e um contexto que
 * diz "e aqui você pode isto", obrigadas a concordar para sempre. É a mesma
 * razão pela qual a G-B2 devolveu o {@code ContextoDeAcesso} direto, sem DTO
 * paralelo.
 *
 * <p>E isto <b>dispensa o gêmeo público do {@code /internal/}</b>: aquela rota
 * responde sobre <i>uma</i> loja e existe para serviço perguntar a serviço com
 * o token encaminhado (ADR-045). Esta responde sobre <i>todas</i> e existe para
 * o navegador. Duas superfícies, dois donos, nenhuma duplicada.
 *
 * <h2>O que ela não tem</h2>
 *
 * <p>Não tem o estado da operação — se a loja está dentro do horário. Isso é
 * {@code Disponibilidade} e dia operacional, e o cálculo correto deles é
 * justamente o que a G-C vai desenhar. Pôr aqui um valor calculado por engano
 * seria criar o segundo lugar que erra na madrugada.
 *
 * <p>Não tem {@code membroId} nem datas: é a projeção que a tela usa, não o
 * vínculo inteiro.
 */
public record LojaDoUsuario(
        UUID estabelecimentoId,
        String nome,
        Papel papel,
        List<Permissao> permissoes
) {

    public LojaDoUsuario {
        permissoes = List.copyOf(permissoes);
    }
}
