/* AKI Delivery - carrinho e interações compartilhadas */
let cart = JSON.parse(localStorage.getItem('akiCart') || '[]');

const saveCart = () => localStorage.setItem('akiCart', JSON.stringify(cart));

function addToCart(id) {
  const product = products.find(item => Number(item.id) === Number(id));
  if (!product) return;

  const existing = cart.find(item => Number(item.id) === Number(id));
  if (existing) {
    existing.qty += 1;
  } else {
    cart.push({ ...product, qty: 1 });
  }

  saveCart();
  renderCart();
  toast(`${product.name} foi adicionado ao carrinho`);
}

function changeQty(id, delta) {
  const item = cart.find(product => Number(product.id) === Number(id));
  if (!item) return;

  item.qty += Number(delta);
  if (item.qty <= 0) {
    removeFromCart(id, false);
    return;
  }

  saveCart();
  renderCart();
}

function removeFromCart(id, notify = true) {
  const item = cart.find(product => Number(product.id) === Number(id));
  cart = cart.filter(product => Number(product.id) !== Number(id));
  saveCart();
  renderCart();
  if (notify && item) toast(`${item.name} foi retirado do carrinho`);
}

function clearCart() {
  if (!cart.length) return;
  cart = [];
  saveCart();
  renderCart();
  toast('Carrinho limpo');
}

function cartTotals() {
  const subtotal = cart.reduce((total, item) => total + Number(item.price) * Number(item.qty), 0);
  const fee = subtotal > 0 ? 5 : 0;
  return { subtotal, fee, total: subtotal + fee };
}

function renderCart() {
  const list = document.querySelector('#cartItems');
  const { subtotal, fee, total } = cartTotals();

  if (list) {
    if (!cart.length) {
      list.innerHTML = `
        <div class="empty-cart">
          <span class="material-symbols-outlined">shopping_cart</span>
          <strong>Seu carrinho está vazio</strong>
          <p>Adicione seus lanches favoritos para começar.</p>
          <a class="btn btn-primary" href="cardapio.html">Ver cardápio</a>
        </div>`;
    } else {
      list.innerHTML = cart.map(item => `
        <article class="cart-item">
          <img src="${item.image}" alt="${item.name}" loading="lazy">
          <div class="cart-item-info">
            <h3>${item.name}</h3>
            <p>${money(item.price)} cada</p>
            <div class="qty-control" aria-label="Quantidade de ${item.name}">
              <button type="button" onclick="changeQty(${item.id}, -1)" aria-label="Diminuir quantidade">−</button>
              <span>${item.qty}</span>
              <button type="button" onclick="changeQty(${item.id}, 1)" aria-label="Aumentar quantidade">+</button>
            </div>
          </div>
          <div class="cart-item-end">
            <strong>${money(item.price * item.qty)}</strong>
            <button type="button" class="remove-item" onclick="removeFromCart(${item.id})">
              <span class="material-symbols-outlined">delete</span> Retirar
            </button>
          </div>
        </article>`).join('');
    }
  }

  document.querySelectorAll('[data-subtotal]').forEach(el => el.textContent = money(subtotal));
  document.querySelectorAll('[data-fee]').forEach(el => el.textContent = money(fee));
  document.querySelectorAll('[data-total]').forEach(el => el.textContent = money(total));
  document.querySelectorAll('[data-cart-count]').forEach(el => el.textContent = cart.reduce((sum, item) => sum + Number(item.qty), 0));
  document.querySelectorAll('[data-cart-empty]').forEach(el => el.hidden = cart.length > 0);
  document.querySelectorAll('[data-clear-cart]').forEach(el => el.disabled = cart.length === 0);
}

function renderMenu() {
  const target = document.querySelector('#menuContent');
  if (!target) return;

  const query = (document.querySelector('#searchInput')?.value || '').trim().toLowerCase();
  const category = document.querySelector('#categoryFilter')?.value || 'Todos';

  const filtered = products.filter(product => {
    const matchesCategory = category === 'Todos' || product.category === category;
    const text = `${product.name} ${product.description} ${product.category}`.toLowerCase();
    return matchesCategory && text.includes(query);
  });

  target.innerHTML = filtered.length ? filtered.map(product => `
    <article class="product-card">
      ${product.tag ? `<span class="badge">${product.tag}</span>` : ''}
      <img src="${product.image}" alt="${product.name}" loading="lazy">
      <div class="product-body">
        <span class="product-category">${product.category}</span>
        <h3>${product.name}</h3>
        <p>${product.description}</p>
        <div class="product-bottom">
          <span class="price">${money(product.price)}</span>
          <button class="add" type="button" onclick="addToCart(${product.id})" aria-label="Adicionar ${product.name}">
            <span class="material-symbols-outlined">add</span>
          </button>
        </div>
      </div>
    </article>`).join('') : `
      <div class="empty-menu">
        <span class="material-symbols-outlined">search_off</span>
        <strong>Nenhum produto encontrado</strong>
        <p>Tente outro nome ou categoria.</p>
      </div>`;
}

