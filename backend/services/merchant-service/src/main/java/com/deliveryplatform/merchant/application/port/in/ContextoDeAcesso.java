package com.deliveryplatform.merchant.application.port.in;

import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;

import java.util.Set;
import java.util.UUID;

/**
 * A resposta da autorização entre serviços — desenhada pelo consumidor, e por
 * isso ela é exatamente o record do {@code estabelecimento.md} §3:
 *
 * <pre>
 * record ContextoDeAcesso(UUID usuarioId, UUID estabelecimentoId,
 *                         Papel papel, Set&lt;Permissao&gt; permissoes) {}
 * </pre>
 *
 * <p><b>Por que mora em {@code application/port/in} e não em {@code domain}.</b>
 * O conceito de domínio do {@code merchant} é o {@link
 * com.deliveryplatform.merchant.domain.model.Membro} — vínculo com id, estado,
 * datas e regras de escalada. Isto é uma <i>projeção</i> dele, no formato que
 * outro serviço pediu. Pôr no domínio faria parecer que o {@code merchant} tem
 * dois conceitos para a mesma coisa.
 *
 * <p><b>E por que não existe um DTO de API separado.</b> Um DTO existe para
 * desacoplar o formato do fio do formato do domínio. Aqui o tipo de retorno da
 * porta <b>já é</b> o formato do fio, porque foi o documento do consumidor que
 * o definiu. Um segundo record idêntico ao lado seria uma cópia — e cópia de
 * contrato é a metade que envelhece.
 *
 * <h2>O que ele deliberadamente não carrega</h2>
 *
 * <p>Não tem {@code membroId}, {@code criadoEm} nem {@code estado}. Os dois
 * primeiros porque o consumidor não precisa deles, e todo campo a mais vira
 * contrato congelado (ADR-039). O {@code estado} porque <b>só vínculo
 * {@code ATIVO} recebe resposta</b>: o campo seria a constante
 * {@code "ATIVO"} em toda resposta que existe, e um campo que não pode variar é
 * documentação disfarçada de dado.
 *
 * <h2>As permissões são as gravadas, e não as deduzidas do papel</h2>
 *
 * <p>O {@code estabelecimento.md} §2 é explícito: <i>"papel não é uma lista de
 * permissões. Tem só dois valores e uma função: ADMINISTRADOR é quem
 * GERENCIAR_EQUIPE não pode tocar. Toda autorização real é feita pela lista de
 * permissões"</i>. Um colaborador promovido a administrador continua com as
 * permissões que tinha — o {@code promover} muda só o papel. Quem construísse
 * a resposta com "administrador tem tudo" daria, a cada promoção, dez
 * permissões que ninguém concedeu.
 */
public record ContextoDeAcesso(
        UUID usuarioId,
        UUID estabelecimentoId,
        Papel papel,
        Set<Permissao> permissoes
) {
}
