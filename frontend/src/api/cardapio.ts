import { chamar, ErroDaApi, SemResposta } from './cliente';
import type { components } from './generated/catalog';

/**
 * A escrita do cardápio (ADR-067): criar seção, criar produto, publicar. As três rotas
 * vêm da E-3b e da E-3c, e esta é a primeira tela que as chama.
 *
 * Os corpos são os tipos **gerados** do contrato, e a resposta também. A recusa chega
 * como `ErroDaApi` com o `detail` em `detalhe`, ou `SemResposta` — e quem a transforma
 * em frase é o {@link fraseDaRecusa}, num lugar só.
 */
export type NovaSecao = components['schemas']['CategoriaRequest'];
export type SecaoCriada = components['schemas']['CategoriaResponse'];
export type NovoProduto = components['schemas']['ProdutoRequest'];
export type ProdutoCriado = components['schemas']['ProdutoResponse'];

export function criarSecao(
  estabelecimentoId: string,
  corpo: NovaSecao,
  token: string,
): Promise<SecaoCriada> {
  return chamar<SecaoCriada>(`/api/v1/merchants/${estabelecimentoId}/catalog/categorias`, {
    metodo: 'POST',
    corpo,
    token,
  });
}

export function criarProduto(
  estabelecimentoId: string,
  corpo: NovoProduto,
  token: string,
): Promise<ProdutoCriado> {
  return chamar<ProdutoCriado>(`/api/v1/merchants/${estabelecimentoId}/catalog/produtos`, {
    metodo: 'POST',
    corpo,
    token,
  });
}

/** `PUT` em sub-recurso, sem corpo, idempotente: 200 também quando já estava publicado. */
export function publicarProduto(
  estabelecimentoId: string,
  produtoId: string,
  token: string,
): Promise<ProdutoCriado> {
  return chamar<ProdutoCriado>(
    `/api/v1/merchants/${estabelecimentoId}/catalog/produtos/${produtoId}/publicacao`,
    { metodo: 'PUT', token },
  );
}

/**
 * A frase da recusa. **O `detail` do servidor vai à tela como veio** (ADR-055, ADR-063):
 * a tela nunca compõe a frase de uma recusa de regra. Sem `detail`, a mensagem do cliente
 * por código; e o 403 nunca mostra o texto do servidor, que é genérico de propósito (M7).
 *
 * `null` para o 401: a sessão caiu, e a raiz da aplicação é quem troca a tela.
 */
export function fraseDaRecusa(
  erro: unknown,
  porCodigo: Record<number, string>,
  padrao: string,
): string | null {
  if (erro instanceof ErroDaApi) {
    if (erro.status === 401) return null;
    const detalhe = erro.detalhe?.trim();
    if (erro.status !== 403 && detalhe !== undefined && detalhe !== '') return detalhe;
    return porCodigo[erro.status] ?? padrao;
  }
  if (erro instanceof SemResposta) return 'Não foi possível falar com o servidor.';
  return padrao;
}