function toast(message) {
  let element = document.querySelector('.toast');
  if (!element) {
    element = document.createElement('div');
    element.className = 'toast';
    document.body.appendChild(element);
  }
  element.textContent = message;
  element.classList.add('show');
  clearTimeout(window.akiToastTimer);
  window.akiToastTimer = setTimeout(() => element.classList.remove('show'), 2200);
}

function toggleTheme() {
  document.body.classList.toggle('dark');
  localStorage.setItem('akiTheme', document.body.classList.contains('dark') ? 'dark' : 'light');
}

function initShared() {
  const menuBtn = document.querySelector('#menuBtn');
  const sidebar = document.querySelector('.sidebar');
  menuBtn?.addEventListener('click', () => sidebar?.classList.toggle('open'));

  document.querySelectorAll('.sidebar .nav a').forEach(link => {
    link.addEventListener('click', () => sidebar?.classList.remove('open'));
  });

  document.querySelector('#themeToggle')?.addEventListener('click', toggleTheme);
  if (localStorage.getItem('akiTheme') === 'dark') document.body.classList.add('dark');

  document.querySelector('#searchInput')?.addEventListener('input', renderMenu);
  document.querySelector('#categoryFilter')?.addEventListener('change', renderMenu);

  document.querySelectorAll('[data-clear-cart]').forEach(button => {
    button.addEventListener('click', clearCart);
  });

  const paymentSelect = document.querySelector('#clientPaymentMethod');
  const cashWrap = document.querySelector('#cashChangeWrap');
  paymentSelect?.addEventListener('change', () => { if (cashWrap) cashWrap.hidden = paymentSelect.value !== 'Dinheiro'; });
  renderMenu();
  renderCart();
  renderLatestOrderTicket();
  renderAdminOrders();
}

document.addEventListener('DOMContentLoaded', initShared);


/* =========================
   PEDIDOS / TICKET
========================= */
const ORDER_KEY = 'akiOrders';

function getOrders() {
  try { return JSON.parse(localStorage.getItem(ORDER_KEY) || '[]'); }
  catch { return []; }
}

function saveOrders(orders) {
  localStorage.setItem(ORDER_KEY, JSON.stringify(orders));
}

function nextOrderNumber() {
  const orders = getOrders();
  const last = orders.reduce((max, order) => Math.max(max, Number(order.number) || 123450), 123450);
  return String(last + 1).padStart(6, '0');
}

function createOrder() {
  if (!cart.length) {
    toast('Adicione itens ao carrinho antes de finalizar.');
    return;
  }

  const { subtotal, fee, total } = cartTotals();
  const order = {
    number: nextOrderNumber(),
    date: new Date().toISOString(),
    customer: 'Cliente AKI',
    status: 'Pedido recebido',
    statusClass: 'orange',
    items: cart.map(item => ({
      id: item.id,
      name: item.name,
      price: Number(item.price),
      qty: Number(item.qty),
      image: item.image
    })),
    subtotal,
    fee,
    total,
    paymentMethod: document.querySelector('#clientPaymentMethod')?.value || 'Pix',
    cashChange: document.querySelector('#cashChange')?.value || ''
  };

  const orders = getOrders();
  orders.unshift(order);
  saveOrders(orders.slice(0, 30));
  cart = [];
  saveCart();
  renderCart();
  renderLatestOrderTicket();
  toast(`Pedido #${order.number} criado com sucesso`);
  setTimeout(() => {
    const ticket = document.querySelector('#pedido-ticket');
    if (ticket) ticket.scrollIntoView({ behavior: 'smooth', block: 'start' });
    else window.location.href = 'pedidos.html';
  }, 450);
}

function formatOrderDate(iso) {
  const date = new Date(iso);
  return date.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}

