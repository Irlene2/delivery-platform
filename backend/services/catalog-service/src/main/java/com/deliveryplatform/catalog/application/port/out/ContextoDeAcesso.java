package com.deliveryplatform.catalog.application.port.out;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * O que o {@code merchant} respondeu sobre o portador do token nesta loja —
 * <b>no recorte que este serviço usa</b>.
 *
 * <h2>Três campos, e o quarto ficou de fora de propósito</h2>
 *
 * <p>O {@code estabelecimento.md} desenha o record canônico com quatro campos,
 * {@code papel} entre eles. Aqui ele não entra, e a razão está escrita no mesmo
 * documento: <i>"papel não é uma lista de permissões. Tem só dois valores e uma
 * função: ADMINISTRADOR é quem GERENCIAR_EQUIPE não pode tocar. Toda
 * autorização real é feita pela lista de permissões"</i>.
 *
 * <p>O {@code catalog} não tem nenhuma regra que consulte papel — ele não
 * administra equipe. Guardar o campo significaria carregar um enum de outro
 * serviço, com o mesmo problema de tolerância das permissões, para alimentar
 * zero decisões. <b>Se algum dia houver regra de catálogo que dependa do papel,
 * o campo entra junto com ela</b>, e não antes.
 *
 * <p>O {@code usuarioId} fica, mesmo sem regra que o consulte hoje: ele é o que
 * um registro de auditoria precisa para dizer quem viu o quê, e vem de graça na
 * mesma resposta.
 */
public record ContextoDeAcesso(
        UUID usuarioId,
        UUID estabelecimentoId,
        Set<PermissaoDoCatalogo> permissoes
) {

    public ContextoDeAcesso {
        permissoes = permissoes == null || permissoes.isEmpty()
                ? EnumSet.noneOf(PermissaoDoCatalogo.class)
                : EnumSet.copyOf(permissoes);
    }

    /**
     * A única pergunta que o caso de uso faz.
     *
     * <p>Não existe {@code podeTudo()}, nem atalho por papel: o conjunto é o que
     * o {@code merchant} gravou, e uma permissão que este serviço não conhece
     * já foi descartada no adaptador — descartada, nunca concedida.
     */
    public boolean pode(PermissaoDoCatalogo permissao) {
        return permissoes.contains(permissao);
    }

    /**
     * Cópia: quem lê não altera o contexto por fora. Seguro mesmo vazio, porque
     * o construtor garante que o campo é sempre um {@code EnumSet} — o
     * {@code copyOf} que recusa coleção vazia é a sobrecarga de
     * {@code Collection}, não a de {@code EnumSet}.
     */
    @Override
    public Set<PermissaoDoCatalogo> permissoes() {
        return EnumSet.copyOf(permissoes);
    }
}
