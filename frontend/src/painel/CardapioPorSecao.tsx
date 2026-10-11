import { ErroDaApi } from '../api/cliente';
import { useRecurso } from '../api/useRecurso';
import {
  agrupar,
  caminhoDasSecoes,
  secaoParaTela,
  TAMANHO_DO_CARDAPIO,
  type CategoriaDoContrato,
  type GrupoDoCardapio,
} from './cardapio';
import {
  caminhoDosProdutos,
  paraTela,
  rotuloDaPublicacao,
  type PaginaDoContrato,
  type Produto,
} from './produtos';

/**
 * O cardápio da loja, **lido por seção** (ADR-067). A tela compõe duas rotas do
 * `catalog` — as seções e os produtos — e agrupa: é a primeira vista deste front montada
 * com duas requisições, e o back não ganha rota de "cardápio agrupado" (§1).
 *
 * ## O que é lido, e não recalculado
 *
 * O estado de publicação vem em cada produto desde a E-3d (ADR-068), e o `vendavel`
 * continua do servidor (W-C). A marca "não vendável" só aparece no que está **no
 * cardápio**: num rascunho, `vendavel: false` é o óbvio — ele nunca foi publicado —, e
 * a marca confundiria "nunca foi publicado" com "acabou tudo". É derivado do que o
 * servidor mandou, campo `estadoDePublicacao`, e não de regra nenhuma.
 *
 * ## A página
 *
 * O cardápio pede a página do tamanho do teto da rota. Uma loja com mais produtos do que
 * isso vê o aviso — a paginação da **tela** tem gatilho na ADR-067.
 */
export function CardapioPorSecao({
  estabelecimentoId,
  aoAbrir,
}: {
  estabelecimentoId: string;
  aoAbrir?: (produtoId: string) => void;
}) {
  const secoes = useRecurso<CategoriaDoContrato[]>(caminhoDasSecoes(estabelecimentoId));
  const produtos = useRecurso<PaginaDoContrato>(
    caminhoDosProdutos(estabelecimentoId, TAMANHO_DO_CARDAPIO),
  );

  if (secoes.estado === 'carregando' || produtos.estado === 'carregando') {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando o cardápio…</p>;
  }

  if (secoes.estado === 'erro' || produtos.estado === 'erro') {
    const erro = secoes.estado === 'erro' ? secoes.erro : (produtos as { erro: Error }).erro;
    const semPermissao = erro instanceof ErroDaApi && erro.status === 403;
    return (
      <p role="alert" className="text-sm text-amber-700 dark:text-amber-400">
        {semPermissao
          ? 'Você não tem permissão para ver o cardápio desta loja.'
          : 'Não deu para carregar o cardápio agora.'}
      </p>
    );
  }

  const pagina = paraTela(produtos.dados);
  const grupos = agrupar((secoes.dados ?? []).map(secaoParaTela), pagina.produtos);

  if (grupos.length === 0) {
    return (
      <p className="text-sm text-slate-600 dark:text-slate-300">
        Seu cardápio está vazio. Comece criando uma seção, como Bebidas ou Pizzas, e depois coloque
        os produtos nela.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      {grupos.map((grupo) => (
        <GrupoDaTela key={grupo.secao?.id ?? 'sem-secao'} grupo={grupo} aoAbrir={aoAbrir} />
      ))}
      {pagina.temMais && (
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Mostrando {pagina.produtos.length} de {pagina.total} produtos.
        </p>
      )}
    </div>
  );
}

function GrupoDaTela({
  grupo,
  aoAbrir,
}: {
  grupo: GrupoDoCardapio;
  aoAbrir: ((produtoId: string) => void) | undefined;
}) {
  const titulo = grupo.secao?.nome ?? 'Sem seção';
  return (
    <section aria-label={titulo}>
      <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {titulo}
      </h3>
      {grupo.produtos.length === 0 ? (
        <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
          Nenhum produto nesta seção.
        </p>
      ) : (
        <ul className="divide-y divide-slate-200 dark:divide-slate-700">
          {grupo.produtos.map((produto) => (
            <LinhaDoProduto key={produto.id} produto={produto} aoAbrir={aoAbrir} />
          ))}
        </ul>
      )}
    </section>
  );
}

function LinhaDoProduto({
  produto,
  aoAbrir,
}: {
  produto: Produto;
  aoAbrir: ((produtoId: string) => void) | undefined;
}) {
  const estado = rotuloDaPublicacao(produto.publicacao);
  return (
    <li className="py-3">
      <div className="flex items-baseline justify-between gap-4">
        {aoAbrir === undefined ? (
          <span className="text-slate-900 dark:text-slate-100">{produto.nome}</span>
        ) : (
          <button
            type="button"
            onClick={() => aoAbrir(produto.id)}
            className="text-left text-slate-900 underline-offset-2 hover:underline dark:text-slate-100"
          >
            {produto.nome}
          </button>
        )}
        <span className="flex items-baseline gap-3">
          {estado !== null && (
            <span className="text-xs text-slate-500 dark:text-slate-400">{estado}</span>
          )}
          {produto.publicacao === 'ATIVO' && !produto.vendavel && (
            <span className="rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-900/40 dark:text-amber-300">
              não vendável
            </span>
          )}
          <span className="tabular-nums text-slate-700 dark:text-slate-300">{produto.preco}</span>
        </span>
      </div>
    </li>
  );
}
