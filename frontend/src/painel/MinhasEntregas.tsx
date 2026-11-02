import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import type { FormaDePagamento, PedidoDaLoja } from '../api/pedidos';
import { useSessao } from '../auth/useSessao';
import { emDemonstracao } from '../aki/demo';
import { dataEHora, dinheiro } from '../aki/formatos';
import { toast } from '../aki/toast';
import { acertosDosEntregadores } from '../pedido/fechamento';
import {
  entregasDaDemonstracao,
  entregasDoServidor,
  type FonteDasEntregas,
} from '../pedido/fontes';
import { classeDoStatus, ROTULO_DA_FORMA, ROTULO_DO_STATUS } from '../pedido/regras';
import { lerPreco, mensagemDoErro } from './catalogo';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';

/**
 * O app do entregador da equipe (decisão de 31/10/2026): as entregas que a loja passou para
 * ele hoje, os dois passos que são dele — saí, entreguei — e o que recebeu na porta. Embaixo,
 * os ganhos do dia: o repasse (a taxa de cada entrega) e o acerto com a loja.
 *
 * Na primeira vez ele informa o nome com que a loja vai reconhecê-lo — é por esse nome que a
 * loja o escolhe no pedido.
 */
const INTERVALO = 20_000;

export function MinhasEntregas() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const token = useSessao().sessao?.token ?? '';
  const fonte = useMemo<FonteDasEntregas>(
    () =>
      emDemonstracao() ? entregasDaDemonstracao() : entregasDoServidor(estabelecimentoId, token),
    [estabelecimentoId, token],
  );
  const [eu, definirEu] = useState<{ nome: string } | null | undefined>(undefined);
  const [nome, definirNome] = useState('');
  const [entregas, definirEntregas] = useState<PedidoDaLoja[] | null>(null);
  const [falha, definirFalha] = useState<string | null>(null);

  useEffect(() => {
    fonte
      .eu()
      .then((lido) => definirEu(lido ?? null))
      .catch((erro: unknown) => definirFalha(mensagemDoErro(erro)));
  }, [fonte]);

  const ler = useCallback(() => {
    fonte
      .doDia()
      .then((lidas) => {
        definirEntregas(lidas);
        definirFalha(null);
      })
      .catch((erro: unknown) => definirFalha(mensagemDoErro(erro)));
  }, [fonte]);

  useEffect(() => {
    if (eu === undefined || eu === null) return;
    ler();
    const relogio = window.setInterval(ler, INTERVALO);
    return () => window.clearInterval(relogio);
  }, [eu, ler]);

  async function apresentar(evento: FormEvent) {
    evento.preventDefault();
    try {
      definirEu(await fonte.apresentar(nome.trim()));
      toast('Pronto: a loja já pode passar entregas para você.');
    } catch (erro) {
      definirFalha(mensagemDoErro(erro));
    }
  }

  const lista = useMemo(() => entregas ?? [], [entregas]);
  const ganhos = useMemo(() => acertosDosEntregadores(lista)[0], [lista]);
  const abertas = lista.filter((um) => um.status !== 'ENTREGUE' && um.status !== 'CANCELADO');
  const feitas = lista.filter((um) => um.status === 'ENTREGUE');

  return (
    <CascaDaLoja
      atual="entregas"
      titulo={eu ? `Olá, ${eu.nome.split(' ')[0] ?? ''}!` : 'Minhas entregas'}
      subtitulo={`As suas entregas de hoje na ${loja?.nome ?? 'loja'}.`}
    >
      {falha !== null && (
        <p className="prod-aviso" role="alert">
          {falha}
        </p>
      )}

      {eu === null && (
        <section className="panel prod-panel">
          <div className="table-title">
            <div>
              <h2>Como a loja vai te chamar?</h2>
              <p className="admin-muted">
                É por este nome que a loja escolhe você no pedido. Você só faz isso uma vez.
              </p>
            </div>
          </div>
          <form className="pedido-campo" onSubmit={(evento) => void apresentar(evento)}>
            <input
              className="select"
              aria-label="Seu nome"
              placeholder="Ex.: Carlos Moto"
              maxLength={80}
              value={nome}
              onChange={(evento) => definirNome(evento.target.value)}
            />
            <button type="submit" className="btn btn-primary" disabled={nome.trim() === ''}>
              <span className="material-symbols-outlined">badge</span>Salvar meu nome
            </button>
          </form>
        </section>
      )}

      {eu && (
        <>
          <section className="stats">
            <div className="stat">
              <div className="stat-top">
                <span>Para entregar</span>
                <div className="stat-icon">
                  <span className="material-symbols-outlined">two_wheeler</span>
                </div>
              </div>
              <strong>{abertas.length}</strong>
              <small>Passadas pela loja</small>
            </div>
            <div className="stat">
              <div className="stat-top">
                <span>Entregues hoje</span>
                <div className="stat-icon">
                  <span className="material-symbols-outlined">task_alt</span>
                </div>
              </div>
              <strong>{feitas.length}</strong>
              <small>Entregas concluídas</small>
            </div>
            <div className="stat">
              <div className="stat-top">
                <span>Meus ganhos</span>
                <div className="stat-icon">
                  <span className="material-symbols-outlined">savings</span>
                </div>
              </div>
              <strong>{dinheiro(ganhos?.repasse ?? 0)}</strong>
              <small>A taxa de cada entrega feita</small>
            </div>
            <div className="stat">
              <div className="stat-top">
                <span>Acerto com a loja</span>
                <div className="stat-icon">
                  <span className="material-symbols-outlined">point_of_sale</span>
                </div>
              </div>
              <strong>{dinheiro(Math.abs(ganhos?.acerto ?? 0))}</strong>
              <small>
                {(ganhos?.acerto ?? 0) > 0
                  ? 'Você devolve à loja (dinheiro recebido − ganhos)'
                  : (ganhos?.acerto ?? 0) < 0
                    ? 'A loja paga a você'
                    : 'Nada a acertar'}
              </small>
            </div>
          </section>

          <section className="panel orders-history" id="atual">
            <div className="table-title">
              <div>
                <h2>Entregas de hoje</h2>
                <p className="admin-muted">Atualiza sozinha a cada 20 segundos.</p>
              </div>
              <button type="button" className="btn btn-light" onClick={ler}>
                <span className="material-symbols-outlined">refresh</span>Atualizar
              </button>
            </div>
            {entregas === null ? (
              <div className="ticket-empty">
                <strong>Carregando…</strong>
              </div>
            ) : lista.length === 0 ? (
              <div className="ticket-empty">
                <span className="material-symbols-outlined">two_wheeler</span>
                <strong>Nenhuma entrega com você ainda</strong>
                <p>Quando a loja passar um pedido para você, ele aparece aqui.</p>
              </div>
            ) : (
              <div className="orders-list">
                {lista.map((pedido) => (
                  <CartaoDaEntrega
                    key={pedido.id}
                    pedido={pedido}
                    fonte={fonte}
                    aoMudar={(novo) =>
                      definirEntregas(
                        (atuais) => atuais?.map((um) => (um.id === novo.id ? novo : um)) ?? atuais,
                      )
                    }
                  />
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </CascaDaLoja>
  );
}

function CartaoDaEntrega({
  pedido,
  fonte,
  aoMudar,
}: {
  pedido: PedidoDaLoja;
  fonte: FonteDasEntregas;
  aoMudar: (pedido: PedidoDaLoja) => void;
}) {
  // Começa no que o cliente disse, mas ele pode pagar de outro jeito na porta.
  const [forma, definirForma] = useState<FormaDePagamento>(pedido.pagamentoDeclarado.forma);
  const [recebido, definirRecebido] = useState(
    pedido.pagamentoDeclarado.trocoPara === null ? '' : String(pedido.pagamentoDeclarado.trocoPara),
  );
  const [ocupado, definirOcupado] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);
  const pago = pedido.pagamentoRegistrado;
  const pixAConferir = pago !== null && pago.formaEfetiva === 'PIX' && !pago.pixConferido;
  const endereco = pedido.endereco;
  const mapa =
    endereco === null
      ? null
      : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(
          `${endereco.logradouro}, ${endereco.bairro}`,
        )}`;

  async function fazer(ato: () => Promise<PedidoDaLoja>, aviso: string) {
    definirOcupado(true);
    definirRecusa(null);
    try {
      aoMudar(await ato());
      toast(aviso);
    } catch (erro) {
      definirRecusa(mensagemDoErro(erro));
    } finally {
      definirOcupado(false);
    }
  }

  function receber() {
    let valorRecebido: number | undefined;
    if (forma === 'DINHEIRO') {
      const lido = lerPreco(recebido);
      if (lido === null) {
        definirRecusa('Informe quanto recebeu em dinheiro, como 50,00.');
        return;
      }
      valorRecebido = lido;
    }
    void fazer(
      () =>
        fonte.receber(pedido.id, {
          formaEfetiva: forma,
          ...(valorRecebido === undefined ? {} : { valorRecebido }),
        }),
      'Recebimento registrado',
    );
  }

  return (
    <article className="order-history-card" data-pedido={pedido.numero}>
      <div className="order-history-main">
        <div className="order-number">
          <span className="material-symbols-outlined">two_wheeler</span>
          <div>
            <strong>
              #{pedido.numero} · {pedido.cliente.nome}
            </strong>
            <small>
              {dataEHora(pedido.criadoEm)} · {pedido.cliente.telefone}
            </small>
          </div>
        </div>
        <span className={`ticket-status ${classeDoStatus(pedido.status)}`}>
          {ROTULO_DO_STATUS[pedido.status]}
        </span>
      </div>
      {endereco !== null && (
        <p className="pedido-linha">
          <span className="material-symbols-outlined">location_on</span>
          {endereco.logradouro}, {endereco.bairro}
          {endereco.complemento !== null ? ` — ${endereco.complemento}` : ''}
          {mapa !== null && (
            <a href={mapa} target="_blank" rel="noreferrer">
              {' '}
              Abrir no mapa
            </a>
          )}
        </p>
      )}
      <div className="order-history-items">
        {pedido.itens.map((item, i) => (
          <span key={`${item.produtoId}-${i}`}>
            {item.quantidade}x {item.nome}
          </span>
        ))}
      </div>
      <p className="pedido-linha">
        <span className="material-symbols-outlined">payments</span>
        Cobrar {dinheiro(pedido.total)} — {ROTULO_DA_FORMA[pedido.pagamentoDeclarado.forma]}
        {pedido.pagamentoDeclarado.trocoPara !== null
          ? ` · levar troco para ${dinheiro(pedido.pagamentoDeclarado.trocoPara)}`
          : ''}
        {pixAConferir ? (
          <b className="pedido-pago"> · Pix informado, a loja confere no extrato</b>
        ) : (
          pago !== null && <b className="pedido-pago"> · recebido</b>
        )}
      </p>

      {pedido.status !== 'ENTREGUE' && pedido.status !== 'CANCELADO' && (
        <div className="pedido-acoes">
          {pedido.status === 'SAIU_PARA_ENTREGA' && pago === null && (
            <div className="pedido-campo">
              <select
                className="select"
                aria-label={`Como recebeu o pedido ${pedido.numero}`}
                value={forma}
                onChange={(evento) => definirForma(evento.target.value as FormaDePagamento)}
              >
                <option value="DINHEIRO">Recebi em dinheiro</option>
                <option value="CARTAO">Recebi no cartão</option>
                <option value="PIX">Recebi por Pix (a loja confere)</option>
              </select>
              {forma === 'DINHEIRO' && (
                <input
                  className="select"
                  inputMode="decimal"
                  aria-label="Valor recebido"
                  placeholder={`Recebido (total ${dinheiro(pedido.total)})`}
                  value={recebido}
                  onChange={(evento) => definirRecebido(evento.target.value)}
                />
              )}
              <button type="button" className="btn btn-light" disabled={ocupado} onClick={receber}>
                <span className="material-symbols-outlined">price_check</span>Registrar recebimento
              </button>
            </div>
          )}
          <div className="pedido-campo">
            {pedido.status === 'PRONTO' && (
              <button
                type="button"
                className="btn btn-primary"
                disabled={ocupado}
                onClick={() =>
                  void fazer(
                    () => fonte.avancar(pedido.id, 'SAIU_PARA_ENTREGA'),
                    `Saiu com o pedido #${pedido.numero}`,
                  )
                }
              >
                <span className="material-symbols-outlined">two_wheeler</span>Saí para entregar
              </button>
            )}
            {pedido.status === 'SAIU_PARA_ENTREGA' && (
              <button
                type="button"
                className="btn btn-primary"
                disabled={ocupado}
                onClick={() =>
                  void fazer(
                    () => fonte.avancar(pedido.id, 'ENTREGUE'),
                    `Pedido #${pedido.numero} entregue`,
                  )
                }
              >
                <span className="material-symbols-outlined">task_alt</span>Entreguei
              </button>
            )}
            {(pedido.status === 'CONFIRMADO' || pedido.status === 'EM_PREPARO') && (
              <span className="admin-muted">A loja ainda está preparando este pedido.</span>
            )}
          </div>
        </div>
      )}
      {recusa !== null && (
        <p className="prod-msg erro" role="alert">
          {recusa}
        </p>
      )}
    </article>
  );
}
