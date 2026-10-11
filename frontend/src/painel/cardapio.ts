import type { components } from '../api/generated/catalog';
import type { Produto } from './produtos';

/**
 * O cardápio por seção (ADR-067 §1 e §2): a tela compõe duas rotas do `catalog` — as
 * seções e os produtos — e o agrupamento é daqui. Puro, sem React, para ser testado
 * sem tela.
 */

export type CategoriaDoContrato = components['schemas']['CategoriaResponse'];

/** Uma seção na forma da tela. "Seção" para quem lê; `Categoria` no domínio (ADR-035). */
export type SecaoDoCardapio = { id: string; nome: string; ordem: number };

export type GrupoDoCardapio = {
  /** `null` no grupo "Sem seção". */
  secao: SecaoDoCardapio | null;
  produtos: Produto[];
};

/** O tamanho da página pedida pelo cardápio: o teto da rota (`max-page-size`). */
export const TAMANHO_DO_CARDAPIO = 100;

export function caminhoDasSecoes(estabelecimentoId: string): string {
  return `/api/v1/merchants/${estabelecimentoId}/catalog/categorias`;
}

export function secaoParaTela(categoria: CategoriaDoContrato): SecaoDoCardapio {
  return {
    id: String(categoria.id ?? ''),
    nome: categoria.nome ?? '',
    ordem: categoria.ordem ?? 0,
  };
}

/** Pela `ordem`; o sort é estável, então o desempate do servidor sobrevive. */
function pelaOrdem<T extends { ordem: number }>(itens: readonly T[]): T[] {
  return [...itens].sort((a, b) => a.ordem - b.ordem);
}

/**
 * Seções pela `ordem`, produtos pela `ordem` dentro de cada uma — e **seção vazia
 * continua**: foi criada por alguém, e o que falta é item.
 *
 * <p>O produto cuja seção não está na lista vai para **"Sem seção", no fim** (ADR-067
 * §2), e o grupo só existe se houver algum: a ADR-066 decidiu que a leitura não valida
 * referência, e esconder o produto o tornaria inconsertável.
 */
export function agrupar(
  secoes: readonly SecaoDoCardapio[],
  produtos: readonly Produto[],
): GrupoDoCardapio[] {
  const conhecidas = new Set(secoes.map((secao) => secao.id));
  const grupos: GrupoDoCardapio[] = pelaOrdem(secoes).map((secao) => ({
    secao,
    produtos: pelaOrdem(produtos.filter((produto) => produto.categoriaId === secao.id)),
  }));
  const orfaos = produtos.filter(
    (produto) => produto.categoriaId === null || !conhecidas.has(produto.categoriaId),
  );
  if (orfaos.length > 0) {
    grupos.push({ secao: null, produtos: pelaOrdem(orfaos) });
  }
  return grupos;
}

/**
 * A `ordem` de um item novo: o maior da lista, mais um — e zero na lista vazia.
 *
 * <p><b>É dívida nossa, não recurso da tela</b> (ADR-067 §4): o contrato exige `ordem`,
 * e nenhum comerciante tem opinião sobre ela. A regra *"entra no fim"* é de domínio e
 * está vivendo no cliente. *Está no `dividas.md`, com gatilho.*
 */
export function proximaOrdem(itens: readonly { ordem: number }[]): number {
  return itens.reduce((maior, item) => Math.max(maior, item.ordem + 1), 0);
}
