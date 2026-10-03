import { ErroDaApi } from '../api/cliente';
import { useRecurso } from '../api/useRecurso';
import { caminhoDosProdutos, paraTela, type PaginaDoContrato } from './produtos';

/**
 * O cardápio publicado da loja escolhida — a rota que a G-B3 abriu, com tela
 * pela primeira vez.
 *
 * ## `vendavel` é a coluna que esta tela existe para mostrar
 *
 * A rota devolve os **publicados**, não os vendáveis, e cada item traz
 * `vendavel` já calculado — porque o cliente não consegue derivá-lo sem os
 * grupos de opções, que o resumo não carrega. Um produto ativo e não vendável é
 * cadastro perfeito num dia ruim: a pizza está `DISPONIVEL` e todos os tamanhos
 * acabaram.
 *
 * É exatamente o que o comerciante precisa ver para agir, e é por isso que a
 * lista não filtra: esconder o não vendável esconderia o problema.
 *
 * ## 403 não é erro genérico
 *
 * Quem não tem `VER_PRODUTO` nesta loja recebe 403, e isso não é falha — é a
 * autorização funcionando. A mensagem diz isso. O menu já não oferece o link a
 * essa pessoa; este caso existe para quem chegou pela URL, ou para quem perdeu
 * a permissão com a aba aberta.
 */
export function ListaDeProdutos({ estabelecimentoId }: { estabelecimentoId: string | null }) {
  const recurso = useRecurso<PaginaDoContrato>(
    estabelecimentoId === null ? null : caminhoDosProdutos(estabelecimentoId),
  );

  if (recurso.estado === 'carregando') {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando o cardápio…</p>;
  }

  if (recurso.estado === 'erro') {
    const semPermissao = recurso.erro instanceof ErroDaApi && recurso.erro.status === 403;
    return (
      <p role="alert" className="text-sm text-amber-700 dark:text-amber-400">
        {semPermissao
          ? 'Você não tem permissão para ver o cardápio desta loja.'
          : 'Não deu para carregar o cardápio agora.'}
      </p>
    );
  }

  const { produtos, total, temMais } = paraTela(recurso.dados);

  if (produtos.length === 0) {
    return (
      <p className="text-sm text-slate-600 dark:text-slate-300">
        Esta loja ainda não tem nenhum produto publicado.
      </p>
    );
  }

  return (
    <div>
      <ul className="divide-y divide-slate-200 dark:divide-slate-700">
        {produtos.map((produto) => (
          <li key={produto.id} className="flex items-baseline justify-between gap-4 py-3">
            <span className="text-slate-900 dark:text-slate-100">{produto.nome}</span>
            <span className="flex items-baseline gap-3">
              {!produto.vendavel && (
                <span className="rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">
                  não vendável
                </span>
              )}
              <span className="tabular-nums text-slate-700 dark:text-slate-300">
                {produto.preco}
              </span>
            </span>
          </li>
        ))}
      </ul>

      {temMais && (
        <p className="mt-3 text-xs text-slate-500 dark:text-slate-400">
          Mostrando {produtos.length} de {total}. A paginação da tela é da rodada seguinte.
        </p>
      )}
    </div>
  );
}