function ticketMarkup(order) {
  if (!order) return `
    <div class="ticket-empty">
      <span class="material-symbols-outlined">receipt_long</span>
      <strong>Nenhum pedido realizado ainda</strong>
      <p>Adicione produtos ao carrinho e finalize seu primeiro pedido.</p>
      <a class="btn btn-primary" href="cardapio.html">Ir para o cardápio</a>
    </div>`;

  return `
    <div class="ticket">
      <div class="ticket-top">
        <div class="ticket-brand"><span class="material-symbols-outlined">lunch_dining</span><strong>AKI DELIVERY</strong></div>
        <span class="ticket-status ${order.statusClass || 'orange'}">${order.status || 'Pedido recebido'}</span>
      </div>
      <div class="ticket-head">
        <div><small>NÚMERO DO PEDIDO</small><strong>#${order.number}</strong></div>
        <div><small>DATA E HORA</small><strong>${formatOrderDate(order.date)}</strong></div>
      </div>
      <div class="ticket-items">
        ${order.items.map(item => `<div class="ticket-item"><div><b>${item.qty}x</b> ${item.name}</div><strong>${money(item.price * item.qty)}</strong></div>`).join('')}
      </div>
      <div class="ticket-payment"><span class="material-symbols-outlined">payments</span><span>Pagamento: <strong>${order.paymentMethod || 'Pix'}</strong>${order.paymentMethod === 'Dinheiro' && order.cashChange ? ` · Troco para ${money(order.cashChange)}` : ''}</span></div><div class="ticket-totals">
        <div><span>Subtotal</span><strong>${money(order.subtotal)}</strong></div>
        <div><span>Taxa de entrega</span><strong>${money(order.fee)}</strong></div>
        <div class="ticket-total"><span>Total do pedido</span><strong>${money(order.total)}</strong></div>
      </div>
      <div class="ticket-footer"><span class="material-symbols-outlined">verified</span> Obrigado por pedir no AKI Delivery!</div>
    </div>`;
}

function renderLatestOrderTicket() {
  const target = document.querySelector('#latestTicket');
  if (!target) return;
  target.innerHTML = ticketMarkup(getOrders()[0]);

  const current = getOrders()[0];
  if (current) {
    document.querySelectorAll('[data-current-order]').forEach(el => el.textContent = `#${current.number}`);
    document.querySelectorAll('[data-current-order-total]').forEach(el => el.textContent = money(current.total));
  }
}

function viewOrderTicket(number) {
  const order = getOrders().find(item => String(item.number) === String(number));
  if (!order) {
    toast('Detalhes do pedido não encontrados.');
    return;
  }
  let modal = document.querySelector('#ticketModal');
  if (!modal) {
    modal = document.createElement('div');
    modal.id = 'ticketModal';
    modal.className = 'ticket-modal';
    modal.innerHTML = `<div class="ticket-modal-backdrop" onclick="closeTicketModal()"></div><div class="ticket-modal-card"><button class="ticket-close" onclick="closeTicketModal()" aria-label="Fechar"><span class="material-symbols-outlined">close</span></button><div id="ticketModalContent"></div></div>`;
    document.body.appendChild(modal);
  }
  document.querySelector('#ticketModalContent').innerHTML = ticketMarkup(order);
  modal.classList.add('show');
  document.body.classList.add('modal-open');
}

function closeTicketModal() {
  document.querySelector('#ticketModal')?.classList.remove('show');
  document.body.classList.remove('modal-open');
}

function seedDemoOrders() {
  if (getOrders().length) return;
  const make = (number, customer, status, ids, date, statusClass) => {
    const items = ids.map(id => products.find(p => p.id === id)).filter(Boolean).map(p => ({ id:p.id,name:p.name,price:p.price,qty:1,image:p.image }));
    const subtotal = items.reduce((sum,i)=>sum+i.price*i.qty,0);
    const fee = 5;
    return {number,customer,status,statusClass,date,items,subtotal,fee,total:subtotal+fee};
  };
  saveOrders([
    make('123456','Cliente','Em preparo',[1,5],'2026-09-15T14:32:00','orange'),
    make('123455','Maria S.','Em entrega',[2,6],'2026-09-15T14:05:00','blue'),
    make('123454','João P.','Concluído',[11],'2026-09-15T13:40:00','green'),
    make('123453','Ana C.','Em preparo',[4,14],'2026-09-15T13:15:00','orange')
  ]);
}

function renderAdminOrders() {
  const body = document.querySelector('#adminOrdersBody');
  if (!body) return;
  seedDemoOrders();
  const orders = getOrders();
  body.innerHTML = orders.slice(0, 8).map(order => `<tr>
    <td><strong>#${order.number}</strong></td>
    <td>${order.customer || 'Cliente'}</td>
    <td>${money(order.total)}</td>
    <td><span class="status ${order.statusClass || 'orange'}">${order.status || 'Pedido recebido'}</span></td>
    <td><button type="button" class="ticket-btn" onclick="viewOrderTicket('${order.number}')"><span class="material-symbols-outlined">receipt_long</span> Ticket</button></td>
  </tr>`).join('');

  const totalOrders = orders.reduce((sum, order) => sum + Number(order.total || 0), 0);
  document.querySelectorAll('[data-orders-total]').forEach(el => el.textContent = money(totalOrders));
  document.querySelectorAll('[data-orders-count]').forEach(el => el.textContent = orders.length > 99 ? '99+' : orders.length);
}
