import { useMemo } from 'react';
import { Link } from 'react-router';
import { dinheiro } from '../aki/formatos';
import { resumoDoDia } from '../pedido/fechamento';
import { classeDoStatus, ROTULO_DO_STATUS } from '../pedido/regras';
import { CascaDaLoja } from './CascaDaLoja';
import { caminhoDaLoja } from './caminhos';
import { useLojaAtual } from './lojaAtual';
import { useFonteDePedidos } from './useFonteDePedidos';
import { usePedidosDoDia } from './usePedidosDoDia';

/**
 * O painel do colaborador do protótipo (`colaborador.html`), com os pedidos de verdade do dia.
 * Sem `VER_PEDIDO`, o servidor recusa a lista, e a tela mostra só os atalhos.
 */
export function DashboardColaborador() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const fonte = useFonteDePedidos(estabelecimentoId);
  const vePedidos = loja?.permissoes.includes('VER_PEDIDO') ?? false;
  const { pedidos: lidos } = usePedidosDoDia(fonte);
  const pedidos = useMemo(() => (vePedidos ? (lidos ?? []) : []), [vePedidos, lidos]);
  const resumo = useMemo(() => resumoDoDia(pedidos), [pedidos]);
  const base = caminhoDaLoja(estabelecimentoId);

  return (
    <CascaDaLoja
      atual="dashboard"
      titulo="Painel do colaborador"
      subtitulo="Acompanhe os pedidos e consulte informações operacionais."
    >
      <section className="hero-banner">
        <div>
          <span className="eyebrow orange-text">ÁREA OPERACIONAL</span>
          <h1>Olá, colaborador!</h1>
          <p>
            Você possui acesso às funções necessárias para apoiar o atendimento e a operação da AKI
            Delivery.
          </p>
        </div>
        <span className="material-symbols-outlined hero-icon">badge</span>
      </section>
      <section className="stats">
        <article className="stat panel">
          <div className="stat-top">
            <span>Pedidos hoje</span>
            <span className="stat-icon">
              <span className="material-symbols-outlined">receipt_long</span>
            </span>
          </div>
          <strong id="collabOrders">{resumo.pedidos}</strong>
          <small>Pedidos recebidos hoje</small>
        </article>
        <article className="stat panel">
          <div className="stat-top">
            <span>Em preparo</span>
            <span className="stat-icon">
              <span className="material-symbols-outlined">soup_kitchen</span>
            </span>
          </div>
          <strong id="collabPrep">{resumo.naCozinha}</strong>
          <small>Acompanhamento operacional</small>
        </article>
        <article className="stat panel">
          <div className="stat-top">
            <span>Em entrega</span>
            <span className="stat-icon">
              <span className="material-symbols-outlined">delivery_dining</span>
            </span>
          </div>
          <strong id="collabDelivery">{resumo.emEntrega}</strong>
          <small>Pedidos em rota</small>
        </article>
        <article className="stat panel">
          <div className="stat-top">
            <span>Acesso</span>
            <span className="stat-icon">
              <span className="material-symbols-outlined">lock</span>
            </span>
          </div>
          <strong style={{ fontSize: '18px' }}>Limitado</strong>
          <small>Sem configurações administrativas</small>
        </article>
      </section>
      <div className="dashboard-grid" id="pedidos">
        <section className="panel table-panel">
          <div className="table-title">
            <div>
              <h2>Pedidos recentes</h2>
              <p className="admin-muted">Visualização para acompanhamento da operação.</p>
            </div>
            <Link className="btn btn-light" to={`${base}/pedidos`}>
              Ver pedidos
            </Link>
          </div>
          <div style={{ overflow: 'auto' }}>
            <table className="orders-table">
              <thead>
                <tr>
                  <th>Pedido</th>
                  <th>Cliente</th>
                  <th>Status</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody id="collabOrdersTable">
                {pedidos.length === 0 ? (
                  <tr>
                    <td colSpan={4}>Nenhum pedido hoje ainda.</td>
                  </tr>
                ) : (
                  pedidos.slice(0, 8).map((pedido) => (
                    <tr key={pedido.id}>
                      <td>
                        <strong>#{pedido.numero}</strong>
                      </td>
                      <td>{pedido.cliente.nome}</td>
                      <td>
                        <span className={`status ${classeDoStatus(pedido.status)}`}>
                          {ROTULO_DO_STATUS[pedido.status]}
                        </span>
                      </td>
                      <td>{dinheiro(pedido.total)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
        <section className="panel quick-panel">
          <div className="table-title">
            <h2>Acessos disponíveis</h2>
          </div>
          <div className="quick-list">
            <Link className="quick-card" to={`${base}/pedidos`}>
              <span className="material-symbols-outlined">receipt_long</span>
              <span>
                <strong>Consultar pedidos</strong>
                <small>Visualizar pedidos e seus status.</small>
              </span>
            </Link>
            <Link className="quick-card" to={`${base}/cardapio`}>
              <span className="material-symbols-outlined">restaurant_menu</span>
              <span>
                <strong>Consultar cardápio</strong>
                <small>Ver produtos e valores cadastrados.</small>
              </span>
            </Link>
            <Link className="quick-card" to={`${base}/disponibilidade`}>
              <span className="material-symbols-outlined">inventory_2</span>
              <span>
                <strong>Consultar estoque</strong>
                <small>Acompanhar disponibilidade dos itens.</small>
              </span>
            </Link>
          </div>
          <div className="support" style={{ marginTop: '15px' }}>
            <div className="support-icon">
              <span className="material-symbols-outlined">info</span>
            </div>
            <div>
              <strong>Permissões do perfil</strong>
              <p>
                O colaborador não possui acesso a pagamentos, configurações, usuários ou gestão
                administrativa completa.
              </p>
            </div>
          </div>
        </section>
      </div>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Painel do colaborador
      </footer>
    </CascaDaLoja>
  );
}
