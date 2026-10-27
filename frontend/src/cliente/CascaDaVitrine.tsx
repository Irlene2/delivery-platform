import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { Casca } from '../aki/Casca';
import { quantidadeNoCarrinho, useCarrinho } from '../aki/pedidos';

/**
 * A moldura de quem compra pelo link da loja (ADR-077): a mesma `Casca` do protótipo, com o
 * menu do cliente reduzido ao que funciona sem conta — cardápio, carrinho e os pedidos feitos
 * neste aparelho.
 */
export function CascaDaVitrine({
  estabelecimentoId,
  atual,
  titulo,
  subtitulo,
  children,
}: {
  estabelecimentoId: string;
  atual: 'cardapio' | 'pedidos';
  titulo: string;
  subtitulo: string;
  children: ReactNode;
}) {
  const carrinho = useCarrinho();
  const noCarrinho = quantidadeNoCarrinho(carrinho);
  const base = `/loja/${estabelecimentoId}`;

  return (
    <Casca
      menu={[
        { chave: 'cardapio', rotulo: 'Cardápio', icone: 'restaurant_menu', para: base },
        {
          chave: 'carrinho',
          rotulo: 'Carrinho',
          icone: 'shopping_cart',
          para: `${base}#carrinho`,
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
          para: `${base}/pedidos`,
        },
      ]}
      atual={atual}
      titulo={titulo}
      subtitulo={subtitulo}
      rodape={
        <>
          Peça sem cadastro
          <br />
          <b>AKI Delivery</b>
        </>
      }
      acoes={
        <>
          <Link className="icon-btn notification" to={`${base}#carrinho`} aria-label="Carrinho">
            <span className="material-symbols-outlined">shopping_cart</span>
            <b data-cart-count="">{noCarrinho}</b>
          </Link>
          <div className="avatar">CL</div>
          <span className="role">Cliente</span>
        </>
      }
    >
      {children}
    </Casca>
  );
}
