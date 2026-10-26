import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import { fraseDaRecusa } from '../api/cardapio';
import { ErroDaApi } from '../api/cliente';
import { aceitarConvite, verConvite, type ConvitePublico } from '../api/convites';
import { useSessao } from '../auth/useSessao';
import { AvisoDoCartao, CabecalhoDoCartao, TelaDeEntrada } from '../aki/TelaDeEntrada';
import { caminhoDaLoja } from '../painel/caminhos';

/**
 * O convite para a equipe (ADR-070): página pública — quem abre o link pode não
 * ter conta. Quem está dentro aceita; quem não está toca em "Aceitar convite",
 * entra ou cria a conta, e na volta o convite é aceito sozinho (ADR-076).
 */
type Papel = NonNullable<ConvitePublico['papel']>;
const PAPEIS: Record<Papel, string> = {
  COLABORADOR: 'colaborador',
  ADMINISTRADOR: 'administrador',
};
const VALIDADE = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeStyle: 'short' });

/** A intenção que viaja com a volta do login (ADR-076). */
export const ACAO_ACEITAR_CONVITE = 'aceitar-convite';

type Leitura =
  | { estado: 'carregando' }
  | { estado: 'pronto'; convite: ConvitePublico }
  | { estado: 'recusado'; frase: string };

export function ConvitePage() {
  const { token = '' } = useParams();
  const local = useLocation();
  const navegar = useNavigate();
  const { autenticado, sessao } = useSessao();
  const [leitura, definirLeitura] = useState<Leitura>({ estado: 'carregando' });
  const [aceitando, definirAceitando] = useState(false);
  const [recusa, definirRecusa] = useState<{ frase: string; status: number } | null>(null);

  useEffect(() => {
    let atual = true;
    verConvite(token)
      .then((convite) => {
        if (atual) definirLeitura({ estado: 'pronto', convite });
      })
      .catch((falha: unknown) => {
        if (!atual) return;
        const frase = 'Não foi possível abrir este convite agora.';
        definirLeitura({ estado: 'recusado', frase: fraseDaRecusa(falha, {}, frase) ?? frase });
      });
    return () => {
      atual = false;
    };
  }, [token]);

  const pediuAceite = (local.state as { acao?: string } | null)?.acao === ACAO_ACEITAR_CONVITE;
  const aceiteAutomaticoTentado = useRef(false);
  const [automaticoTerminou, definirAutomaticoTerminou] = useState(false);
  const aceitandoSozinho = autenticado && pediuAceite && !automaticoTerminou;

  const aceitar = useCallback(async () => {
    if (sessao === null) return;
    definirAceitando(true);
    definirRecusa(null);
    try {
      const vinculo = await aceitarConvite(token, sessao.token);
      void navegar(caminhoDaLoja(String(vinculo.estabelecimentoId)), { replace: true });
    } catch (falha) {
      const frase = fraseDaRecusa(falha, {}, 'Não foi possível aceitar o convite agora.');
      if (frase !== null) {
        definirRecusa({ frase, status: falha instanceof ErroDaApi ? falha.status : 0 });
      }
      definirAceitando(false);
    }
  }, [sessao, token, navegar]);

  // Voltou do login com a intenção de aceitar: aceita uma vez, sozinho.
  const prontoParaAceitar = leitura.estado === 'pronto';
  useEffect(() => {
    if (!prontoParaAceitar || !autenticado || !pediuAceite) return;
    if (aceiteAutomaticoTentado.current) return;
    aceiteAutomaticoTentado.current = true;
    void aceitar().finally(() => definirAutomaticoTerminou(true));
  }, [prontoParaAceitar, autenticado, pediuAceite, aceitar]);

  const destino = { de: local.pathname, acao: ACAO_ACEITAR_CONVITE };

  return (
    <TelaDeEntrada>
      <CabecalhoDoCartao
        selo="Convite"
        icone="group_add"
        titulo="Convite para a equipe"
        texto={
          leitura.estado === 'carregando' ? 'Abrindo o convite…' : 'Confira e aceite o convite.'
        }
      />

      {leitura.estado === 'recusado' && <AvisoDoCartao tom="erro">{leitura.frase}</AvisoDoCartao>}

      {leitura.estado === 'pronto' && (
        <div className="login-form">
          <p>
            Você foi convidado para a equipe de{' '}
            <strong data-testid="nome-da-loja">{leitura.convite.nomeDaLoja}</strong>.
          </p>
          {leitura.convite.papel !== undefined && (
            <p>Você entra como {PAPEIS[leitura.convite.papel]}.</p>
          )}
          {leitura.convite.expiraEm !== undefined && (
            <p>Este convite vale até {VALIDADE.format(new Date(leitura.convite.expiraEm))}.</p>
          )}

          {recusa !== null && (
            <>
              <AvisoDoCartao tom="erro">{recusa.frase}</AvisoDoCartao>
              {recusa.status === 409 && <Link to="/painel">Ir para o painel</Link>}
            </>
          )}

          {aceitandoSozinho ? (
            <p>Aceitando o convite…</p>
          ) : autenticado ? (
            <button
              type="button"
              className={`login-button${aceitando ? ' loading' : ''}`}
              disabled={aceitando}
              onClick={() => void aceitar()}
            >
              <span className="button-text">Aceitar convite</span>
            </button>
          ) : (
            <>
              <p>
                Para entrar na equipe, você acessa sua conta — ou cria uma. Na volta, o convite já
                estará aceito.
              </p>
              <button
                type="button"
                className="login-button"
                onClick={() => void navegar('/entrar', { state: destino })}
              >
                <span className="button-text">Aceitar convite</span>
              </button>
              <div className="login-footer">
                <p>
                  Ainda não possui uma conta?{' '}
                  <Link to="/cadastrar" state={destino}>
                    Cadastre-se
                  </Link>
                </p>
              </div>
            </>
          )}
        </div>
      )}
    </TelaDeEntrada>
  );
}
