(function(){
  const brl = new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'});
  const orders = typeof getOrders === 'function' ? getOrders() : JSON.parse(localStorage.getItem('akiOrders') || '[]');
  const today = new Date();
  const sameDay = o => { const d = new Date(o.createdAt || o.date || today); return d.toDateString() === today.toDateString(); };
  const todayOrders = orders.filter(sameDay);
  const total = todayOrders.reduce((s,o)=>s + Number(o.total || 0),0);
  const el = id => document.getElementById(id);
  if(el('todayOrders')) el('todayOrders').textContent = todayOrders.length;
  if(el('todaySales')) el('todaySales').textContent = brl.format(total);

  const chart = el('salesChart');
  const slots = ['08h','10h','12h','14h','16h','18h','20h','22h'];
  const values = slots.map((_,i)=> todayOrders.reduce((sum,o)=>{
    const d=new Date(o.createdAt || o.date || today); const h=d.getHours();
    return (h >= 8+i*2 && h < 10+i*2) ? sum + Number(o.total||0) : sum;
  },0));
  const max = Math.max(...values,1);
  if(chart){ chart.innerHTML=''; values.forEach((v,i)=>{ const bar=document.createElement('div'); bar.className='sales-bar-wrap'; bar.title=slots[i]+' — '+brl.format(v); bar.innerHTML='<div class="sales-value">'+(v?brl.format(v):'')+'</div><div class="sales-bar" style="height:'+Math.max(8,(v/max)*180)+'px"></div><small>'+slots[i]+'</small>'; chart.appendChild(bar); }); }

  const payments = { 'Pix':0,'Cartão de crédito':0,'Cartão de débito':0,'Dinheiro':0,'Vale-refeição':0 };
  todayOrders.forEach(o=>{ const p=o.paymentMethod || o.payment || 'Pix'; if(payments[p]===undefined) payments[p]=0; payments[p]+=Number(o.total||0); });
  const list=el('paymentList');
  if(list){ const maxPay=Math.max(...Object.values(payments),1); list.innerHTML=Object.entries(payments).map(([name,val])=>'<div class="payment-row"><div><span>'+name+'</span><strong>'+brl.format(val)+'</strong></div><div class="payment-track"><i style="width:'+((val/maxPay)*100)+'%"></i></div></div>').join(''); }

  const select=el('whatsappOrder');
  if(select){ orders.slice().reverse().forEach(o=>{ const op=document.createElement('option'); op.value=o.id || o.number || ''; op.textContent='#'+(o.number || o.id || '---')+' · '+(o.customerName || o.customer || 'Cliente')+' · '+brl.format(Number(o.total||0)); select.appendChild(op); }); }
  const msgByStatus={recebido:'Seu pedido foi recebido pela AKI Delivery. Já estamos cuidando dele! 🍔',preparo:'Seu pedido está em preparo. Em breve estará a caminho! 👨‍🍳',entrega:'Seu pedido saiu para entrega. O entregador está a caminho! 🛵',entregue:'Seu pedido foi entregue. Obrigado por pedir com a AKI Delivery! ❤️'};
  const btn=el('sendWhatsapp');
  if(btn) btn.addEventListener('click',()=>{ const id=select.value; const status=el('whatsappStatus').value; const order=orders.find(o=>String(o.id||o.number)===String(id)); if(!order){ alert('Selecione um pedido.'); return; } const phone=(order.customerPhone||order.phone||'').replace(/\D/g,''); const text='Olá! Aqui é da AKI Delivery. Pedido #'+(order.number||order.id)+' — '+msgByStatus[status]+' Total: '+brl.format(Number(order.total||0))+'.'; const url=phone ? 'https://wa.me/'+phone+'?text='+encodeURIComponent(text) : 'https://wa.me/?text='+encodeURIComponent(text); window.open(url,'_blank','noopener'); });
})();

// Central de mensagens visível dentro do aplicativo
(function initWhatsappMessageCenter(){
  const target = document.querySelector('#whatsappMessages');
  if (!target) return;
  const orders = typeof getOrders === 'function' ? getOrders() : JSON.parse(localStorage.getItem('akiOrders') || '[]');
  const templates = {
    recebido: 'Olá! Aqui é da AKI Delivery. Recebemos seu pedido e já estamos cuidando dele. 🍔',
    preparo: 'Olá! Seu pedido está em preparo. Em breve estará a caminho! 👨‍🍳',
    entrega: 'Olá! Seu pedido saiu para entrega. O entregador está a caminho! 🛵',
    entregue: 'Olá! Seu pedido foi entregue. Obrigado por pedir com a AKI Delivery! ❤️'
  };
  if (!orders.length) { target.innerHTML='<div class="empty-menu"><span class="material-symbols-outlined">forum</span><strong>Nenhuma mensagem disponível</strong><p>As mensagens dos pedidos aparecerão aqui.</p></div>'; return; }
  target.innerHTML = orders.slice(0,8).map((o,i)=>{
    const status = String(o.status||'Pedido recebido').toLowerCase();
    const key = status.includes('preparo')?'preparo':status.includes('entrega')?'entrega':status.includes('entreg')?'entregue':'recebido';
    const text = `${templates[key]} Pedido #${o.number}. Total: ${new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(Number(o.total||0))}.`;
    return `<details class="whatsapp-message" ${i===0?'open':''}><summary><span class="message-avatar"><span class="material-symbols-outlined">chat</span></span><span class="message-summary"><strong>${o.customer||'Cliente'}</strong><small>Pedido #${o.number} · ${o.status||'Pedido recebido'}</small></span><span class="material-symbols-outlined expand-icon">expand_more</span></summary><div class="message-body"><p>${text}</p><div class="message-meta"><span><span class="material-symbols-outlined">payments</span>${o.paymentMethod||'Pix'}</span><span>${new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(Number(o.total||0))}</span></div></div></details>`;
  }).join('');
})();
