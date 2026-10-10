import { useOutletContext } from 'react-router';

import type { LojaDoUsuario } from './tipos';

/**
 * O que o `PainelPage` entrega às telas que moram embaixo dele: as lojas do
 * portador, lidas uma vez, e o jeito de relê-las — o cadastro precisa disso, porque
 * a loja que acabou de nascer ainda não está na lista que foi lida antes dela.
 *
 * Fora do componente, pelo mesmo motivo do `secoes.ts`: o fast refresh só recarrega
 * módulo que exporta apenas componentes.
 */
export type ContextoDoPainel = {
  lojas: LojaDoUsuario[];
  recarregarLojas: () => void;
};

export function useContextoDoPainel(): ContextoDoPainel {
  return useOutletContext<ContextoDoPainel>();
}
