import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { dinheiro } from '../aki/formatos';
import { semearPedidosDeExemplo, usePedidos, type Pedido } from '../aki/pedidos';
import { JanelaDoTicket } from '../aki/Ticket';
import { toast } from '../aki/toast';
import { CascaDaLoja } from './CascaDaLoja';
import { caminhoDaLoja } from './caminhos';
import { useLojaAtual } from './lojaAtual';

/**
 * O painel administrativo do protótipo (`administrador.html`).
 *
 * Pedidos, vendas, pagamentos e WhatsApp ainda não têm rota no back-end
 * (marcos 3, 4 e 7): os números vêm dos pedidos de demonstração desta aba,
 * como no protótipo.
 */
const HORARIOS = ['08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h'];
const FORMAS = ['Pix', 'Cartão de crédito', 'Cartão de débito', 'Dinheiro', 'Vale-refeição'];
const MENSAGEM_POR_STATUS: Record<string, string> = {
  recebido: 'Seu pedido foi recebido pela AKI Delivery. Já estamos cuidando dele! 🍔',
  preparo: 'Seu pedido está em preparo. Em breve estará a caminho! 👨‍🍳',
  entrega: 'Seu pedido saiu para entrega. O entregador está a caminho! 🛵',
  entregue: 'Seu pedido foi entregue. Obrigado por pedir com a AKI Delivery! ❤️',
};
const MODELOS: Record<string, string> = {
  recebido: 'Olá! Aqui é da AKI Delivery. Recebemos seu pedido e já estamos cuidando dele. 🍔',
  preparo: 'Olá! Seu pedido está em preparo. Em breve estará a caminho! 👨‍🍳',
  entrega: 'Olá! Seu pedido saiu para entrega. O entregador está a caminho! 🛵',
  entregue: 'Olá! Seu pedido foi entregue. Obrigado por pedir com a AKI Delivery! ❤️',
};

const doDia = (pedido: Pedido) =>
  new Date(pedido.date).toDateString() === new Date().toDateString();

function chaveDoStatus(status: string): string {
  const texto = status.toLowerCase();
  if (texto.includes('preparo')) return 'preparo';
  if (texto.includes('entrega')) return 'entrega';
  if (texto.includes('entreg') || texto.includes('conclu')) return 'entregue';
  return 'recebido';
}

