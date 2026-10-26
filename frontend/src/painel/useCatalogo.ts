import { useCallback, useEffect, useState } from 'react';
import { mensagemDoErro, type Catalogo, type FonteDoCatalogo } from './catalogo';

export type EstadoDoCatalogo =
  | { estado: 'carregando' }
  | { estado: 'pronto'; catalogo: Catalogo }
  | { estado: 'erro'; frase: string };

/** Carrega o catálogo da fonte (servidor ou demonstração) e deixa a tela trocá-lo. */
export function useCatalogo(fonte: FonteDoCatalogo) {
  const [leitura, definirLeitura] = useState<EstadoDoCatalogo>({ estado: 'carregando' });

  useEffect(() => {
    let atual = true;
    definirLeitura({ estado: 'carregando' });
    fonte
      .carregar()
      .then((catalogo) => {
        if (atual) definirLeitura({ estado: 'pronto', catalogo });
      })
      .catch((erro: unknown) => {
        if (atual) definirLeitura({ estado: 'erro', frase: mensagemDoErro(erro) });
      });
    return () => {
      atual = false;
    };
  }, [fonte]);

  const trocar = useCallback(
    (catalogo: Catalogo) => definirLeitura({ estado: 'pronto', catalogo }),
    [],
  );
  return { leitura, trocar };
}
