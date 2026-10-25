document.addEventListener('DOMContentLoaded', () => {
  const orders = typeof getOrders === 'function' ? getOrders() : JSON.parse(localStorage.getItem('akiOrders') || '[]');
  const order = orders[0];
  const box = document.querySelector('#driverPaymentInfo');
  if (!box) return;
  if (!order) { box.innerHTML = '<span class="material-symbols-outlined">payments</span><span>Nenhum pedido disponível.</span>'; return; }
  const method = order.paymentMethod || 'Pix';
  const detail = method === 'Dinheiro' ? 'Pagamento em dinheiro na entrega' : `Pagamento: ${method}`;
  box.innerHTML = `<span class="material-symbols-outlined">payments</span><span>${detail}${order.cashChange ? ` · Troco para R$ ${Number(order.cashChange).toFixed(2).replace('.', ',')}` : ''}</span>`;
});
