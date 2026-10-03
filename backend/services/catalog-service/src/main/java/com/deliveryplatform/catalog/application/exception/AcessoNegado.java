package com.deliveryplatform.catalog.application.exception;

/**
 * Uma recusa só, para quatro situações que o cliente não deve conseguir
 * distinguir — cópia deliberada da {@code AcessoNegado} do {@code merchant},
 * porque a ADR-001 proíbe importá-la:
 *
 * <ul>
 *   <li>a loja da URL não existe;</li>
 *   <li>existe, e quem pede não tem vínculo nela;</li>
 *   <li>tem vínculo, e ele está suspenso ou removido;</li>
 *   <li>tem vínculo ativo, e ele não tem {@code VER_PRODUTO}.</li>
 * </ul>
 *
 * <p>As três primeiras o {@code merchant} já funde num 403 só (M7); a quarta é
 * decisão deste serviço, e segue a mesma regra pelo mesmo motivo. Respostas
 * diferentes transformariam a rota num scanner de estabelecimentos.
 *
 * <p><b>Ela cobre também o que a {@link
 * com.deliveryplatform.catalog.application.port.out.AutorizacaoIndisponivel}
 * significa na borda</b>: as duas viram 403 para o cliente. Não é que sejam a
 * mesma coisa — é que a diferença entre elas interessa a quem opera, não a quem
 * pede. Ver o tratador de erros.
 */
public class AcessoNegado extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AcessoNegado() {
        super("sem acesso a este estabelecimento");
    }
}
