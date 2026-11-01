import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import type { PedidoDaLoja } from '../api/pedidos';
import { dataEHora, dinheiro } from '../aki/formatos';
import { JanelaDoTicket } from '../aki/Ticket';
import { toast } from '../aki/toast';
import {
  acertosDosEntregadores,
  FAIXAS_DE_HORARIO,
  paraOTicket,
  resumoDoDia,
} from '../pedido/fechamento';
import {
  classeDoStatus,
  linkDoWhatsApp,
  mensagemDoWhatsApp,
  ROTULO_DA_FORMA,
  ROTULO_DO_STATUS,
} from '../pedido/regras';
import { CascaDaLoja } from './CascaDaLoja';
import { caminhoDaLoja } from './caminhos';
import { useLojaAtual } from './lojaAtual';
import { linkDoAcompanhamento, linkDoCardapio, useFonteDePedidos } from './useFonteDePedidos';
import { usePedidosDoDia } from './usePedidosDoDia';

/**
 * O painel administrativo do protótipo (`administrador.html`), com os **pedidos de verdade**
 * do dia (ADR-077). Os números de exemplo e as mensagens de exemplo saíram: tudo aqui vem da
 * mesma lista da fila de Pedidos.
 *
 * Loja nova, sem produto publicado, vê primeiro os primeiros passos — cadastrar o cardápio,
 * a taxa, o link e a equipe.
 */
