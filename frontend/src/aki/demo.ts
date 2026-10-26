import type { LojaDoUsuario, Permissao } from '../painel/tipos';
import { apagar, gravar, ler } from './guardado';

/**
 * Modo demonstração: ver as telas sem o back-end no ar, com os dados de exemplo.
 *
 * Nenhuma chamada à API acontece nesse modo. A "loja" e o papel são de mentira e
 * só decidem o que desenhar — no modo real, quem decide é o servidor.
 */
const CHAVE = 'aki.demo';

export type PerfilDeDemonstracao = 'administrador' | 'colaborador' | 'entregador' | 'cliente';

export const ID_DA_LOJA_DEMO = 'demo';

export function emDemonstracao(): boolean {
  return ler<PerfilDeDemonstracao | null>(CHAVE, null) !== null;
}

export function perfilDaDemonstracao(): PerfilDeDemonstracao | null {
  return ler<PerfilDeDemonstracao | null>(CHAVE, null);
}

export function entrarEmDemonstracao(perfil: PerfilDeDemonstracao): void {
  gravar(CHAVE, perfil);
}

export function sairDaDemonstracao(): void {
  apagar(CHAVE);
}

const TODAS: Permissao[] = [
  'VER_PRODUTO',
  'CRIAR_PRODUTO',
  'ALTERAR_PRODUTO',
  'DESATIVAR_PRODUTO',
  'VER_PEDIDO',
  'ALTERAR_STATUS',
  'VER_VENDAS',
  'VER_ENTREGA',
  'GERENCIAR_EQUIPE',
  'GERENCIAR_JORNADA',
  'CONFIGURAR_LOJA',
];

/** A loja de exemplo, com o papel escolhido na tela de entrada. */
export function lojaDeDemonstracao(): LojaDoUsuario {
  const colaborador = perfilDaDemonstracao() === 'colaborador';
  return {
    estabelecimentoId: ID_DA_LOJA_DEMO,
    nome: 'AKI Lanches',
    papel: colaborador ? 'COLABORADOR' : 'ADMINISTRADOR',
    permissoes: colaborador ? ['VER_PRODUTO', 'VER_PEDIDO'] : TODAS,
  };
}
