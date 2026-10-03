package com.deliveryplatform.merchant.application.port.out;

import com.deliveryplatform.merchant.domain.model.Equipe;
import com.deliveryplatform.merchant.domain.model.Membro;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída do vínculo. O adaptador mora em
 * {@code infrastructure/persistence}, como o do {@code Estabelecimento}.
 *
 * <p><b>Duas leituras, e elas existem por motivos opostos.</b>
 * {@link #buscarPorUsuarioELoja} é o caminho quente — é a consulta que toda
 * requisição de todo serviço vai fazer para resolver permissão (ADR-011), e
 * carrega um vínculo só. {@link #equipeParaAlteracao} é o caminho frio: carrega
 * a loja inteira e <b>toma um cadeado</b>, porque é o único jeito de A3 valer
 * sob concorrência.
 *
 * <p>Os nomes dizem para que servem justamente para que ninguém use o caro no
 * lugar do barato — e, pior, o barato no lugar do caro.
 */
public interface MembroRepositorio {

    Membro salvar(Membro membro);

    /**
     * O caminho quente da autorização contextual. Sem cadeado, sem a equipe.
     *
     * <p><b>Devolve o vínculo em qualquer estado</b> — ativo, suspenso e
     * removido. Quem resolve acesso a partir daqui e <b>não</b> passa por
     * {@code Membro.pode(...)} precisa filtrar {@code ativo()} na mão: o
     * {@code pode(...)} confere estado por dentro, e quem não o chama fica sem
     * rede. Ver a armadilha no {@code CLAUDE.md}.
     */
    Optional<Membro> buscarPorUsuarioELoja(UUID usuarioId, UUID estabelecimentoId);

    /**
     * Todos os vínculos de uma pessoa, <b>em qualquer estado</b>.
     *
     * <p>Devolve SUSPENSO e REMOVIDO junto com ATIVO, como
     * {@link #buscarPorUsuarioELoja} — e vale para ele o mesmo aviso: quem
     * resolve acesso a partir daqui filtra {@code ativo()} na mão. Nasceu na
     * G-B5, para a rota das lojas do portador.
     *
     * <p>Lista vazia quando não há vínculo nenhum — nunca {@code null}, nunca
     * exceção. Não ter vínculo é o estado de quem acabou de se cadastrar.
     */
    List<Membro> buscarPorUsuario(UUID usuarioId);

    /**
     * A equipe <b>para ler</b> — sem cadeado.
     *
     * <p>Nasceu na C-A, com a tela de equipe. Usar
     * {@link #equipeParaAlteracao} para exibir uma lista faria toda consulta de
     * leitura enfileirar-se atrás de qualquer alteração em curso na mesma loja:
     * um cadeado de escrita tomado para não escrever nada. Os nomes dizem para
     * que servem justamente para que essa troca não aconteça por descuido.
     */
    Equipe equipeDe(UUID estabelecimentoId);

    /**
     * Toma o cadeado na linha do estabelecimento e devolve a equipe inteira.
     *
     * <p><b>Precisa estar dentro de uma transação</b>, e a do chamador é a que
     * vale: um cadeado que é solto antes da escrita não protege nada. Duas
     * alterações de equipe da mesma loja passam a se enfileirar; de lojas
     * diferentes, não se veem.
     */
    Equipe equipeParaAlteracao(UUID estabelecimentoId);
}