export function DashboardAdmin() {
  const { estabelecimentoId } = useLojaAtual();
  useEffect(semearPedidosDeExemplo, []);
  const pedidos = usePedidos();
  const [ticket, definirTicket] = useState<Pedido | null>(null);
  const [pedidoDoWhats, definirPedidoDoWhats] = useState('');
  const [statusDoWhats, definirStatusDoWhats] = useState('recebido');

  const deHoje = pedidos.filter(doDia);
  const vendasDeHoje = deHoje.reduce((soma, pedido) => soma + pedido.total, 0);
  const porHorario = HORARIOS.map((_, i) =>
    deHoje.reduce((soma, pedido) => {
      const hora = new Date(pedido.date).getHours();
      return hora >= 8 + i * 2 && hora < 10 + i * 2 ? soma + pedido.total : soma;
    }, 0),
  );
  const maiorHorario = Math.max(...porHorario, 1);
  const porForma = new Map<string, number>(FORMAS.map((forma) => [forma, 0]));
  for (const pedido of deHoje) {
    const forma = pedido.paymentMethod ?? 'Pix';
    porForma.set(forma, (porForma.get(forma) ?? 0) + pedido.total);
  }
  const maiorForma = Math.max(...porForma.values(), 1);
  const totalDosPedidos = pedidos.reduce((soma, pedido) => soma + pedido.total, 0);

  function abrirWhatsApp() {
    const pedido = pedidos.find((um) => um.number === pedidoDoWhats);
    if (pedido === undefined) {
      toast('Selecione um pedido.');
      return;
    }
    const texto = `Olá! Aqui é da AKI Delivery. Pedido #${pedido.number} — ${MENSAGEM_POR_STATUS[statusDoWhats] ?? ''} Total: ${dinheiro(pedido.total)}.`;
    window.open(`https://wa.me/?text=${encodeURIComponent(texto)}`, '_blank', 'noopener');
  }

  const base = caminhoDaLoja(estabelecimentoId);

  return (
    <CascaDaLoja
      atual="dashboard"
      titulo="Painel administrativo"
      subtitulo="Visão geral da operação do AKI Delivery."
    >
      <section className="stats">
        <div className="stat">
          <div className="stat-top">
            <span>Pedidos hoje</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">receipt_long</span>
            </div>
          </div>
          <strong id="todayOrders">{deHoje.length}</strong>
          <small>Pedidos registrados hoje</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Em preparo</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">restaurant</span>
            </div>
          </div>
          <strong>8</strong>
          <small>Pedidos em produção</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Em entrega</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">two_wheeler</span>
            </div>
          </div>
          <strong>6</strong>
          <small>Entregadores ativos</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Vendas hoje</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">payments</span>
            </div>
          </div>
          <strong id="todaySales">{dinheiro(vendasDeHoje)}</strong>
          <small>Atualizado pelo app</small>
        </div>
      </section>

      <div className="dashboard-grid admin-dashboard-grid">
        <section className="panel admin-chart-panel">
          <div className="table-title">
            <div>
              <h2>Vendas do dia</h2>
              <p className="admin-muted">Acompanhe a evolução das vendas por horário.</p>
            </div>
            <span className="status orange">Hoje</span>
          </div>
          <div className="sales-chart" id="salesChart" aria-label="Gráfico de vendas do dia">
            {porHorario.map((valor, i) => (
              <div
                className="sales-bar-wrap"
                key={HORARIOS[i]}
                title={`${HORARIOS[i]} — ${dinheiro(valor)}`}
              >
                <div className="sales-value">{valor > 0 ? dinheiro(valor) : ''}</div>
                <div
                  className="sales-bar"
                  style={{ height: `${Math.max(8, (valor / maiorHorario) * 180)}px` }}
                />
                <small>{HORARIOS[i]}</small>
              </div>
            ))}
          </div>
          <div className="chart-axis">
            {HORARIOS.map((hora) => (
              <span key={hora}>{hora}</span>
            ))}
          </div>
        </section>
        <section className="panel payment-panel" id="pagamentos">
          <div className="table-title">
            <div>
              <h2>Formas de pagamento</h2>
              <p className="admin-muted">Distribuição dos pedidos pagos.</p>
            </div>
            <span className="material-symbols-outlined section-icon">account_balance_wallet</span>
          </div>
          <div className="payment-list" id="paymentList">
            {[...porForma.entries()].map(([forma, valor]) => (
              <div className="payment-row" key={forma}>
                <div>
                  <span>{forma}</span>
                  <strong>{dinheiro(valor)}</strong>
                </div>
                <div className="payment-track">
                  <i style={{ width: `${(valor / maiorForma) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section className="panel whatsapp-panel" id="whatsapp">
        <div className="table-title">
          <div>
            <h2>
              <span className="material-symbols-outlined inline-icon">chat</span> WhatsApp integrado
            </h2>
            <p className="admin-muted">
              Envie atualizações do pedido diretamente pelo WhatsApp do cliente.
            </p>
          </div>
          <span className="whatsapp-connected">
            <span className="material-symbols-outlined">check_circle</span> Integrado
          </span>
        </div>
        <div className="whatsapp-grid">
          <div className="whatsapp-info">
            <div className="whatsapp-icon">
              <span className="material-symbols-outlined">chat</span>
            </div>
            <div>
              <strong>Central de mensagens</strong>
              <p>
                Selecione um pedido para montar uma mensagem automática com número, status e total.
              </p>
            </div>
          </div>
          <div className="whatsapp-actions">
            <select
              id="whatsappOrder"
              value={pedidoDoWhats}
              onChange={(evento) => definirPedidoDoWhats(evento.target.value)}
            >
              <option value="">Selecione um pedido</option>
              {[...pedidos].reverse().map((pedido) => (
                <option key={pedido.number} value={pedido.number}>
                  #{pedido.number} · {pedido.customer} · {dinheiro(pedido.total)}
                </option>
              ))}
            </select>
            <select
              id="whatsappStatus"
              value={statusDoWhats}
              onChange={(evento) => definirStatusDoWhats(evento.target.value)}
            >
              <option value="recebido">Pedido recebido</option>
              <option value="preparo">Em preparo</option>
              <option value="entrega">Saiu para entrega</option>
              <option value="entregue">Pedido entregue</option>
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
              <span className="material-symbols-outlined inline-icon">forum</span> Mensagens no
              aplicativo
            </h2>
            <p className="admin-muted">
              Consulte e expanda as mensagens preparadas para cada pedido.
            </p>
          </div>
          <span className="status green">Central ativa</span>
        </div>
        <div id="whatsappMessages" className="whatsapp-messages-list">
          {pedidos.length === 0 ? (
            <div className="empty-menu">
              <span className="material-symbols-outlined">forum</span>
              <strong>Nenhuma mensagem disponível</strong>
              <p>As mensagens dos pedidos aparecerão aqui.</p>
            </div>
          ) : (
            pedidos.slice(0, 8).map((pedido, i) => (
              <details className="whatsapp-message" key={pedido.number} open={i === 0}>
                <summary>
                  <span className="message-avatar">
                    <span className="material-symbols-outlined">chat</span>
                  </span>
                  <span className="message-summary">
                    <strong>{pedido.customer}</strong>
                    <small>
                      Pedido #{pedido.number} · {pedido.status}
                    </small>
                  </span>
                  <span className="material-symbols-outlined expand-icon">expand_more</span>
                </summary>
                <div className="message-body">
                  <p>
                    {MODELOS[chaveDoStatus(pedido.status)]} Pedido #{pedido.number}. Total:{' '}
                    {dinheiro(pedido.total)}.
                  </p>
                  <div className="message-meta">
                    <span>
                      <span className="material-symbols-outlined">payments</span>
                      {pedido.paymentMethod ?? 'Pix'}
                    </span>
                    <span>{dinheiro(pedido.total)}</span>
                  </div>
                </div>
              </details>
            ))
          )}
        </div>
      </section>

      <section className="panel order-total-card">
        <div className="stat-top">
          <span>Ticket dos pedidos</span>
          <div className="stat-icon">
            <span className="material-symbols-outlined">receipt_long</span>
          </div>
        </div>
        <strong data-orders-total="">{dinheiro(totalDosPedidos)}</strong>
        <small>
          <b data-orders-count="">{pedidos.length > 99 ? '99+' : pedidos.length}</b> pedidos
          registrados no app
        </small>
      </section>

      <div className="dashboard-grid" id="pedidos">
        <section className="panel table-panel">
          <div className="table-title">
            <h2>Pedidos recentes</h2>
            <span className="status orange">Atualizado pelo app</span>
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
                {pedidos.slice(0, 8).map((pedido) => (
                  <tr key={pedido.number}>
                    <td>
                      <strong>#{pedido.number}</strong>
                    </td>
                    <td>{pedido.customer}</td>
                    <td>{dinheiro(pedido.total)}</td>
                    <td>
                      <span className={`status ${pedido.statusClass || 'orange'}`}>
                        {pedido.status}
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
                ))}
              </tbody>
            </table>
          </div>
        </section>
        <section className="panel quick-panel">
          <div className="table-title">
            <h2>Acesso rápido</h2>
          </div>
          <div className="quick-list">
            <Link className="quick-card" to={`${base}/produtos`}>
              <span className="material-symbols-outlined">restaurant_menu</span>
              <div>
                <strong>Gerenciar catálogo</strong>
                <small>Produtos, preços e categorias</small>
              </div>
            </Link>
            <Link className="quick-card" to={`${base}/disponibilidade`} id="estoque">
              <span className="material-symbols-outlined">inventory_2</span>
              <div>
                <strong>Controle de estoque</strong>
                <small>Entradas, saídas e alertas</small>
              </div>
            </Link>
            <Link className="quick-card" to={`${base}#whatsapp`}>
              <span className="material-symbols-outlined">chat</span>
              <div>
                <strong>WhatsApp</strong>
                <small>Mensagens e atualizações</small>
              </div>
            </Link>
            <Link className="quick-card" to={`${base}#configuracoes`}>
              <span className="material-symbols-outlined">settings</span>
              <div>
                <strong>Configurações</strong>
                <small>Empresa, horários e pagamentos</small>
              </div>
            </Link>
          </div>
        </section>
      </div>

      <section className="panel admin-settings-panel" id="configuracoes">
        <div className="table-title">
          <div>
            <h2>Configurações de recebimento</h2>
            <p className="admin-muted">Defina as formas aceitas pelo estabelecimento.</p>
          </div>
        </div>
        <div className="payment-method-config">
          {[
            ['Pix', 'qr_code_2'],
            ['Cartão de crédito', 'credit_card'],
            ['Cartão de débito', 'credit_card'],
            ['Dinheiro', 'payments'],
            ['Vale-refeição', 'confirmation_number'],
          ].map(([forma, icone]) => (
            <label key={forma}>
              <input type="checkbox" defaultChecked data-payment={forma} />
              <span className="material-symbols-outlined">{icone}</span>
              <b>{forma}</b>
            </label>
          ))}
        </div>
      </section>
      <footer className="footer">
        <b>Aki Delivery</b> © 2026 — Painel administrativo
      </footer>

      <JanelaDoTicket
        pedido={ticket}
        aoFechar={() => definirTicket(null)}
        linkDoCardapio={`${base}/cardapio`}
      />
    </CascaDaLoja>
  );
}