export function DashboardAdmin() {
  const { estabelecimentoId, loja, fonte } = useLojaAtual();
  const fonteDePedidos = useFonteDePedidos(estabelecimentoId);
  const { pedidos, falha } = usePedidosDoDia(fonteDePedidos);
  const [ticket, definirTicket] = useState<PedidoDaLoja | null>(null);
  const [pedidoDoWhats, definirPedidoDoWhats] = useState('');
  const [publicados, definirPublicados] = useState<number | null>(null);

  useEffect(() => {
    let atual = true;
    fonte
      .carregar()
      .then((catalogo) => {
        if (atual) definirPublicados(catalogo.itens.filter((um) => um.situacao === 'ATIVO').length);
      })
      .catch(() => undefined);
    return () => {
      atual = false;
    };
  }, [fonte]);

  const lista = useMemo(() => pedidos ?? [], [pedidos]);
  const resumo = useMemo(() => resumoDoDia(lista), [lista]);
  const emRota = useMemo(
    () => acertosDosEntregadores(lista).filter((um) => um.emRota > 0).length,
    [lista],
  );
  const maiorHorario = Math.max(...resumo.porHorario, 1);
  const formas = Object.entries(resumo.porForma) as [keyof typeof ROTULO_DA_FORMA, number][];
  const maiorForma = Math.max(...formas.map(([, valor]) => valor), 1);
  const base = caminhoDaLoja(estabelecimentoId);
  const nomeDaLoja = loja?.nome ?? 'nossa loja';

  function abrirWhatsApp() {
    const pedido = lista.find((um) => um.id === pedidoDoWhats);
    if (pedido === undefined) {
      toast('Selecione um pedido.');
      return;
    }
    const texto = mensagemDoWhatsApp(
      pedido,
      nomeDaLoja,
      linkDoAcompanhamento(pedido.codigoDeAcompanhamento),
    );
    window.open(linkDoWhatsApp(pedido.cliente.telefone, texto), '_blank', 'noopener');
  }

  return (
    <CascaDaLoja
      atual="dashboard"
      titulo="Painel administrativo"
      subtitulo={`Visão geral do dia na ${nomeDaLoja}.`}
    >
      {publicados === 0 && <PrimeirosPassos base={base} link={linkDoCardapio(estabelecimentoId)} />}
      {falha !== null && (
        <p className="prod-aviso" role="alert">
          {falha}
        </p>
      )}

      <section className="stats">
        <div className="stat">
          <div className="stat-top">
            <span>Pedidos hoje</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">receipt_long</span>
            </div>
          </div>
          <strong id="todayOrders">{resumo.pedidos}</strong>
          <small>
            {resumo.novos > 0 ? `${resumo.novos} esperando confirmação` : 'Pedidos recebidos hoje'}
          </small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Em preparo</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">restaurant</span>
            </div>
          </div>
          <strong>{resumo.naCozinha}</strong>
          <small>Confirmados, em preparo e prontos</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Em entrega</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">two_wheeler</span>
            </div>
          </div>
          <strong>{resumo.emEntrega}</strong>
          <small>{emRota === 1 ? '1 entregador em rota' : `${emRota} entregadores em rota`}</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Vendas hoje</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">payments</span>
            </div>
          </div>
          <strong id="todaySales">{dinheiro(resumo.vendas)}</strong>
          <small>
            {resumo.concluidos}{' '}
            {resumo.concluidos === 1 ? 'pedido concluído' : 'pedidos concluídos'}
          </small>
        </div>
      </section>

      <div className="dashboard-grid admin-dashboard-grid">
        <section className="panel admin-chart-panel">
          <div className="table-title">
            <div>
              <h2>Vendas do dia</h2>
              <p className="admin-muted">Pedidos concluídos, pelo horário em que foram feitos.</p>
            </div>
            <span className="status orange">Hoje</span>
          </div>
          <div className="sales-chart" id="salesChart" aria-label="Gráfico de vendas do dia">
            {resumo.porHorario.map((valor, i) => (
              <div
                className="sales-bar-wrap"
                key={FAIXAS_DE_HORARIO[i]}
                title={`${FAIXAS_DE_HORARIO[i] ?? ''} — ${dinheiro(valor)}`}
              >
                <div className="sales-value">{valor > 0 ? dinheiro(valor) : ''}</div>
                <div
                  className="sales-bar"
                  style={{ height: `${Math.max(8, (valor / maiorHorario) * 180)}px` }}
                />
                <small>{FAIXAS_DE_HORARIO[i]}</small>
              </div>
            ))}
          </div>
          <div className="chart-axis">
            {FAIXAS_DE_HORARIO.map((hora) => (
              <span key={hora}>{hora}</span>
            ))}
          </div>
        </section>
        <section className="panel payment-panel" id="pagamentos">
          <div className="table-title">
            <div>
              <h2>Formas de pagamento</h2>
              <p className="admin-muted">O que entrou hoje, pela forma registrada.</p>
            </div>
            <span className="material-symbols-outlined section-icon">account_balance_wallet</span>
          </div>
          <div className="payment-list" id="paymentList">
            {formas.map(([forma, valor]) => (
              <div className="payment-row" key={forma}>
                <div>
                  <span>{ROTULO_DA_FORMA[forma]}</span>
                  <strong>{dinheiro(valor)}</strong>
                </div>
                <div className="payment-track">
                  <i style={{ width: `${(valor / maiorForma) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
          <Link className="go-orders" to={`${base}/fechamento`}>
            <span className="material-symbols-outlined">point_of_sale</span>Ver o fechamento do dia
          </Link>
        </section>
      </div>

      <section className="panel whatsapp-panel" id="whatsapp">
        <div className="table-title">
          <div>
            <h2>
              <span className="material-symbols-outlined inline-icon">chat</span> WhatsApp
            </h2>
            <p className="admin-muted">
              Avise o cliente pelo WhatsApp dele, com o status atual e o link do acompanhamento.
            </p>
          </div>
        </div>
        <div className="whatsapp-grid">
          <div className="whatsapp-info">
            <div className="whatsapp-icon">
              <span className="material-symbols-outlined">chat</span>
            </div>
            <div>
              <strong>Central de mensagens</strong>
              <p>Escolha um pedido de hoje: a mensagem sai pronta com o status em que ele está.</p>
            </div>
          </div>
          <div className="whatsapp-actions">
            <select
              id="whatsappOrder"
              value={pedidoDoWhats}
              onChange={(evento) => definirPedidoDoWhats(evento.target.value)}
            >
              <option value="">Selecione um pedido</option>
              {lista.map((pedido) => (
                <option key={pedido.id} value={pedido.id}>
                  #{pedido.numero} · {pedido.cliente.nome} · {ROTULO_DO_STATUS[pedido.status]}
                </option>
              ))}
            </select>
            <button
              className="btn btn-primary"
              id="sendWhatsapp"
              type="button"
              onClick={abrirWhatsApp}
            >
              <span className="material-symbols-outlined">send</span> Abrir WhatsApp
            </button>
          </div>
        </div>
      </section>

      <section className="panel whatsapp-messages-panel" id="mensagens-whatsapp">
        <div className="table-title">
          <div>
            <h2>
              <span className="material-symbols-outlined inline-icon">forum</span> Mensagens dos
              pedidos de hoje
            </h2>
            <p className="admin-muted">A mensagem que cada pedido manda, no status em que está.</p>
          </div>
        </div>
        <div id="whatsappMessages" className="whatsapp-messages-list">
          {lista.length === 0 ? (
            <div className="empty-menu">
              <span className="material-symbols-outlined">forum</span>
              <strong>Nenhum pedido hoje</strong>
              <p>As mensagens aparecem aqui quando os pedidos chegarem.</p>
            </div>
          ) : (
            lista.slice(0, 8).map((pedido, i) => (
              <details className="whatsapp-message" key={pedido.id} open={i === 0}>
                <summary>
                  <span className="message-avatar">
                    <span className="material-symbols-outlined">chat</span>
                  </span>
                  <span className="message-summary">
                    <strong>{pedido.cliente.nome}</strong>
                    <small>
                      Pedido #{pedido.numero} · {ROTULO_DO_STATUS[pedido.status]}
                    </small>
                  </span>
                  <span className="material-symbols-outlined expand-icon">expand_more</span>
                </summary>
                <div className="message-body">
                  <p>
                    {mensagemDoWhatsApp(
                      pedido,
                      nomeDaLoja,
                      linkDoAcompanhamento(pedido.codigoDeAcompanhamento),
                    )}
                  </p>
                  <div className="message-meta">
                    <span>
                      <span className="material-symbols-outlined">payments</span>
                      {ROTULO_DA_FORMA[pedido.pagamentoDeclarado.forma]}
                    </span>
                    <a
                      href={linkDoWhatsApp(
                        pedido.cliente.telefone,
                        mensagemDoWhatsApp(
                          pedido,
                          nomeDaLoja,
                          linkDoAcompanhamento(pedido.codigoDeAcompanhamento),
                        ),
                      )}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Enviar
                    </a>
                  </div>
                </div>
              </details>
            ))
          )}
        </div>
      </section>

      <section className="panel order-total-card">
        <div className="stat-top">
          <span>Ticket médio</span>
          <div className="stat-icon">
            <span className="material-symbols-outlined">receipt_long</span>
          </div>
        </div>
        <strong data-orders-total="">{dinheiro(resumo.ticketMedio)}</strong>
        <small>
          <b data-orders-count="">{resumo.concluidos}</b> pedidos concluídos hoje ·{' '}
          {dinheiro(resumo.taxas)} em taxas de entrega
        </small>
      </section>

      <div className="dashboard-grid" id="pedidos">
        <section className="panel table-panel">
          <div className="table-title">
            <h2>Pedidos recentes</h2>
            <Link className="btn btn-light" to={`${base}/pedidos`}>
              Abrir a fila
            </Link>
          </div>
          <div className="table-scroll">
            <table className="orders-table">
              <thead>
                <tr>
                  <th>Pedido</th>
                  <th>Cliente</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Ticket</th>
                </tr>
              </thead>
              <tbody id="adminOrdersBody">
                {lista.length === 0 ? (
                  <tr>
                    <td colSpan={5}>
                      {pedidos === null ? 'Carregando…' : 'Nenhum pedido hoje ainda.'}
                    </td>
                  </tr>
                ) : (
                  lista.slice(0, 8).map((pedido) => (
                    <tr key={pedido.id}>
                      <td>
                        <strong>#{pedido.numero}</strong>
                        <br />
                        <small>{dataEHora(pedido.criadoEm)}</small>
                      </td>
                      <td>{pedido.cliente.nome}</td>
                      <td>{dinheiro(pedido.total)}</td>
                      <td>
                        <span className={`status ${classeDoStatus(pedido.status)}`}>
                          {ROTULO_DO_STATUS[pedido.status]}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="ticket-btn"
                          onClick={() => definirTicket(pedido)}
                        >
                          <span className="material-symbols-outlined">receipt_long</span> Ticket
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
        <section className="panel quick-panel">
          <div className="table-title">
            <h2>Acesso rápido</h2>
          </div>
          <div className="quick-list">
            <Link className="quick-card" to={`${base}/pedidos`}>
              <span className="material-symbols-outlined">receipt_long</span>
              <div>
                <strong>Fila de pedidos</strong>
                <small>Confirmar, preparar e despachar</small>
              </div>
            </Link>
            <Link className="quick-card" to={`${base}/produtos`}>
              <span className="material-symbols-outlined">restaurant_menu</span>
              <div>
                <strong>Gerenciar catálogo</strong>
                <small>Produtos, preços e categorias</small>
              </div>
            </Link>
            <Link className="quick-card" to={`${base}/fechamento`}>
              <span className="material-symbols-outlined">point_of_sale</span>
              <div>
                <strong>Fechamento do dia</strong>
                <small>Caixa e acerto dos entregadores</small>
              </div>
            </Link>
            <Link className="quick-card" to={`${base}/configuracoes`}>
              <span className="material-symbols-outlined">settings</span>
              <div>
                <strong>Configurações</strong>
                <small>Link do cardápio, taxa e modalidades</small>
              </div>
            </Link>
          </div>
        </section>
      </div>
      <footer className="footer">
        <b>Aki Delivery</b> © 2026 — Painel administrativo
      </footer>

      <JanelaDoTicket
        pedido={ticket === null ? null : paraOTicket(ticket)}
        aoFechar={() => definirTicket(null)}
        linkDoCardapio={`${base}/cardapio`}
      />
    </CascaDaLoja>
  );
}

/** A loja nova: o que fazer antes do primeiro pedido, na ordem. */
function PrimeirosPassos({ base, link }: { base: string; link: string }) {
  return (
    <section className="panel prod-panel" aria-label="Primeiros passos">
      <div className="table-title">
        <div>
          <h2>Primeiros passos da sua loja</h2>
          <p className="admin-muted">
            Cinco passos e a loja está pronta para receber pedidos pelo link.
          </p>
        </div>
      </div>
      <div className="quick-list">
        <Link className="quick-card" to={`${base}/produtos/novo`}>
          <span className="material-symbols-outlined">restaurant_menu</span>
          <div>
            <strong>1. Cadastre e publique os produtos</strong>
            <small>Só o que está publicado aparece no cardápio do cliente.</small>
          </div>
        </Link>
        <Link className="quick-card" to={`${base}/configuracoes`}>
          <span className="material-symbols-outlined">two_wheeler</span>
          <div>
            <strong>2. Defina a taxa de entrega</strong>
            <small>E se a loja faz entrega, retirada ou as duas.</small>
          </div>
        </Link>
        <Link className="quick-card" to={`${base}/horario`}>
          <span className="material-symbols-outlined">schedule</span>
          <div>
            <strong>3. Defina o horário de funcionamento</strong>
            <small>
              Fora do horário, o cardápio avisa que a loja está fechada e não aceita pedido.
            </small>
          </div>
        </Link>
        <Link className="quick-card" to={`${base}/equipe`}>
          <span className="material-symbols-outlined">group_add</span>
          <div>
            <strong>4. Convide a equipe e os entregadores</strong>
            <small>Entregador recebe a permissão de entregas e usa o app dele.</small>
          </div>
        </Link>
        <Link className="quick-card" to={`${base}/configuracoes`}>
          <span className="material-symbols-outlined">link</span>
          <div>
            <strong>5. Mande o link do cardápio</strong>
            <small>{link}</small>
          </div>
        </Link>
      </div>
    </section>
  );
}
