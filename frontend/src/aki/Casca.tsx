import { useEffect, useState, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { useSessao } from '../auth/useSessao';
import { emDemonstracao, sairDaDemonstracao } from './demo';
import { esquecerCarrinho } from './pedidos';
import { alternarTema } from './tema';
import { toast } from './toast';

/**
 * A moldura das telas internas do protótipo: menu lateral escuro, barra do topo
 * e a área da página (`.app-shell`, `.sidebar`, `.main`, `.topbar`, `.page`).
 * A marcação é a mesma do `aki_delivery`, classe por classe.
 */

export interface ItemDoMenu {
  readonly chave: string;
  readonly rotulo: string;
  readonly icone: string;
  /** Caminho do app; com `#ancora` rola até a seção, como os links do protótipo. */
  readonly para: string;
  readonly selo?: ReactNode;
}

export function Casca({
  menu,
  atual,
  rodape,
  titulo,
  subtitulo,
  acoes,
  rotuloDoMenu = 'Menu principal',
  children,
}: {
  menu: readonly ItemDoMenu[];
  atual: string;
  rodape: ReactNode;
  titulo: string;
  subtitulo: string;
  acoes: ReactNode;
  rotuloDoMenu?: string;
  children: ReactNode;
}) {
  const [aberto, definirAberto] = useState(false);
  const { autenticado, sair } = useSessao();
  const navegar = useNavigate();
  const demo = emDemonstracao();
  useRolarParaAncora();
  const { pathname } = useLocation();

  // No celular, o menu fecha ao trocar de tela, com o Esc e tocando fora dele (13/11/2026).
  useEffect(() => {
    definirAberto(false);
  }, [pathname]);
  useEffect(() => {
    if (!aberto) return;
    const tecla = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') definirAberto(false);
    };
    document.addEventListener('keydown', tecla);
    return () => document.removeEventListener('keydown', tecla);
  }, [aberto]);

  // Como no protótipo: cada tela avisa que é demonstração.
  useEffect(() => {
    if (emDemonstracao()) toast('Modo demonstração: dados de exemplo, sem servidor.');
  }, [pathname]);

  function sairDaConta() {
    sairDaDemonstracao();
    esquecerCarrinho();
    sair();
    void navegar('/entrar', { replace: true });
  }

  return (
    <div className="app-shell">
      {aberto && (
        <button
          type="button"
          className="sidebar-fundo"
          aria-label="Fechar menu"
          onClick={() => definirAberto(false)}
        />
      )}
      <aside className={`sidebar${aberto ? ' open' : ''}`} id="sidebar">
        <button
          type="button"
          className="icon-btn sidebar-fechar"
          aria-label="Fechar menu e voltar à tela"
          onClick={() => definirAberto(false)}
        >
          <span className="material-symbols-outlined">close</span>
        </button>
        <div className="brand">
          <div className="brand-mark">
            <span className="material-symbols-outlined">lunch_dining</span>
          </div>
          <div className="brand-text">
            Aki<small>DELIVERY</small>
          </div>
        </div>
        <nav className="nav" aria-label={rotuloDoMenu}>
          {menu.map((item) => (
            <Link
              key={item.chave}
              className={item.chave === atual ? 'active' : undefined}
              to={item.para}
              onClick={() => definirAberto(false)}
            >
              <span className="material-symbols-outlined">{item.icone}</span>
              <span>{item.rotulo}</span>
              {item.selo}
            </Link>
          ))}
          {(demo || autenticado) && (
            <a
              href="/entrar"
              data-sair=""
              onClick={(evento) => {
                evento.preventDefault();
                sairDaConta();
              }}
            >
              <span className="material-symbols-outlined">logout</span>
              <span>{demo ? 'Sair da demonstração' : 'Sair'}</span>
            </a>
          )}
        </nav>
        <div className="sidebar-footer">
          <div className="theme-row">
            <span className="material-symbols-outlined">light_mode</span>Modo escuro
            <button
              className="switch"
              id="themeToggle"
              type="button"
              aria-label="Alternar tema"
              onClick={() => alternarTema()}
            >
              <i />
            </button>
          </div>
          {rodape}
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="topbar-title">
            <button
              id="menuBtn"
              className="icon-btn mobile-menu"
              type="button"
              aria-label="Abrir menu"
              aria-expanded={aberto}
              aria-controls="sidebar"
              onClick={() => definirAberto((valor) => !valor)}
            >
              <span className="material-symbols-outlined">menu</span>
            </button>
            <div className="topbar-texto">
              <h1>{titulo}</h1>
              <p>{subtitulo}</p>
            </div>
          </div>
          <div className="top-actions">{acoes}</div>
        </header>
        <div className="page">{children}</div>
      </main>
    </div>
  );
}

/** Os links `#ancora` do protótipo: ao chegar com hash, rola até a seção. */
function useRolarParaAncora(): void {
  const { hash, pathname } = useLocation();
  useEffect(() => {
    if (hash === '') {
      window.scrollTo({ top: 0 });
      return;
    }
    const alvo = document.getElementById(decodeURIComponent(hash.slice(1)));
    alvo?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, [hash, pathname]);
}
