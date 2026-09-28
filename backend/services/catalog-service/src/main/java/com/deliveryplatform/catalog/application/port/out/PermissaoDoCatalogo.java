package com.deliveryplatform.catalog.application.port.out;

import java.util.Optional;

/**
 * As permissões que <b>este</b> serviço sabe nomear — quatro, e nenhuma a mais.
 *
 * <h2>Por que não é a `Permissao` do merchant</h2>
 *
 * <p>A ADR-001 proíbe importar de outro serviço, e a ADR-040 fecha a porta do
 * {@code :value-types} com todas as letras: <i>"O que entra hoje:
 * {@code Money}. Só."</i> — e o argumento que ela usa para recusar o
 * {@code Telefone} vale aqui inteiro: mesma palavra, recortes diferentes.
 *
 * <p>O javadoc da {@code Permissao} do {@code merchant} escreveu o gatilho:
 * <i>"O gatilho para mudar é o segundo serviço que precise nomear uma
 * permissão"</i>. O {@code catalog} é esse segundo serviço, e a resposta é esta:
 * <b>cada serviço nomeia o recorte que usa; o contrato entre eles é o texto.</b>
 *
 * <h2>Quatro valores, e o que fica de fora não é esquecimento</h2>
 *
 * <p>O {@code merchant} tem dez permissões. O {@code catalog} nomeia as quatro
 * de produto porque são as únicas sobre as quais ele tem regra. Nomear
 * {@code GERENCIAR_JORNADA} aqui seria declarar um interesse que este serviço
 * não tem — e enum com valor que nenhuma regra consulta é a mesma promessa com
 * sintaxe de código que este repositório já recusou quatro vezes.
 *
 * <h2>A leitura perdoa, e é obrigatório que perdoe</h2>
 *
 * <p>O contrato do {@code merchant} congelou {@code permissoes} como enum
 * <b>fechado</b>. No dia em que ele ganhar uma permissão nova, a resposta vai
 * trazer um texto que este enum não conhece — e a ADR-027 diz que acrescentar
 * valor a enum é <b>mudança compatível</b>, o que só é verdade se o consumidor
 * tolerar o valor novo.
 *
 * <p>Por isso {@link #de(String)} devolve {@link Optional} em vez de estourar:
 * o adaptador descarta o desconhecido e registra. Um {@code Enum.valueOf} aqui
 * transformaria a mudança compatível do {@code merchant} num incidente do
 * {@code catalog} — e no pior formato possível, porque a falha seria "ninguém
 * consegue ver o cardápio" no dia de um deploy que não mexeu no catálogo.
 *
 * <p>E o inverso também não pode acontecer: <b>descartar não é conceder</b>. Uma
 * permissão desconhecida some do conjunto; ela nunca vira "deixa passar".
 */
public enum PermissaoDoCatalogo {

    VER_PRODUTO,
    CRIAR_PRODUTO,
    ALTERAR_PRODUTO,
    DESATIVAR_PRODUTO;

    /**
     * O texto que veio do {@code merchant}, quando este serviço sabe o que ele
     * significa.
     *
     * <p>Vazio para qualquer outra coisa — inclusive {@code null}, texto em
     * branco e permissão nova. Quem chama descarta e registra.
     */
    public static Optional<PermissaoDoCatalogo> de(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        for (PermissaoDoCatalogo permissao : values()) {
            if (permissao.name().equals(nome)) {
                return Optional.of(permissao);
            }
        }
        return Optional.empty();
    }
}
