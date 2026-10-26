import { Casca } from '../aki/Casca';
import { usePedidos } from '../aki/pedidos';
import { toast } from '../aki/toast';

/**
 * A área do entregador do protótipo (`entregador.html`). Entregas, jornada e
 * ganhos ainda não existem no back-end (marcos 5 e 6): tudo aqui é demonstração.
 */
export function EntregadorPage() {
  const pedido = usePedidos()[0];
  let pagamento = 'Nenhum pedido disponível.';
  if (pedido !== undefined) {
    const forma = pedido.paymentMethod ?? 'Pix';
    pagamento = forma === 'Dinheiro' ? 'Pagamento em dinheiro na entrega' : `Pagamento: ${forma}`;
    if (pedido.cashChange) {
      pagamento += ` · Troco para R$ ${Number(pedido.cashChange).toFixed(2).replace('.', ',')}`;
    }
  }

  return (
    <Casca
      menu={[
        { chave: 'entregas', rotulo: 'Entregas', icone: 'two_wheeler', para: '/entregador' },
        { chave: 'atual', rotulo: 'Entrega atual', icone: 'near_me', para: '/entregador#atual' },
        {
          chave: 'historico',
          rotulo: 'Histórico',
          icone: 'history',
          para: '/entregador#historico',
        },
        { chave: 'ganhos', rotulo: 'Ganhos', icone: 'payments', para: '/entregador#ganhos' },
        { chave: 'perfil', rotulo: 'Meu perfil', icone: 'person', para: '/entregador#perfil' },
      ]}
      atual="entregas"
      titulo="Olá, entregador!"
      subtitulo="Confira suas entregas e ganhos."
      rodape={
        <>
          <div className="online">
            <i /> Online
          </div>
          <span>Pronto para novas entregas</span>
        </>
      }
      acoes={
        <>
          <button className="icon-btn notification" type="button" aria-label="Notificações">
            <span className="material-symbols-outlined">notifications</span>
            <b>1</b>
          </button>
          <div className="avatar">ED</div>
          <span className="role">Entregador</span>
        </>
      }
    >
      <section className="panel driver-card" id="atual">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <span className="eyebrow" style={{ color: 'var(--orange)' }}>
              ENTREGA ATUAL
            </span>
            <h2 style={{ margin: '7px 0', fontSize: '23px' }}>
              Pedido #{pedido?.number ?? '123456'}
            </h2>
            <p style={{ fontSize: '11px', color: 'var(--muted)', margin: 0 }}>
              Restaurante → Cliente • 15 min estimados
            </p>
            <div className="driver-payment-info" id="driverPaymentInfo">
              <span className="material-symbols-outlined">payments</span>
              <span>{pagamento}</span>
            </div>
          </div>
          <span className="status orange">Em coleta</span>
        </div>
        <div className="map" style={{ marginTop: '18px' }}>
          <div className="route" />
          <div className="marker store">
            <span className="material-symbols-outlined">restaurant</span>
          </div>
          <div className="marker driver">
            <span className="material-symbols-outlined">two_wheeler</span>
          </div>
          <div className="marker home">
            <span className="material-symbols-outlined">home</span>
          </div>
          <span className="map-label label-store">AKI Lanches</span>
          <span className="map-label label-home">Cliente</span>
        </div>
        <div className="delivery-actions" style={{ marginTop: '13px' }}>
          <button
            className="btn btn-primary"
            type="button"
            onClick={() => toast('Status atualizado para Em entrega.')}
          >
            <span className="material-symbols-outlined">two_wheeler</span>Iniciar entrega
          </button>
          <button
            className="btn"
            type="button"
            style={{ background: 'var(--orange-soft)', color: 'var(--orange-dark)' }}
            onClick={() => toast('Cliente será avisado pelo sistema.')}
          >
            <span className="material-symbols-outlined">call</span>Contato
          </button>
        </div>
      </section>

      <div className="driver-grid">
        <section className="panel driver-card" id="historico">
          <div className="table-title">
            <h2>Resumo de hoje</h2>
            <span className="online">
              <i /> Online
            </span>
          </div>
          <div className="earnings">
            <div className="earning-box">
              <small>Entregas</small>
              <strong>12</strong>
            </div>
            <div className="earning-box">
              <small>Ganhos</small>
              <strong>R$ 186,40</strong>
            </div>
            <div className="earning-box">
              <small>Distância</small>
              <strong>34,8 km</strong>
            </div>
            <div className="earning-box">
              <small>Avaliação</small>
              <strong>4,9 ★</strong>
            </div>
          </div>
          <h2 style={{ fontSize: '14px', margin: '22px 0 10px' }}>Últimas entregas</h2>
          <div className="delivery-card">
            <div className="delivery-top">
              <strong>#123455</strong>
              <span className="status green">Concluída</span>
            </div>
            <p>
              Centro → Jardim das Flores
              <br />
              Valor da entrega: <b>R$ 15,50</b>
            </p>
          </div>
          <div className="delivery-card">
            <div className="delivery-top">
              <strong>#123454</strong>
              <span className="status green">Concluída</span>
            </div>
            <p>
              Vila Nova → Centro
              <br />
              Valor da entrega: <b>R$ 14,90</b>
            </p>
          </div>
        </section>
        <section className="panel driver-card" id="ganhos">
          <div className="table-title">
            <h2>Ganhos</h2>
          </div>
          <div className="earning-box">
            <small>Ganhos desta semana</small>
            <strong>R$ 842,60</strong>
            <p style={{ fontSize: '10px', color: 'var(--muted)' }}>
              +12% em relação à semana anterior
            </p>
          </div>
          <div style={{ marginTop: '18px' }}>
            <div className="summary-row">
              <span>Segunda</span>
              <b>R$ 118,20</b>
            </div>
            <div className="summary-row">
              <span>Terça</span>
              <b>R$ 156,40</b>
            </div>
            <div className="summary-row">
              <span>Quarta</span>
              <b>R$ 186,40</b>
            </div>
            <div className="summary-row">
              <span>Quinta</span>
              <b>R$ 142,80</b>
            </div>
            <div className="summary-row">
              <span>Outros</span>
              <b>R$ 238,80</b>
            </div>
          </div>
        </section>
      </div>
      <section className="panel driver-card" id="perfil" style={{ marginTop: '15px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div className="avatar" style={{ width: '55px', height: '55px' }}>
            ED
          </div>
          <div>
            <h2 style={{ fontSize: '15px', margin: 0 }}>Entregador AKI</h2>
            <p style={{ fontSize: '10px', color: 'var(--muted)', margin: '5px 0' }}>
              Perfil verificado • Veículo cadastrado
            </p>
          </div>
        </div>
      </section>
      <footer className="footer">
        <b>Aki Delivery</b> © 2026 — Área do entregador
      </footer>
    </Casca>
  );
}
