import { useCallback, useEffect, useState } from 'react';
import type { PedidoDaLoja } from '../api/pedidos';
import { emDemonstracao } from '../aki/demo';
import type { FonteDePedidos } from '../pedido/fontes';
import { hojeLocal } from '../pedido/regras';
import { mensagemDoErro } from './catalogo';

/**
 * Os pedidos de um dia da loja, relidos a cada {@link INTERVALO} ms — o dashboard e o
 * fechamento mostram a mesma lista que a fila de Pedidos.
 */
const INTERVALO = 30_000;

export function usePedidosDoDia(fonte: FonteDePedidos, dia: string = hojeLocal()) {
  const [pedidos, definirPedidos] = useState<PedidoDaLoja[] | null>(null);
  const [falha, definirFalha] = useState<string | null>(null);

  const recarregar = useCallback(() => {
    fonte
      .listar(emDemonstracao() ? {} : { dia })
      .then((lidos) => {
        definirPedidos(lidos);
        definirFalha(null);
      })
      .catch((erro: unknown) => definirFalha(mensagemDoErro(erro)));
  }, [fonte, dia]);

  useEffect(() => {
    recarregar();
    const relogio = window.setInterval(recarregar, INTERVALO);
    return () => window.clearInterval(relogio);
  }, [recarregar]);

  return { pedidos, falha, recarregar };
}
