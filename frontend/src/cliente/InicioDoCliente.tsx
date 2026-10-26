import { Link } from 'react-router';
import { quantidadeNoCarrinho, useCarrinho, usePedidos } from '../aki/pedidos';
import { toast } from '../aki/toast';
import { SecaoDoCarrinho } from './Carrinho';
import { CascaDoCliente } from './CascaDoCliente';

/**
 * A área do cliente do protótipo (`cliente.html`). Acompanhamento e trajeto são
 * de demonstração: o pedido e a entrega ainda não existem no back-end.
 */
export function InicioDoCliente() {
  const pedidos = usePedidos();
  const atual = pedidos[0];
  const noCarrinho = quantidadeNoCarrinho(useCarrinho());

  return (
    <CascaDoCliente
      atual="inicio"
      icone="notificacoes"
      titulo="Área do cliente"
      subtitulo="Bem-vindo ao AKI Delivery"
    >
      <section className="client-hero">
        <div className="client-hero-copy">
          <span className="eyebrow">AKI LANCHES • DELIVERY</span>
          <h2>
            Seu pedido começa
            <br />
            <strong>com sabor.</strong>
          </h2>
          <p>Escolha seu lanche, acompanhe cada etapa e receba tudo quentinho.</p>
          <div className="hero-actions">
            <Link className="btn btn-primary" to="/cliente/cardapio">
              <span className="material-symbols-outlined">restaurant_menu</span>Ver cardápio
            </Link>
            <Link className="btn btn-light" to="/cliente/pedidos">
              <span className="material-symbols-outlined">receipt_long</span>Meus pedidos
            </Link>
          </div>
        </div>
        <div className="client-hero-image">
          <img src="/assets/banner-hamburguer.jpg" alt="Sanduíche do AKI Delivery" />
        </div>
      </section>

      <section className="quick-grid" aria-label="Atalhos">
        <Link to="/cliente/cardapio" className="quick-card">
          <span className="quick-icon orange">
            <span className="material-symbols-outlined">restaurant_menu</span>
          </span>
          <div>
            <strong>Cardápio</strong>
            <small>Escolha seus favoritos</small>
          </div>
          <span className="material-symbols-outlined arrow">chevron_right</span>
        </Link>
        <a href="#carrinho" className="quick-card">
          <span className="quick-icon green">
            <span className="material-symbols-outlined">shopping_cart</span>
          </span>
          <div>
            <strong>Carrinho</strong>
            <small>
              <span data-cart-count="">{noCarrinho}</span> item(ns) selecionado(s)
            </small>
          </div>
          <span className="material-symbols-outlined arrow">chevron_right</span>
        </a>
        <Link to="/cliente/pedidos" className="quick-card">
          <span className="quick-icon blue">
            <span className="material-symbols-outlined">receipt_long</span>
          </span>
          <div>
            <strong>Meus pedidos</strong>
            <small>Histórico e tickets</small>
          </div>
          <span className="material-symbols-outlined arrow">chevron_right</span>
        </Link>
      </section>

      <section className="panel tracking" id="pedido">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">route</span>
          </div>
          <div>
            <small>ACOMPANHAMENTO</small>
            <h2>Acompanhe seu pedido</h2>
            <p>Veja em qual etapa seu pedido está e acompanhe a entrega.</p>
          </div>
        </div>
        <div className="tracking-order-head">
          <div>
            <span>Pedido atual</span>
            <strong data-current-order="">{atual === undefined ? '#—' : `#${atual.number}`}</strong>
          </div>
          <div className="tracking-live">
            <i /> Atualização automática
          </div>
        </div>
        <div className="timeline">
          <div className="timeline-step done">
            <div className="timeline-icon">
              <span className="material-symbols-outlined">check_circle</span>
            </div>
            <b>Pedido recebido</b>
            <p>Seu pedido foi recebido.</p>
          </div>
          <div className="timeline-line done" />
          <div className="timeline-step current">
            <div className="timeline-icon">
              <span className="material-symbols-outlined">restaurant</span>
            </div>
            <b>Em preparo</b>
            <p>Estamos preparando.</p>
          </div>
          <div className="timeline-line" />
          <div className="timeline-step">
            <div className="timeline-icon">
              <span className="material-symbols-outlined">two_wheeler</span>
            </div>
            <b>Em entrega</b>
            <p>A caminho.</p>
          </div>
          <div className="timeline-line" />
          <div className="timeline-step">
            <div className="timeline-icon">
              <span className="material-symbols-outlined">task_alt</span>
            </div>
            <b>Entregue</b>
            <p>Pedido finalizado.</p>
          </div>
        </div>
      </section>

      <section className="panel map-panel" id="trajeto">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">near_me</span>
          </div>
          <div>
            <small>ENTREGA</small>
            <h2>Trajeto do entregador</h2>
            <p>Visualização do percurso do restaurante até seu endereço.</p>
          </div>
        </div>
        <div className="delivery-map">
          <div className="map-block block-a" />
          <div className="map-block block-b" />
          <div className="map-block block-c" />
          <div className="map-block block-d" />
          <div className="map-road road-1" />
          <div className="map-road road-2" />
          <div className="map-road road-3" />
          <div className="map-road road-4" />
          <div className="route-line" />
          <div className="map-marker restaurant">
            <span className="material-symbols-outlined">restaurant</span>
            <small>AKI Lanches</small>
          </div>
          <div className="map-marker driver">
            <span className="material-symbols-outlined">two_wheeler</span>
            <small>Entregador</small>
          </div>
          <div className="map-marker customer">
            <span className="material-symbols-outlined">home</span>
            <small>Você</small>
          </div>
          <div className="map-status">
            <div className="driver-avatar">
              <span className="material-symbols-outlined">two_wheeler</span>
            </div>
            <div>
              <strong>Entregador a caminho</strong>
              <span>
                Previsão de chegada: <b>15 min</b>
              </span>
            </div>
            <span className="material-symbols-outlined">chevron_right</span>
          </div>
          <div className="map-zoom">
            <button type="button" aria-label="Aumentar mapa">
              +
            </button>
            <button type="button" aria-label="Diminuir mapa">
              −
            </button>
          </div>
        </div>
        <div className="route-summary">
          <div>
            <span className="material-symbols-outlined">storefront</span>
            <div>
              <small>Saída</small>
              <strong>AKI Lanches</strong>
            </div>
          </div>
          <span className="route-dots" />
          <div>
            <span className="material-symbols-outlined">location_on</span>
            <div>
              <small>Destino</small>
              <strong>Seu endereço</strong>
            </div>
          </div>
          <div className="eta">
            <small>Tempo estimado</small>
            <strong>15 min</strong>
          </div>
        </div>
        <div className="map-note">
          <span className="material-symbols-outlined">info</span> O mapa está preparado para receber
          a localização real do entregador quando a integração com o banco de dados/GPS estiver
          ativa.
        </div>
      </section>

      <SecaoDoCarrinho
        variante="cliente"
        linkDoCardapio="/cliente/cardapio"
        linkDosPedidos="/cliente/pedidos"
      />

      <section className="panel account-panel" id="conta">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">person</span>
          </div>
          <div>
            <small>PERFIL</small>
            <h2>Minha Conta</h2>
            <p>Seus dados de cliente.</p>
          </div>
        </div>
        <div className="account-card">
          <div className="avatar large">CL</div>
          <div>
            <strong>Cliente AKI</strong>
            <span>cliente@akidelivery.com</span>
            <small>
              <span className="material-symbols-outlined">location_on</span> Endereço principal
              cadastrado
            </small>
          </div>
          <button
            className="btn btn-light"
            type="button"
            onClick={() => toast('Área de edição de perfil pronta para integração.')}
          >
            <span className="material-symbols-outlined">edit</span>Editar
          </button>
        </div>
      </section>

      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Seu pedido, seu jeito.
      </footer>
    </CascaDoCliente>
  );
}
