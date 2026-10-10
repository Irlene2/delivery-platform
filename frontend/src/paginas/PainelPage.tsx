import { useState } from 'react';
import { Outlet } from 'react-router';

import { useRecurso } from '../api/useRecurso';
import { useSessao } from '../auth/useSessao';
import { Botao } from '../componentes/Botao';
import type { ContextoDoPainel } from '../painel/contextoDoPainel';
import type { LojaDoUsuario } from '../painel/tipos';

/**
 * O painel: o cabeçalho da sessão, as lojas do portador, e a tela que a URL pedir.
 *
 * <h2>O que veio da W-A e continua</h2>
 *
 * O cabeçalho com a sessão e o botão de sair. Não há refresh token (ADR-037:
 * *"o access token é a sessão"*), e esconder a expiração não a evita — só faz a
 * pessoa descobrir no meio de um formulário.
 *
 * <h2>O que a W-B trouxe</h2>
 *
 * As lojas vêm de `GET /api/v1/me/estabelecimentos` (G-B5), com papel e
 * permissões de cada uma. O menu é construído **das permissões**, por loja. Nada
 * aqui decide acesso: o que protege é o servidor, e a tela só desenha o que ele
 * respondeu.
 *
 * <h2>O que a W-E mudou: a loja e a seção moram na URL</h2>
 *
 * Até a W-D a loja e a seção eram estado desta página, e o gatilho escrito no
 * `MenuDoPainel` — *"a segunda seção com tela"* — venceu na W-C sem ser lido. Agora
 * cada tela tem endereço (ADR-061): esta página é a moldura, lê as lojas uma vez e as
 * entrega pelo `Outlet` a quem mora embaixo. Recarregar não perde a loja, e o `201`
 * do cadastro tem para onde ir.
 */
export function PainelPage() {
  const { sessao, sair } = useSessao();
  // Reler as lojas é remontar quem as lê — o mesmo truque da lista de produtos, para
  // que o `useRecurso`, que é de todas as telas, não ganhe parâmetro só para isto.
  const [geracao, definirGeracao] = useState(0);

  if (sessao === null) return null;

  const minutos = Math.max(0, Math.round((sessao.expiraEm - Date.now()) / 60_000));

  return (
    <main className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-10">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
            Painel
          </h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Sessão ativa · expira em aproximadamente {minutos} min
          </p>
        </div>
        <Botao variante="discreto" onClick={sair}>
          Sair
        </Botao>
      </header>

      <LojasDoPainel key={geracao} recarregarLojas={() => definirGeracao((atual) => atual + 1)} />
    </main>
  );
}

function LojasDoPainel({ recarregarLojas }: { recarregarLojas: () => void }) {
  const lojas = useRecurso<LojaDoUsuario[]>('/api/v1/me/estabelecimentos');

  if (lojas.estado === 'carregando') {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando as suas lojas…</p>;
  }

  if (lojas.estado === 'erro') {
    return (
      <p role="alert" className="text-sm text-amber-700 dark:text-amber-400">
        Não deu para carregar as suas lojas agora.
      </p>
    );
  }

  const contexto: ContextoDoPainel = { lojas: lojas.dados, recarregarLojas };
  return <Outlet context={contexto} />;
}
