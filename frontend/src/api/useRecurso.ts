import { useEffect, useState } from 'react';

import { useSessao } from '../auth/useSessao';
import { chamar, ErroDaApi } from './cliente';

/**
 * Um recurso lido da API, em um dos três estados possíveis.
 *
 * É união discriminada, e não `{ dados, carregando, erro }` com três campos
 * soltos: com três campos existem oito combinações e cinco delas não fazem
 * sentido — "carregando com erro e dados", por exemplo. Assim o TypeScript
 * obriga a tela a tratar os três, e não deixa ler `dados` antes de existir.
 */
export type Recurso<T> =
  | { estado: 'carregando' }
  | { estado: 'pronto'; dados: T }
  | { estado: 'erro'; erro: ErroDaApi | Error };

/**
 * Lê um caminho da API e devolve o estado dele.
 *
 * ## O token vem da sessão
 *
 * As duas rotas do painel exigem token, e o `chamar` só manda `Authorization`
 * quando recebe um. Sem ele a resposta é 401 — e o cliente trata 401
 * derrubando a sessão, então o painel deslogaria a pessoa no instante em que
 * abrisse. Sem sessão, não há chamada.
 *
 * `caminho` nulo significa **ainda não há o que buscar** — é o caso da lista de
 * produtos antes de alguém escolher a loja. Fica em `carregando` sem fazer
 * chamada nenhuma, que é o estado honesto: não há dado, e também não há erro.
 *
 * ## A corrida, que é a razão de este arquivo ter teste próprio
 *
 * Trocar de loja duas vezes seguidas dispara duas buscas. Se a primeira
 * responder **depois** da segunda — e é o caso comum, porque a primeira é a que
 * pegou o servidor frio —, a tela mostraria os produtos da loja anterior com o
 * nome da loja nova. O dado seria verdadeiro e estaria no lugar errado, que é o
 * tipo de defeito que ninguém reproduz.
 *
 * A guarda é a variável `atual`, fechada no `useEffect`: a limpeza do efeito a
 * desliga antes de o próximo começar, e a resposta que chegar depois é
 * descartada. `useRecurso.test.ts` tem um caso que fica vermelho sem ela.
 *
 * ## Por que não TanStack Query
 *
 * A W-A a declarou e nunca a usou, e a W-A deixou o gatilho escrito para esta
 * rodada decidir. Duas leituras sem escrita não pagam o provedor, a
 * configuração e o vocabulário — e peça declarada e nunca exercitada é o que
 * este repositório já encontrou quatro vezes, com outro nome. O gatilho para
 * trazê-la de volta está no `frontend/README.md`: a primeira
 * escrita que precise invalidar leitura de outra tela.
 */
export function useRecurso<T>(caminho: string | null): Recurso<T> {
  const [recurso, definir] = useState<Recurso<T>>({ estado: 'carregando' });
  // O token da sessão, e não um argumento: toda leitura do painel é de quem
  // entrou, e esquecer de passá-lo não é erro de compilação — compila e volta
  // 401, que o cliente trata derrubando a sessão.
  const token = useSessao().sessao?.token;

  useEffect(() => {
    if (caminho === null || token === undefined) {
      return;
    }

    let atual = true;
    definir({ estado: 'carregando' });

    chamar<T>(caminho, { token })
      .then((dados) => {
        if (atual) {
          definir({ estado: 'pronto', dados });
        }
      })
      .catch((erro: unknown) => {
        if (atual) {
          definir({ estado: 'erro', erro: erro instanceof Error ? erro : new Error(String(erro)) });
        }
      });

    return () => {
      atual = false;
    };
  }, [caminho, token]);

  return recurso;
}
