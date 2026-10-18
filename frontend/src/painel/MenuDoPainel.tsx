import { secoesDe, type Secao } from './secoes';
import type { Permissao } from './tipos';

/**
 * O menu é **construído a partir das permissões do vínculo**, e não de um perfil.
 *
 * ## Por que isto é o coração da rodada, e não decoração
 *
 * O protótipo que chegou em 28/09 tinha um `<select>` chamado "Tipo de acesso"
 * na tela de login, com Administrador, Colaborador, Entregador e Cliente — e o
 * destino depois de entrar era a página correspondente à escolha. Isso é
 * **autorização declarada pelo cliente**, o inverso exato do que dez rodadas
 * construíram: o token tem seis claims e nenhuma permissão dentro, e a G-B3
 * existe para o `catalog` ir perguntar ao `merchant` quem você é *naquela loja*.
 *
 * Este componente é a resposta: as permissões vêm da resposta do servidor, por
 * loja, e o menu é o que elas permitem. **Trocar de loja troca o menu.**
 *
 * ## Só aparece o que tem tela
 *
 * Nem toda permissão tem tela. As que não têm estão na tabela de
 * `secoes.ts` com `secao: null` e não são renderizadas — um item de menu que não
 * leva a lugar nenhum é pior do que a ausência dele, e o dia em que a tela
 * existir é uma linha aqui.
 *
 * A tabela existe justamente para que a ausência fique **visível no código**,
 * em vez de virar uma lista de um item só que ninguém sabe de onde veio.
 *
 * ## A seção mora na URL (W-E, ADR-061)
 *
 * Com uma seção, roteamento seria cerimônia, e a W-B deixou a seção em estado com
 * gatilho escrito aqui: *"a segunda seção com tela. Aí a URL passa a precisar dizer
 * onde a pessoa está — para recarregar, para voltar e para mandar link a alguém —, e
 * isso é rota, não estado."* O gatilho venceu na W-C, a W-D o agravou com a terceira
 * tela, e a W-E o pagou: `atual` vem da URL, e `aoEscolher` navega.
 */
export function MenuDoPainel({
  permissoes,
  atual,
  aoEscolher,
}: {
  permissoes: Permissao[];
  atual: Secao | null;
  aoEscolher: (secao: Secao) => void;
}) {
  const visiveis = secoesDe(permissoes);

  if (visiveis.length === 0) {
    return (
      <p className="text-sm text-slate-500 dark:text-slate-400">
        Nesta loja você ainda não tem nenhuma permissão com tela.
      </p>
    );
  }

  return (
    <nav aria-label="Seções desta loja">
      <ul className="flex flex-wrap gap-2">
        {visiveis.map((item) => (
          <li key={item.secao}>
            <button
              type="button"
              aria-current={atual === item.secao ? 'page' : undefined}
              onClick={() => aoEscolher(item.secao)}
              className={[
                'rounded-md px-3 py-1.5 text-sm font-medium',
                atual === item.secao
                  ? 'bg-slate-900 text-white dark:bg-slate-100 dark:text-slate-900'
                  : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200',
              ].join(' ')}
            >
              {item.rotulo}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  );
}
