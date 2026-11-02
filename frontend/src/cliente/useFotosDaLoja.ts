import { useEffect, useState } from 'react';
import { vitrineDe } from '../pedido/fontes';

/**
 * As fotos dos produtos da loja, pelo cardápio público: `produtoId → endereço da foto`.
 *
 * O item do pedido guarda nome, preço e quantidade, mas não a foto (02/11/2026). Em vez de copiar
 * a foto para dentro do pedido, o acompanhamento lê a do cardápio — a mesma que o cliente viu ao
 * escolher. Produto que saiu do cardápio fica sem foto, e a tela mostra a imagem padrão.
 */
export function useFotosDaLoja(estabelecimentoId: string): ReadonlyMap<string, string> {
  const [fotos, definirFotos] = useState<ReadonlyMap<string, string>>(new Map());

  useEffect(() => {
    if (estabelecimentoId === '') return;
    let atual = true;
    vitrineDe(estabelecimentoId)
      .cardapio.carregar()
      .then((catalogo) => {
        if (!atual) return;
        definirFotos(
          new Map(
            catalogo.itens
              .filter((item) => item.imagem !== '')
              .map((item): [string, string] => [item.id, item.imagem]),
          ),
        );
      })
      // Sem o cardápio, as fotos ficam na imagem padrão; o acompanhamento segue.
      .catch(() => undefined);
    return () => {
      atual = false;
    };
  }, [estabelecimentoId]);

  return fotos;
}
