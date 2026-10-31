import { useState, type FormEvent } from 'react';
import { fraseDaRecusa } from '../api/cardapio';
import { convidar, linkDoConvite, type PermissaoDoConvite } from '../api/convites';
import { useSessao } from '../auth/useSessao';
import { emDemonstracao } from '../aki/demo';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';

/**
 * Equipe: convidar alguém pelo telefone, com as permissões marcadas (ADR-070).
 * Só se oferecem as permissões que quem convida tem — o servidor confere de novo.
 */
const ROTULOS: Record<PermissaoDoConvite, string> = {
  VER_PRODUTO: 'Ver o cardápio',
  CRIAR_PRODUTO: 'Criar seção e produto',
  ALTERAR_PRODUTO: 'Publicar e marcar o que acabou',
  DESATIVAR_PRODUTO: 'Tirar produto do cardápio',
  VER_PEDIDO: 'Ver pedidos',
  ALTERAR_STATUS: 'Mudar o andamento do pedido',
  VER_VENDAS: 'Ver vendas',
  VER_ENTREGA: 'Entregador: ver e fazer as próprias entregas',
  GERENCIAR_EQUIPE: 'Convidar e gerenciar a equipe',
  GERENCIAR_JORNADA: 'Abrir e fechar a jornada dos entregadores',
};
const ORDEM = Object.keys(ROTULOS) as PermissaoDoConvite[];

export function Equipe() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const token = useSessao().sessao?.token;
  const [telefone, definirTelefone] = useState('');
  const [marcadas, definirMarcadas] = useState<PermissaoDoConvite[]>([]);
  const [enviando, definirEnviando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);
  const [link, definirLink] = useState<string | null>(null);
  const [copiou, definirCopiou] = useState<'sim' | 'falhou' | null>(null);
  const oferecidas = ORDEM.filter((permissao) => (loja?.permissoes ?? []).includes(permissao));

  function alternar(permissao: PermissaoDoConvite) {
    definirMarcadas((atual) =>
      atual.includes(permissao) ? atual.filter((uma) => uma !== permissao) : [...atual, permissao],
    );
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (telefone.trim() === '') {
      definirRecusa('Informe o telefone de quem você vai convidar.');
      return;
    }
    definirRecusa(null);
    if (emDemonstracao()) {
      definirLink(linkDoConvite(window.location.origin, 'demonstracao'));
      definirCopiou(null);
      return;
    }
    if (token === undefined) return;
    definirEnviando(true);
    try {
      const emitido = await convidar(
        estabelecimentoId,
        { telefone: telefone.trim(), permissoes: marcadas },
        token,
      );
      definirLink(linkDoConvite(window.location.origin, String(emitido.token ?? '')));
      definirCopiou(null);
    } catch (falha) {
      const frase = fraseDaRecusa(
        falha,
        { 403: 'Você não tem permissão para convidar nesta loja.' },
        'Não foi possível criar o convite agora.',
      );
      if (frase !== null) definirRecusa(frase);
    } finally {
      definirEnviando(false);
    }
  }

  async function copiar(texto: string) {
    try {
      await navigator.clipboard.writeText(texto);
      definirCopiou('sim');
    } catch {
      definirCopiou('falhou');
    }
  }

  return (
    <CascaDaLoja atual="equipe" titulo="Equipe" subtitulo="Convide quem trabalha com você.">
      {link !== null && (
        <div className="prod-nota" role="status">
          <b>Copie agora: este link não aparece de novo.</b>
          <p data-testid="link-do-convite" style={{ wordBreak: 'break-all', margin: '8px 0' }}>
            {link}
          </p>
          <div className="prod-acoes">
            <button type="button" className="btn btn-light" onClick={() => void copiar(link)}>
              <span className="material-symbols-outlined">content_copy</span>Copiar link
            </button>
            {copiou === 'sim' && <span>Copiado.</span>}
            {copiou === 'falhou' && (
              <span>Não deu para copiar daqui. Selecione o link acima e copie.</span>
            )}
          </div>
          <small>
            Não há lista de convites pendentes: depois de sair desta tela, não há onde reencontrar
            este link. Se ele se perder, crie outro convite.
          </small>
        </div>
      )}

      <section className="panel prod-panel">
        <div className="table-title">
          <div>
            <h2>Convidar para a equipe</h2>
            <p className="admin-muted">
              Quem aceitar entra na loja como colaborador, com as permissões que você marcar.
            </p>
          </div>
          <span className="material-symbols-outlined section-icon">group_add</span>
        </div>
        <form className="prod-form" noValidate onSubmit={(evento) => void enviar(evento)}>
          <label className="prod-campo">
            Telefone de quem você vai convidar
            <input
              inputMode="tel"
              placeholder="(21) 99999-0000"
              value={telefone}
              onChange={(evento) => {
                definirTelefone(evento.target.value);
                definirRecusa(null);
              }}
              aria-required
            />
          </label>
          <fieldset className="prod-grupo">
            <legend>O que essa pessoa vai poder fazer</legend>
            {oferecidas.map((permissao) => (
              <label key={permissao} className="prod-opcao">
                <input
                  type="checkbox"
                  checked={marcadas.includes(permissao)}
                  onChange={() => alternar(permissao)}
                />{' '}
                <span>
                  <b>{ROTULOS[permissao]}</b>
                </span>
              </label>
            ))}
          </fieldset>
          {recusa !== null && (
            <p className="prod-msg erro" role="alert">
              {recusa}
            </p>
          )}
          <div className="prod-acoes">
            <button type="submit" className="btn btn-primary" aria-busy={enviando}>
              <span className="material-symbols-outlined">send</span>Criar convite
            </button>
          </div>
        </form>
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Equipe
      </footer>
    </CascaDaLoja>
  );
}
