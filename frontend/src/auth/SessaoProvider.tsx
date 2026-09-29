import { useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { esquecer, guardar, recuperar } from './armazenamento';
import type { Sessao } from './armazenamento';
import { quandoASessaoCair } from '../api/cliente';
import { entrar as chamarLogin } from '../api/autenticacao';
import { ContextoDaSessao } from './contexto';
import type { EstadoDaSessao } from './contexto';

/**
 * Quem está dentro, e por quanto tempo.
 *
 * ## O que este contexto NÃO sabe
 *
 * Quem é a pessoa. Não há rota que devolva o usuário do token, e este front
 * **não decodifica o JWT** — nem para mostrar um nome. Decodificar sem verificar
 * é o primeiro passo para confiar, e a permissão deste sistema não está no token
 * (`CLAUDE.md`: *"Permissão é do vínculo usuário × estabelecimento e é resolvida
 * por requisição"*). O dia em que houver `GET /api/v1/me`, o nome entra por aqui.
 *
 * E não sabe em que loja a pessoa está. Não existe rota que diga quais
 * estabelecimentos o portador tem — medido em 29/09, e é o que impede a próxima
 * tela de existir.
 *
 * ## A expiração é contada, não esperada
 *
 * O `expiresIn` do login são 1800 segundos, e **não há refresh**. Um temporizador
 * derruba a sessão no instante certo, em vez de deixar a pessoa descobrir pelo
 * primeiro 401 depois de escrever um formulário inteiro. Os dois caminhos
 * existem: o temporizador é a cortesia, o 401 do cliente HTTP é a rede.
 */
export function SessaoProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(() => recuperar());

  const sair = useCallback(() => {
    esquecer();
    setSessao(null);
  }, []);

  const entrar = useCallback(async (telefone: string, senha: string) => {
    const resposta = await chamarLogin(telefone, senha);
    const nova: Sessao = {
      token: resposta.accessToken,
      // O relógio do cliente é o único disponível, e um relógio adiantado
      // encurta a sessão em vez de esticá-la — que é o lado seguro para errar.
      expiraEm: Date.now() + resposta.expiresIn * 1000,
    };
    guardar(nova);
    setSessao(nova);
  }, []);

  /** Um 401 em qualquer requisição derruba a sessão na tela, e não só no armazenamento. */
  useEffect(() => {
    quandoASessaoCair(() => setSessao(null));
  }, []);

  /** O temporizador da expiração, refeito a cada sessão nova. */
  useEffect(() => {
    if (sessao === null) return;
    const restante = sessao.expiraEm - Date.now();
    if (restante <= 0) {
      sair();
      return;
    }
    const agendado = window.setTimeout(sair, restante);
    return () => window.clearTimeout(agendado);
  }, [sessao, sair]);

  const valor = useMemo<EstadoDaSessao>(
    () => ({ sessao, autenticado: sessao !== null, entrar, sair }),
    [sessao, entrar, sair],
  );

  return <ContextoDaSessao.Provider value={valor}>{children}</ContextoDaSessao.Provider>;
}
