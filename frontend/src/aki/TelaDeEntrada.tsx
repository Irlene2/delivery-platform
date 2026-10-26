import { useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { useEstiloDoLogin } from './estiloDoLogin';
import { alternarTema, temaAtual } from './tema';

/**
 * A página de entrada do protótipo (`aki_delivery/index.html`): a apresentação à
 * esquerda e o cartão à direita. Entrar, criar conta e o convite usam a mesma
 * moldura; muda só o cartão.
 */
export function TelaDeEntrada({ children }: { children: ReactNode }) {
  useEstiloDoLogin();
  const [tema, definirTema] = useState(temaAtual);

  return (
    <>
      <header className="topbar">
        <Link to="/entrar" className="logo" aria-label="AKI Delivery">
          <span className="login-brand-mark">
            <span className="material-symbols-outlined">lunch_dining</span>
          </span>
          <span className="login-brand-text">
            Aki<small>DELIVERY</small>
          </span>
        </Link>
        <button
          className="theme-toggle"
          data-theme-toggle=""
          aria-label="Alternar tema"
          type="button"
          onClick={() => definirTema(alternarTema())}
        >
          <span className="material-icons">{tema === 'escuro' ? 'light_mode' : 'dark_mode'}</span>
        </button>
      </header>

      <main className="landing">
        <section className="hero">
          <div className="hero-content">
            <span className="badge">Sistema de pedidos e entregas</span>
            <h1>
              Gerencie seus pedidos <span>de forma simples.</span>
            </h1>
            <p className="hero-text">
              Uma plataforma completa para clientes, administradores e entregadores.
            </p>

            <div className="stats-container">
              <div className="stats-grid">
                <div className="mini-stat">
                  <div className="mini-stat-icon orange-icon">
                    <span className="material-symbols-outlined">inventory_2</span>
                  </div>
                  <div className="mini-stat-info">
                    <strong>24</strong>
                    <small>Pedidos hoje</small>
                  </div>
                </div>
                <div className="mini-stat">
                  <div className="mini-stat-icon green-icon">
                    <span className="material-symbols-outlined">two_wheeler</span>
                  </div>
                  <div className="mini-stat-info">
                    <strong>5</strong>
                    <small>Em entrega</small>
                  </div>
                </div>
                <div className="mini-stat">
                  <div className="mini-stat-icon purple-icon">
                    <span className="material-symbols-outlined">payments</span>
                  </div>
                  <div className="mini-stat-info">
                    <strong>R$ 1.250</strong>
                    <small>Vendas hoje</small>
                  </div>
                </div>
              </div>
            </div>

            <div className="hero-description">
              <div className="description-item">
                <span className="material-icons">check_circle</span>
                <span>Acompanhe seus pedidos em tempo real</span>
              </div>
              <div className="description-item">
                <span className="material-icons">check_circle</span>
                <span>Organize suas entregas</span>
              </div>
              <div className="description-item">
                <span className="material-icons">check_circle</span>
                <span>Tenha controle das suas vendas</span>
              </div>
            </div>
          </div>

          <div className="login-container">
            <div className="login-card">{children}</div>
          </div>
        </section>
      </main>
    </>
  );
}

/** O cabeçalho do cartão (`.login-header`). */
export function CabecalhoDoCartao({
  selo,
  icone,
  titulo,
  texto,
}: {
  selo: string;
  icone: string;
  titulo: string;
  texto: ReactNode;
}) {
  return (
    <div className="login-header">
      <span className="login-badge">
        <span className="material-icons">{icone}</span>
        {selo}
      </span>
      <h2>{titulo}</h2>
      <p>{texto}</p>
    </div>
  );
}

/** O aviso dentro do cartão, no lugar dos `alert()` do protótipo. */
export function AvisoDoCartao({
  children,
  tom = 'aviso',
}: {
  children: ReactNode;
  tom?: 'aviso' | 'erro';
}) {
  return (
    <p
      id="loginAviso"
      role={tom === 'erro' ? 'alert' : 'status'}
      style={{
        margin: '12px 0 0',
        padding: '10px 12px',
        borderRadius: '10px',
        background: tom === 'erro' ? '#fff0ef' : '#fff1e7',
        color: tom === 'erro' ? '#a1332b' : '#8a3d0a',
        fontSize: '13px',
        fontWeight: 600,
      }}
    >
      {children}
    </p>
  );
}
