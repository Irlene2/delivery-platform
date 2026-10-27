import { useMemo } from 'react';
import { useSessao } from '../auth/useSessao';
import { emDemonstracao } from '../aki/demo';
import {
  pedidosDaDemonstracaoComoFonte,
  pedidosDoServidor,
  type FonteDePedidos,
} from '../pedido/fontes';

/** Os pedidos da loja da URL: do servidor, com o token da sessão, ou da demonstração. */
export function useFonteDePedidos(estabelecimentoId: string): FonteDePedidos {
  const token = useSessao().sessao?.token ?? '';
  return useMemo(
    () =>
      emDemonstracao()
        ? pedidosDaDemonstracaoComoFonte()
        : pedidosDoServidor(estabelecimentoId, token),
    [estabelecimentoId, token],
  );
}

/** O link público do cardápio da loja — o que a loja manda ao cliente (ADR-077). */
export function linkDoCardapio(estabelecimentoId: string): string {
  return `${window.location.origin}/loja/${estabelecimentoId}`;
}

/** O link do acompanhamento, para a mensagem do WhatsApp. */
export function linkDoAcompanhamento(codigo: string): string {
  return `${window.location.origin}/pedido/${codigo}`;
}
