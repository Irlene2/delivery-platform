import { useEffect } from 'react';
import { Link } from 'react-router';
import { dataEHora, dinheiro } from './formatos';
import type { Pedido } from './pedidos';

/** O comprovante do pedido, com a marcação do `ticketMarkup` do protótipo. */
export function Ticket({
  pedido,
  linkDoCardapio,
}: {
  pedido: Pedido | undefined;
  linkDoCardapio: string;
}) {
  if (pedido === undefined) {
    return (
      <div className="ticket-empty">
        <span className="material-symbols-outlined">receipt_long</span>
        <strong>Nenhum pedido realizado ainda</strong>
        <p>Adicione produtos ao carrinho e finalize seu primeiro pedido.</p>
        <Link className="btn btn-primary" to={linkDoCardapio}>
          Ir para o cardápio
        </Link>
      </div>
    );
  }
  return (
    <div className="ticket">
      <div className="ticket-top">
        <div className="ticket-brand">
          <span className="material-symbols-outlined">lunch_dining</span>
          <strong>AKI DELIVERY</strong>
        </div>
        <span className={`ticket-status ${pedido.statusClass || 'orange'}`}>
          {pedido.status || 'Pedido recebido'}
        </span>
      </div>
      <div className="ticket-head">
        <div>
          <small>NÚMERO DO PEDIDO</small>
          <strong>#{pedido.number}</strong>
        </div>
        <div>
          <small>DATA E HORA</small>
          <strong>{dataEHora(pedido.date)}</strong>
        </div>
      </div>
      <div className="ticket-items">
        {pedido.items.map((item) => (
          <div className="ticket-item" key={item.id}>
            <div>
              <b>{item.qty}x</b> {item.name}
            </div>
            <strong>{dinheiro(item.price * item.qty)}</strong>
          </div>
        ))}
      </div>
      <div className="ticket-payment">
        <span className="material-symbols-outlined">payments</span>
        <span>
          Pagamento: <strong>{pedido.paymentMethod ?? 'Pix'}</strong>
          {pedido.paymentMethod === 'Dinheiro' && pedido.cashChange
            ? ` · Troco para ${dinheiro(pedido.cashChange)}`
            : ''}
        </span>
      </div>
      <div className="ticket-totals">
        <div>
          <span>Subtotal</span>
          <strong>{dinheiro(pedido.subtotal)}</strong>
        </div>
        <div>
          <span>Taxa de entrega</span>
          <strong>{dinheiro(pedido.fee)}</strong>
        </div>
        <div className="ticket-total">
          <span>Total do pedido</span>
          <strong>{dinheiro(pedido.total)}</strong>
        </div>
      </div>
      <div className="ticket-footer">
        <span className="material-symbols-outlined">verified</span> Obrigado por pedir no AKI
        Delivery!
      </div>
    </div>
  );
}

/** A janela do ticket (`#ticketModal` do protótipo). */
export function JanelaDoTicket({
  pedido,
  aoFechar,
  linkDoCardapio,
}: {
  pedido: Pedido | null;
  aoFechar: () => void;
  linkDoCardapio: string;
}) {
  useEffect(() => {
    document.body.classList.toggle('modal-open', pedido !== null);
    if (pedido === null) return;
    const tecla = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') aoFechar();
    };
    document.addEventListener('keydown', tecla);
    return () => {
      document.removeEventListener('keydown', tecla);
      document.body.classList.remove('modal-open');
    };
  }, [pedido, aoFechar]);

  return (
    <div className={`ticket-modal${pedido !== null ? ' show' : ''}`}>
      <div className="ticket-modal-backdrop" onClick={aoFechar} />
      <div className="ticket-modal-card">
        <button className="ticket-close" onClick={aoFechar} aria-label="Fechar" type="button">
          <span className="material-symbols-outlined">close</span>
        </button>
        <div>{pedido !== null && <Ticket pedido={pedido} linkDoCardapio={linkDoCardapio} />}</div>
      </div>
    </div>
  );
}
