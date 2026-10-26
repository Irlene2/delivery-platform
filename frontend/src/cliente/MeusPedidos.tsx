import { useState } from 'react';
import { Link } from 'react-router';
import { dataEHora, dinheiro } from '../aki/formatos';
import { usePedidos, type Pedido } from '../aki/pedidos';
import { JanelaDoTicket, Ticket } from '../aki/Ticket';
import { CascaDoCliente } from './CascaDoCliente';

/** Meus pedidos (`pedidos.html`): o último ticket, o histórico desta aba e o atalho do trajeto. */
export function MeusPedidos() {
  const pedidos = usePedidos();
  const [aberto, definirAberto] = useState<Pedido | null>(null);

  return (
    <CascaDoCliente
      atual="pedidos"
      icone="carrinho"
      titulo="Meus Pedidos"
      subtitulo="Histórico, tickets e acompanhamento."
    >
      <section className="orders-hero">
        <div>
          <span className="eyebrow">HISTÓRICO DO CLIENTE</span>
          <h2>Seus pedidos em um só lugar.</h2>
          <p>Consulte o ticket, o valor total e acompanhe a entrega do pedido mais recente.</p>
        </div>
        <Link to="/cliente/cardapio" className="btn btn-primary">
          <span className="material-symbols-outlined">add_shopping_cart</span>Novo pedido
        </Link>
      </section>

      <section className="panel ticket-panel" id="pedido-ticket">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">confirmation_number</span>
          </div>
          <div>
            <small>ÚLTIMO PEDIDO</small>
            <h2>Ticket do pedido</h2>
            <p>Comprovante com itens, taxa e total.</p>
          </div>
        </div>
        <div id="latestTicket">
          <Ticket pedido={pedidos[0]} linkDoCardapio="/cliente/cardapio" />
        </div>
      </section>

      <section className="panel orders-history">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">history</span>
          </div>
          <div>
            <small>HISTÓRICO</small>
            <h2>Meus pedidos</h2>
            <p>Pedidos realizados neste dispositivo.</p>
          </div>
        </div>
        <div id="ordersHistory">
          {pedidos.length === 0 ? (
            <div className="ticket-empty">
              <span className="material-symbols-outlined">receipt_long</span>
              <strong>Nenhum pedido encontrado</strong>
              <p>Seu histórico aparecerá aqui depois do primeiro pedido.</p>
              <Link className="btn btn-primary" to="/cliente/cardapio">
                Ver cardápio
              </Link>
            </div>
          ) : (
            <div className="orders-list">
              {pedidos.map((pedido) => (
                <article className="order-history-card" key={pedido.number}>
                  <div className="order-history-main">
                    <div className="order-number">
                      <span className="material-symbols-outlined">receipt_long</span>
                      <div>
                        <strong>#{pedido.number}</strong>
                        <small>{dataEHora(pedido.date)}</small>
                      </div>
                    </div>
                    <span className={`ticket-status ${pedido.statusClass || 'orange'}`}>
                      {pedido.status}
                    </span>
                  </div>
                  <div className="order-history-items">
                    {pedido.items.map((item) => (
                      <span key={item.id}>
                        {item.qty}x {item.name}
                      </span>
                    ))}
                  </div>
                  <div className="order-history-bottom">
                    <strong>{dinheiro(pedido.total)}</strong>
                    <button
                      className="ticket-btn"
                      type="button"
                      onClick={() => definirAberto(pedido)}
                    >
                      <span className="material-symbols-outlined">confirmation_number</span> Ver
                      ticket
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="panel tracking compact-tracking">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">route</span>
          </div>
          <div>
            <small>ACOMPANHAMENTO</small>
            <h2>Acompanhar entrega</h2>
            <p>Abra a área do cliente para visualizar o trajeto do entregador.</p>
          </div>
        </div>
        <Link className="btn btn-primary" to="/cliente#trajeto">
          <span className="material-symbols-outlined">near_me</span>Acompanhar trajeto
        </Link>
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Seus pedidos, sempre por perto.
      </footer>

      <JanelaDoTicket
        pedido={aberto}
        aoFechar={() => definirAberto(null)}
        linkDoCardapio="/cliente/cardapio"
      />
    </CascaDoCliente>
  );
}
