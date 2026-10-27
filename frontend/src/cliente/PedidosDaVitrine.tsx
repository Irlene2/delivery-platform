import { Link, useParams } from 'react-router';
import { dataEHora, dinheiro } from '../aki/formatos';
import { usePedidosLembrados } from '../aki/pedidos';
import { CascaDaVitrine } from './CascaDaVitrine';

/**
 * `/loja/:estabelecimentoId/pedidos` — os pedidos feitos nesta aba, nesta loja. Sem conta de
 * cliente, quem lembra é o aparelho: o código de acompanhamento de cada um.
 */
export function PedidosDaVitrine() {
  const { estabelecimentoId = '' } = useParams();
  const meus = usePedidosLembrados().filter((um) => um.estabelecimentoId === estabelecimentoId);

  return (
    <CascaDaVitrine
      estabelecimentoId={estabelecimentoId}
      atual="pedidos"
      titulo="Meus Pedidos"
      subtitulo="Os pedidos feitos neste aparelho."
    >
      <section className="orders-hero">
        <div>
          <span className="eyebrow">SEUS PEDIDOS</span>
          <h2>Acompanhe cada pedido.</h2>
          <p>Toque em um pedido para ver em que etapa ele está.</p>
        </div>
        <Link to={`/loja/${estabelecimentoId}`} className="btn btn-primary">
          <span className="material-symbols-outlined">add_shopping_cart</span>Novo pedido
        </Link>
      </section>

      <section className="panel orders-history">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">history</span>
          </div>
          <div>
            <small>HISTÓRICO</small>
            <h2>Meus pedidos</h2>
            <p>Guardados neste navegador enquanto a aba estiver aberta.</p>
          </div>
        </div>
        {meus.length === 0 ? (
          <div className="ticket-empty">
            <span className="material-symbols-outlined">receipt_long</span>
            <strong>Nenhum pedido por aqui</strong>
            <p>Depois do primeiro pedido, ele aparece nesta lista.</p>
            <Link className="btn btn-primary" to={`/loja/${estabelecimentoId}`}>
              Ver cardápio
            </Link>
          </div>
        ) : (
          <div className="orders-list">
            {meus.map((pedido) => (
              <article className="order-history-card" key={pedido.codigo}>
                <div className="order-history-main">
                  <div className="order-number">
                    <span className="material-symbols-outlined">receipt_long</span>
                    <div>
                      <strong>#{pedido.numero}</strong>
                      <small>{dataEHora(pedido.criadoEm)}</small>
                    </div>
                  </div>
                </div>
                <div className="order-history-bottom">
                  <strong>{dinheiro(pedido.total)}</strong>
                  <Link className="ticket-btn" to={`/pedido/${pedido.codigo}`}>
                    <span className="material-symbols-outlined">route</span> Acompanhar
                  </Link>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Seus pedidos, sempre por perto.
      </footer>
    </CascaDaVitrine>
  );
}
