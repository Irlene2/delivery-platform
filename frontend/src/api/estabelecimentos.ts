import { chamar } from './cliente';
import type { components } from './generated/merchant';

/**
 * O cadastro de loja — `POST /api/v1/me/estabelecimentos` (ADR-060).
 *
 * O corpo é o tipo gerado do contrato, inteiro: quatro grupos, e nada mais. A
 * resposta é o `LojaDoUsuario` da loja criada, a mesma forma que o `GET` da coleção
 * devolve — e o `estabelecimentoId` dela é o destino do painel (ADR-061).
 *
 * A recusa chega como `ErroDaApi` (com o `detail` em `detalhe`) ou `SemResposta`,
 * pelo `cliente.ts` de sempre: quem mostra é a tela, e ela mostra como veio (ADR-055).
 */
export type NovaLoja = components['schemas']['CriarEstabelecimentoRequest'];
export type LojaCriada = components['schemas']['LojaDoUsuario'];

export function criarEstabelecimento(corpo: NovaLoja, token: string): Promise<LojaCriada> {
  return chamar<LojaCriada>('/api/v1/me/estabelecimentos', {
    metodo: 'POST',
    corpo,
    token,
  });
}
