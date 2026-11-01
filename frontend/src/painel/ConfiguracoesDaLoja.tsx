import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { emDemonstracao, ID_DA_LOJA_DEMO } from '../aki/demo';
import { toast } from '../aki/toast';
import type { VitrineDaLoja } from '../api/pedidos';
import { vitrineDe } from '../pedido/fontes';
import { quandoAbre } from '../pedido/regras';
import { lerPreco, mensagemDoErro } from './catalogo';
import { caminhoDaLoja } from './caminhos';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';
import { linkDoCardapio, useFonteDePedidos } from './useFonteDePedidos';

/**
 * Configurações da loja para a entrega de 15/11: o **link do cardápio** que a loja manda aos
 * clientes, e a **taxa fixa** e as modalidades (decisão de 26/10/2026). A taxa vale para os
 * pedidos seguintes; os já feitos guardam a do instante em que nasceram.
 */
export function ConfiguracoesDaLoja() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const fonte = useFonteDePedidos(estabelecimentoId);
  const podeConfigurar = loja?.permissoes.includes('CONFIGURAR_LOJA') ?? false;
  const link = linkDoCardapio(emDemonstracao() ? ID_DA_LOJA_DEMO : estabelecimentoId);

  const [taxa, definirTaxa] = useState('');
  const [entrega, definirEntrega] = useState(true);
  const [retirada, definirRetirada] = useState(true);
  const [carregado, definirCarregado] = useState(false);
  const [salvando, definirSalvando] = useState(false);
  const [mensagem, definirMensagem] = useState<{ ok: boolean; texto: string } | null>(null);
  const [situacao, definirSituacao] = useState<VitrineDaLoja | null>(null);
  const vitrine = useMemo(
    () => vitrineDe(emDemonstracao() ? ID_DA_LOJA_DEMO : estabelecimentoId),
    [estabelecimentoId],
  );

  // O que o cliente vê agora pelo link: aberta, ou fechada e quando abre (ADR-077, 01/11/2026).
  useEffect(() => {
    let atual = true;
    vitrine
      .configuracao()
      .then((lida) => {
        if (atual) definirSituacao(lida);
      })
      .catch(() => undefined);
    return () => {
      atual = false;
    };
  }, [vitrine]);

  useEffect(() => {
    if (!podeConfigurar) return;
    let atual = true;
    fonte
      .configuracao()
      .then((lida) => {
        if (!atual) return;
        definirTaxa(lida.taxaDeEntrega.toFixed(2).replace('.', ','));
        definirEntrega(lida.aceitaEntrega);
        definirRetirada(lida.aceitaRetirada);
        definirCarregado(true);
      })
      .catch((erro: unknown) => {
        if (atual) definirMensagem({ ok: false, texto: mensagemDoErro(erro) });
      });
    return () => {
      atual = false;
    };
  }, [fonte, podeConfigurar]);

  async function salvar(evento: FormEvent) {
    evento.preventDefault();
    const valor = lerPreco(taxa === '' ? '0' : taxa);
    if (valor === null) {
      definirMensagem({ ok: false, texto: 'A taxa precisa ser um valor, como 5,00.' });
      return;
    }
    definirSalvando(true);
    definirMensagem(null);
    try {
      const salva = await fonte.definirConfiguracao({
        taxaDeEntrega: valor,
        aceitaEntrega: entrega,
        aceitaRetirada: retirada,
      });
      definirTaxa(salva.taxaDeEntrega.toFixed(2).replace('.', ','));
      definirMensagem({ ok: true, texto: 'Salvo. Vale para os próximos pedidos.' });
    } catch (erro) {
      definirMensagem({ ok: false, texto: mensagemDoErro(erro) });
    } finally {
      definirSalvando(false);
    }
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(link);
      toast('Link do cardápio copiado');
    } catch {
      toast('Não deu para copiar. Selecione o link e copie.');
    }
  }

  const compartilhar = `https://wa.me/?text=${encodeURIComponent(
    `Faça seu pedido na ${loja?.nome ?? 'nossa loja'} pelo cardápio online: ${link}`,
  )}`;

  return (
    <CascaDaLoja
      atual="configuracoes"
      titulo="Configurações"
      subtitulo="O link do cardápio, a taxa de entrega e as modalidades."
    >
      <section className="panel prod-panel">
        <div className="table-title">
          <div>
            <h2>Link do cardápio</h2>
            <p className="admin-muted">
              Mande este link aos clientes. Eles pedem sem criar conta, e o pedido cai em Pedidos.
            </p>
          </div>
        </div>
        <div className="link-do-cardapio">
          <code>{link}</code>
          <button type="button" className="btn btn-light" onClick={() => void copiar()}>
            <span className="material-symbols-outlined">content_copy</span>Copiar
          </button>
          <a className="btn btn-primary" href={compartilhar} target="_blank" rel="noreferrer">
            <span className="material-symbols-outlined">chat</span>Enviar pelo WhatsApp
          </a>
          <a className="btn btn-light" href={link} target="_blank" rel="noreferrer">
            <span className="material-symbols-outlined">open_in_new</span>Abrir
          </a>
        </div>
        {situacao?.aberta === true && (
          <p className="prod-nota">
            Agora a loja está <b>aberta</b> e o link aceita pedidos.
          </p>
        )}
        {situacao?.aberta === false && (
          <p className="prod-nota">
            Agora a loja está <b>fechada</b> e o link não aceita pedidos
            {situacao.proximaAbertura != null
              ? ` — abre ${quandoAbre(situacao.proximaAbertura)}`
              : ' — o horário de funcionamento ainda não foi definido'}
            . <Link to={`${caminhoDaLoja(estabelecimentoId)}/horario`}>Ajustar o horário</Link>
          </p>
        )}
      </section>

      <section className="panel prod-panel">
        <div className="table-title">
          <div>
            <h2>Entrega e retirada</h2>
            <p className="admin-muted">
              Taxa fixa por pedido de entrega. Na retirada não se cobra.
            </p>
          </div>
        </div>
        {!podeConfigurar ? (
          <p className="prod-nota">
            Só quem pode configurar a loja altera a taxa e as modalidades.
          </p>
        ) : (
          <form className="prod-form" onSubmit={(evento) => void salvar(evento)} noValidate>
            <fieldset className="prod-grupo" disabled={!carregado || salvando}>
              <legend>O que a loja aceita</legend>
              <label className="prod-campo">
                Taxa de entrega (R$)
                <input
                  inputMode="decimal"
                  placeholder="0,00"
                  value={taxa}
                  onChange={(evento) => definirTaxa(evento.target.value)}
                />
              </label>
              <label className="prod-opcao">
                <input
                  type="checkbox"
                  checked={entrega}
                  onChange={(evento) => definirEntrega(evento.target.checked)}
                />
                <span>
                  <b>Entrega</b>
                  <small>O cliente informa o endereço e paga a taxa.</small>
                </span>
              </label>
              <label className="prod-opcao">
                <input
                  type="checkbox"
                  checked={retirada}
                  onChange={(evento) => definirRetirada(evento.target.checked)}
                />
                <span>
                  <b>Retirada no balcão</b>
                  <small>O cliente busca na loja, sem taxa.</small>
                </span>
              </label>
            </fieldset>
            <div className="prod-acoes">
              <button type="submit" className="btn btn-primary" disabled={!carregado || salvando}>
                <span className="material-symbols-outlined">save</span>
                {salvando ? 'Salvando…' : 'Salvar'}
              </button>
            </div>
            {mensagem !== null && (
              <p className={`prod-msg ${mensagem.ok ? 'ok' : 'erro'}`} role="alert">
                {mensagem.texto}
              </p>
            )}
          </form>
        )}
      </section>
    </CascaDaLoja>
  );
}
