import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { Casca } from '../aki/Casca';
import { quantidadeNoCarrinho, useCarrinho, usePedidos } from '../aki/pedidos';

/** A moldura da área do cliente do protótipo (`cliente.html`, `cardapio.html`, `pedidos.html`). */
export function CascaDoCliente({
  atual,
  titulo,
  subtitulo,
  icone,
  children,
}: {
  atual: 'inicio' | 'cardapio' | 'pedidos';
  titulo: string;
  subtitulo: string;
  /** O atalho do topo: pedidos (sino) na área do cliente, carrinho nas outras. */
  icone: 'notificacoes' | 'carrinho';
  children: ReactNode;
}) {
  const carrinho = useCarrinho();
  const pedidos = usePedidos();
  const noCarrinho = quantidadeNoCarrinho(carrinho);

  return (
    <Casca
      menu={[
        { chave: 'inicio', rotulo: 'Início', icone: 'home', para: '/cliente' },
        {
          chave: 'cardapio',
          rotulo: 'Cardápio',
          icone: 'restaurant_menu',
          para: '/cliente/cardapio',
        },
        {
          chave: 'carrinho',
          rotulo: 'Carrinho',
          icone: 'shopping_cart',
          para: '/cliente#carrinho',
          selo: (
            <b className="nav-badge" data-cart-count="">
              {noCarrinho}
            </b>
          ),
        },
        {
          chave: 'pedidos',
          rotulo: 'Meus Pedidos',
          icone: 'receipt_long',
          para: '/cliente/pedidos',
        },
        { chave: 'conta', rotulo: 'Minha Conta', icone: 'person', para: '/cliente#conta' },
      ]}
      atual={atual}
      titulo={titulo}
      subtitulo={subtitulo}
      rodape={
        <>
          Atendimento
          <br />
          <b>(21) 99999-0000</b>
        </>
      }
      acoes={
        <>
          {icone === 'notificacoes' ? (
            <Link className="icon-btn notification" to="/cliente/pedidos" aria-label="Meus pedidos">
              <span className="material-symbols-outlined">notifications</span>
              <b data-orders-count="">{pedidos.length > 99 ? '99+' : pedidos.length}</b>
            </Link>
          ) : (
            <Link className="icon-btn notification" to="/cliente#carrinho" aria-label="Carrinho">
              <span className="material-symbols-outlined">shopping_cart</span>
              <b data-cart-count="">{noCarrinho}</b>
            </Link>
          )}
          <div className="avatar">CL</div>
          <span className="role">Cliente</span>
        </>
      }
    >
      {children}
    </Casca>
  );
}
