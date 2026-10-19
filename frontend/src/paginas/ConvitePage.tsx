import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';

import { fraseDaRecusa } from '../api/cardapio';
import { ErroDaApi } from '../api/cliente';
import { aceitarConvite, verConvite, type ConvitePublico } from '../api/convites';
import { useSessao } from '../auth/useSessao';
import { Aviso } from '../componentes/Aviso';
import { Botao } from '../componentes/Botao';
import { caminhoDaLoja } from '../painel/caminhos';

type Papel = NonNullable<ConvitePublico['papel']>;

const PAPEIS: Record<Papel, string> = {
  COLABORADOR: 'colaborador',
  ADMINISTRADOR: 'administrador',
};

const VALIDADE = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeStyle: 'short' });

/**
 * A intenção que atravessa o login (ADR-076). É texto, e não booleano: uma marca `aceitar`
 * só serviria a esta tela. Ela viaja no `state` do roteador, ao lado do `de` — no histórico
 * daquela aba: sobrevive a recarregar aquela entrada, e não chega a um link aberto de novo.
 */
export const ACAO_ACEITAR_CONVITE = 'aceitar-convite';

type Leitura =
  | { estado: 'carregando' }
  | { estado: 'pronto'; convite: ConvitePublico }
  | { estado: 'recusado'; frase: string };

/**
 * `/convite/:token` — **pública** (ADR-070 §4): a primeira tela deste front sem sessão que
 * não é entrada nem cadastro. Fica fora do `ExigeSessao`.
 *
 * <h2>O que ela pede, e o que não pede</h2>
 *
 * **Uma rota só**: a leitura pública do convite, sem token de sessão. Não pede as lojas de
 * quem abriu — quem não entrou receberia 401, e o 401 derruba a sessão.
 *
 * <h2>O que ela mostra</h2>
 *
 * O que a rota devolveu: o nome da loja, o papel com que se entra e até quando vale. Nada
 * que identifique pessoa — e quem convidou não aparece, porque nenhum serviço o sabe dizer
 * (ADR-069 §2).
 *
 * <h2>Uma ação, e três entradas (ADR-076)</h2>
 *
 * **Sem sessão**, a ação é aceitar: ela leva ao login levando **esta URL** como destino, pelo
 * mesmo `state.de` que o `ExigeSessao` já usa (ADR-070 §3), e a intenção ao lado dele. **Com
 * sessão e com a intenção**, aceita ao montar, sem segundo clique. **Com sessão e sem a
 * intenção** — quem abriu a URL direto —, o botão, como antes. Sem sessão a intenção não
 * significa nada: ela é pedido, nunca permissão. No 200, o painel da loja, que relê as lojas
 * ao montar.
 *
 * <h2>As recusas</h2>
 *
 * O `detail` do servidor, como veio (ADR-055): o convite que não vale mais, quem convidou
 * que perdeu o poder de conceder, quem já é da equipe.
 */
