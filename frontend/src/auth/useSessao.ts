import { useContext } from 'react';
import { ContextoDaSessao } from './contexto';
import type { EstadoDaSessao } from './contexto';

/**
 * Estoura quando usado fora do provider, e isso é de propósito: um contexto que
 * devolvesse `null` silenciosamente faria uma tela renderizar deslogada sem que
 * ninguém percebesse que ela estava fora da árvore.
 */
export function useSessao(): EstadoDaSessao {
  const estado = useContext(ContextoDaSessao);
  if (estado === null) {
    throw new Error('useSessao fora do SessaoProvider');
  }
  return estado;
}
