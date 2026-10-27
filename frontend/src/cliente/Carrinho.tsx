import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { dinheiro } from '../aki/formatos';
import {
  finalizarPedido,
  limparCarrinho,
  mudarQuantidade,
  retirarDoCarrinho,
  totaisDoCarrinho,
  useCarrinho,
} from '../aki/pedidos';
import { toast } from '../aki/toast';
import type { PedidoPublico } from '../api/pedidos';
import { IMAGEM_PADRAO } from '../painel/catalogo';
import type { Vitrine } from '../pedido/fontes';
import { ResumoComCheckout } from './Checkout';

/**
 * A seção do carrinho do protótipo (`#carrinho`), com o resumo e a forma de
 * pagamento.
 *
 * Com `vitrine`, o pedido é **de verdade**: o resumo vira o checkout que manda o pedido ao
 * servidor (`ResumoComCheckout`). Sem ela, é o checkout de demonstração do protótipo.
 */
export function SecaoDoCarrinho({
  variante,
  linkDoCardapio,
  linkDosPedidos,
  vitrine,
  aoCriarPedido,
}: {
  variante: 'cardapio' | 'cliente';
  linkDoCardapio: string;
  linkDosPedidos: string;
  vitrine?: Vitrine;
  aoCriarPedido?: (pedido: PedidoPublico) => void;
}) {
  const itens = useCarrinho();
  const navegar = useNavigate();
  const [forma, definirForma] = useState('Pix');
  const [troco, definirTroco] = useState('');
  const { subtotal, fee, total } = totaisDoCarrinho(itens);
  const noCardapio = variante === 'cardapio';

  function finalizar() {
    const pedido = finalizarPedido(forma, troco);
    if (pedido === null) {
      toast('Adicione itens ao carrinho antes de finalizar.');
      return;
    }
    toast(`Pedido #${pedido.number} criado com sucesso`);
    window.setTimeout(() => void navegar(linkDosPedidos), 450);
  }

  function retirar(id: string, nome: string) {
    retirarDoCarrinho(id);
    toast(`${nome} foi retirado do carrinho`);
  }

  function limpar() {
    if (itens.length === 0) return;
    limparCarrinho();
    toast('Carrinho limpo');
  }

  return (
    <section id="carrinho" className={noCardapio ? 'panel cart-section' : 'panel'}>
      <div className="section-title">
        <div className="title-icon">
          <span className="material-symbols-outlined">shopping_cart</span>
        </div>
        <div>
          <small>SEU PEDIDO</small>
          <h2>Carrinho</h2>
          <p>
            {noCardapio
              ? 'Os valores são atualizados automaticamente.'
              : 'Adicione, retire itens e acompanhe o total em tempo real.'}
          </p>
        </div>
      </div>
      <div className="cart-layout">
        <div className="cart-panel">
          <div id="cartItems">
            {itens.length === 0 ? (
              <div className="empty-cart">
                <span className="material-symbols-outlined">shopping_cart</span>
                <strong>Seu carrinho está vazio</strong>
                <p>Adicione seus lanches favoritos para começar.</p>
                <Link className="btn btn-primary" to={linkDoCardapio}>
                  Ver cardápio
                </Link>
              </div>
            ) : (
              itens.map((item) => (
                <article className="cart-item" key={item.id}>
                  <img
                    src={item.image}
                    alt={item.name}
                    loading="lazy"
                    decoding="async"
                    onError={(evento) => {
                      evento.currentTarget.src = IMAGEM_PADRAO;
                    }}
                  />
                  <div className="cart-item-info">
                    <h3>{item.name}</h3>
                    <p>{dinheiro(item.price)} cada</p>
                    <div className="qty-control" aria-label={`Quantidade de ${item.name}`}>
                      <button
                        type="button"
                        onClick={() => mudarQuantidade(item.id, -1)}
                        aria-label="Diminuir quantidade"
                      >
                        −
                      </button>
                      <span>{item.qty}</span>
                      <button
                        type="button"
                        onClick={() => mudarQuantidade(item.id, 1)}
                        aria-label="Aumentar quantidade"
                      >
                        +
                      </button>
                    </div>
                  </div>
                  <div className="cart-item-end">
                    <strong>{dinheiro(item.price * item.qty)}</strong>
                    <button
                      type="button"
                      className="remove-item"
                      onClick={() => retirar(item.id, item.name)}
                    >
                      <span className="material-symbols-outlined">delete</span> Retirar
                    </button>
                  </div>
                </article>
              ))
            )}
          </div>
          <div className="cart-actions">
            {noCardapio ? (
              <a href="#menuContent" className="btn btn-light">
                <span className="material-symbols-outlined">add</span>Continuar escolhendo
              </a>
            ) : (
              <Link to={linkDoCardapio} className="btn btn-light">
                <span className="material-symbols-outlined">add</span>Adicionar mais itens
              </Link>
            )}
            <button
              type="button"
              className="btn btn-danger-soft"
              data-clear-cart=""
              disabled={itens.length === 0}
              onClick={limpar}
            >
              <span className="material-symbols-outlined">delete_sweep</span>
              {noCardapio ? 'Limpar' : 'Limpar carrinho'}
            </button>
          </div>
        </div>
        <aside className="order-summary">
          {vitrine !== undefined && aoCriarPedido !== undefined ? (
            <ResumoComCheckout itens={itens} vitrine={vitrine} aoCriar={aoCriarPedido} />
          ) : (
            <>
              <div className="summary-heading">
                <span>Resumo do pedido</span>
                <span className="summary-live">
                  <i /> Atualizado
                </span>
              </div>
              <div className="summary-row">
                <span>Subtotal</span>
                <b data-subtotal="">{dinheiro(subtotal)}</b>
              </div>
              <div className="summary-row">
                <span>Taxa de entrega</span>
                <b data-fee="">{dinheiro(fee)}</b>
              </div>
              <div className="summary-divider" />
              <div className="summary-row total">
                <span>Total</span>
                <b data-total="">{dinheiro(total)}</b>
              </div>
              <div
                className={
                  noCardapio ? 'payment-checkout menu-payment-checkout' : 'payment-checkout'
                }
              >
                <div className="summary-heading">
                  <span>Forma de pagamento</span>
                  <span className="material-symbols-outlined section-icon">payments</span>
                </div>
                <label className="payment-select-label" htmlFor="clientPaymentMethod">
                  Escolha como deseja pagar
                </label>
                <select
                  id="clientPaymentMethod"
                  className="payment-select"
                  value={forma}
                  onChange={(evento) => definirForma(evento.target.value)}
                >
                  <option value="Pix">Pix</option>
                  <option value="Cartão de crédito">Cartão de crédito</option>
                  <option value="Cartão de débito">Cartão de débito</option>
                  <option value="Dinheiro">Dinheiro</option>
                  <option value="Vale-refeição">Vale-refeição</option>
                </select>
                <label
                  className="cash-change-option"
                  id="cashChangeWrap"
                  hidden={forma !== 'Dinheiro'}
                  htmlFor="cashChange"
                >
                  Troco para (opcional)
                  <input
                    id="cashChange"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Ex.: 50,00"
                    value={troco}
                    onChange={(evento) => definirTroco(evento.target.value)}
                  />
                </label>
              </div>
              <button
                type="button"
                className="btn btn-primary full checkout-btn"
                onClick={finalizar}
              >
                <span className="material-symbols-outlined">shopping_bag</span>Finalizar pedido
              </button>
              {noCardapio ? (
                <Link className="go-orders" to={linkDosPedidos}>
                  <span className="material-symbols-outlined">receipt_long</span>Ver meus pedidos
                </Link>
              ) : (
                <small className="secure-note">
                  <span className="material-symbols-outlined">verified_user</span> Pedido protegido
                  e confirmado pelo AKI Delivery
                </small>
              )}
            </>
          )}
        </aside>
      </div>
    </section>
  );
}