export function ConvitePage() {
  const { token = '' } = useParams();
  const local = useLocation();
  const navegar = useNavigate();
  const { autenticado, sessao } = useSessao();

  const [leitura, definirLeitura] = useState<Leitura>({ estado: 'carregando' });
  const [aceitando, definirAceitando] = useState(false);
  const [recusaDoAceite, definirRecusaDoAceite] = useState<{
    frase: string;
    status: number;
  } | null>(null);
  const pediuAceite = (local.state as { acao?: string } | null)?.acao === ACAO_ACEITAR_CONVITE;
  // O aceite automático acontece uma vez por montagem: se ele falhar, a recusa fica na tela
  // e o botão volta, mas nada tenta de novo sozinho. A guarda é defesa: nenhum teste a
  // derruba (W-I) — o StrictMode só repete efeito na montagem, quando a leitura ainda não
  // está pronta, e depois de uma recusa as dependências do efeito não mudam.
  const aceiteAutomaticoTentado = useRef(false);
  const [automaticoTerminou, definirAutomaticoTerminou] = useState(false);
  const aceitandoSozinho = autenticado && pediuAceite && !automaticoTerminou;

  useEffect(() => {
    let atual = true;
    verConvite(token)
      .then((convite) => {
        if (atual) definirLeitura({ estado: 'pronto', convite });
      })
      .catch((falha: unknown) => {
        if (!atual) return;
        definirLeitura({
          estado: 'recusado',
          frase:
            fraseDaRecusa(falha, {}, 'Não foi possível abrir este convite agora.') ??
            'Não foi possível abrir este convite agora.',
        });
      });
    return () => {
      atual = false;
    };
  }, [token]);

  const aceitar = useCallback(async () => {
    if (sessao === null) return;
    definirAceitando(true);
    definirRecusaDoAceite(null);
    try {
      const vinculo = await aceitarConvite(token, sessao.token);
      void navegar(caminhoDaLoja(String(vinculo.estabelecimentoId)), { replace: true });
    } catch (falha) {
      const frase = fraseDaRecusa(falha, {}, 'Não foi possível aceitar o convite agora.');
      if (frase !== null) {
        const status = falha instanceof ErroDaApi ? falha.status : 0;
        definirRecusaDoAceite({ frase, status });
      }
      definirAceitando(false);
    }
  }, [sessao, token, navegar]);

  const prontoParaAceitar = leitura.estado === 'pronto';
  useEffect(() => {
    if (!prontoParaAceitar || !autenticado || !pediuAceite) return;
    if (aceiteAutomaticoTentado.current) return;
    aceiteAutomaticoTentado.current = true;
    void aceitar().finally(() => definirAutomaticoTerminou(true));
  }, [prontoParaAceitar, autenticado, pediuAceite, aceitar]);

  const destino = { de: local.pathname, acao: ACAO_ACEITAR_CONVITE };

  return (
    <main className="mx-auto flex w-full max-w-md flex-col gap-6 px-4 py-10">
      <h1 className="text-2xl font-semibold text-slate-900 dark:text-slate-50">Convite</h1>

      {leitura.estado === 'carregando' && (
        <p className="text-sm text-slate-500 dark:text-slate-400">Abrindo o convite…</p>
      )}

      {leitura.estado === 'recusado' && <Aviso tom="erro">{leitura.frase}</Aviso>}

      {leitura.estado === 'pronto' && (
        <>
          <section className="flex flex-col gap-1">
            <p className="text-lg text-slate-900 dark:text-slate-100">
              Você foi convidado para a equipe de{' '}
              <strong data-testid="nome-da-loja">{leitura.convite.nomeDaLoja}</strong>.
            </p>
            {leitura.convite.papel !== undefined && (
              <p className="text-sm text-slate-600 dark:text-slate-300">
                Você entra como {PAPEIS[leitura.convite.papel]}.
              </p>
            )}
            {leitura.convite.expiraEm !== undefined && (
              <p className="text-sm text-slate-600 dark:text-slate-300">
                Este convite vale até {VALIDADE.format(new Date(leitura.convite.expiraEm))}.
              </p>
            )}
          </section>

          {recusaDoAceite !== null && (
            <div className="flex flex-col gap-2">
              <Aviso tom="erro">{recusaDoAceite.frase}</Aviso>
              {recusaDoAceite.status === 409 && (
                <Link to="/painel" className="text-sm underline underline-offset-4">
                  Ir para o painel
                </Link>
              )}
            </div>
          )}

          {aceitandoSozinho ? (
            <p className="text-sm text-slate-600 dark:text-slate-300">Aceitando o convite…</p>
          ) : autenticado ? (
            <div>
              <Botao type="button" ocupado={aceitando} onClick={() => void aceitar()}>
                Aceitar convite
              </Botao>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              <p className="text-sm text-slate-600 dark:text-slate-300">
                Para entrar na equipe, você acessa sua conta — ou cria uma. Na volta, o convite já
                estará aceito.
              </p>
              <div>
                <Botao type="button" onClick={() => void navegar('/entrar', { state: destino })}>
                  Aceitar convite
                </Botao>
              </div>
            </div>
          )}
        </>
      )}
    </main>
  );
}
