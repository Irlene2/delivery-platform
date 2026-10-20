(() => {
  const $ = id => document.getElementById(id);
  const status = (id, text, ok=false) => { const el=$(id); if(el){el.textContent=text;el.className='backend-status'+(text?(ok?' success':' error'):'');} };
  const arr = value => Array.isArray(value) ? value : Array.isArray(value?.content) ? value.content : Array.isArray(value?.itens) ? value.itens : [];
  const money = n => Number(n||0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
  let categories=[]; let products=[];
  function showGlobal(text,ok=false){status('globalStatus',text,ok);}
  async function loadStores(){
    try {
      const stores=arr(await AKIPlatform.lojas()); const select=$('storeSelect'); select.innerHTML='';
      if(!stores.length){select.innerHTML='<option value="">Nenhum estabelecimento cadastrado</option>'; $('storeSetup').hidden=false; $('catalogWorkspace').hidden=true; AKIPlatform.merchantId=''; showGlobal('Sua conta ainda não possui estabelecimento. Cadastre os dados da loja abaixo.'); return;}
      $('storeSetup').hidden=true; select.append(new Option('Selecione um estabelecimento',''));
      stores.forEach(s=>select.append(new Option(`${s.nome} — ${s.papel||'membro'}`,s.estabelecimentoId)));
      const chosen=stores.find(s=>s.estabelecimentoId===AKIPlatform.merchantId)||stores[0]; select.value=chosen.estabelecimentoId; AKIPlatform.merchantId=chosen.estabelecimentoId; await loadCatalog(); showGlobal('Estabelecimentos carregados da API.',true);
    } catch(e){showGlobal(e.message||'Falha ao carregar estabelecimentos. Faça login e confira o gateway.');$('storeSelect').innerHTML='<option value="">Não foi possível carregar</option>';}
  }
  async function loadCatalog(){
    const id=AKIPlatform.merchantId; if(!id){$('catalogWorkspace').hidden=true;return;}
    $('catalogWorkspace').hidden=false;
    try {
      [categories,products]=await Promise.all([AKIPlatform.categorias(id).then(arr),AKIPlatform.produtos(id).then(arr)]);
      renderCategories(); renderProducts(); showGlobal('Catálogo sincronizado com a API.',true);
    } catch(e){showGlobal(e.message||'Não foi possível carregar o catálogo.');}
  }
  function renderCategories(){
    $('categoryList').innerHTML=categories.length?categories.map(c=>`<div class="backend-item"><div><b>${escapeHtml(c.nome)}</b><small>Ordem ${c.ordem??0} · ${c.ativa===false?'Inativa':'Ativa'} · ${escapeHtml(c.id||'')}</small></div></div>`).join(''):'<p>Nenhuma categoria cadastrada.</p>';
    const select=$('productCategory'); select.innerHTML='';
    if(!categories.length) select.add(new Option('Cadastre uma categoria primeiro',''));
    categories.forEach(c=>select.add(new Option(c.nome,c.id)));
  }
  function renderProducts(){
    $('productList').innerHTML=products.length?products.map(p=>`<div class="backend-item"><div><b>${escapeHtml(p.nome)}</b><small>${money(p.precoBase)} · ${escapeHtml(p.estadoDePublicacao||'RASCUNHO')} · ${escapeHtml(p.disponibilidade||'DISPONIVEL')} · ${p.vendavel?'Vendável':'Não vendável'}</small><small>${escapeHtml(p.descricao||'Sem descrição')}</small></div><div class="backend-actions">${p.estadoDePublicacao!=='ATIVO'?`<button class="btn btn-primary" data-publish="${escapeHtml(p.id)}" type="button">Publicar</button>`:''}<button class="btn btn-light" data-stock="${escapeHtml(p.id)}" data-state="ESGOTADO_HOJE" type="button">Marcar esgotado</button><button class="btn btn-light" data-stock="${escapeHtml(p.id)}" data-state="DISPONIVEL" type="button">Disponível</button></div></div>`).join(''):'<p>Nenhum produto cadastrado.</p>';
  }
  function escapeHtml(value){return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
  $('reloadStores').addEventListener('click',loadStores); $('reloadCatalog').addEventListener('click',loadCatalog);
  $('storeSelect').addEventListener('change',async e=>{AKIPlatform.merchantId=e.target.value;await loadCatalog();});
  $('storeForm').addEventListener('submit',async e=>{
    e.preventDefault(); const f=new FormData(e.currentTarget); const nome=String(f.get('nome')).trim();
    const body={identificacao:{nome,documento:String(f.get('documento')).trim(),telefone:String(f.get('telefone')).trim(),enderecoTextual:String(f.get('endereco')).trim(),bairro:String(f.get('bairro')).trim(),fusoHorario:String(f.get('fuso'))},politicaDeTroco:{fundoMaximoDeTroco:Number(f.get('troco')),aceitaPedidoSemTrocoDisponivel:f.has('semTroco')},tipoDeOperacao:String(f.get('operacao')),metodosPorModalidade:{ENTREGA:['DINHEIRO','CARTAO','PIX'],RETIRADA:['DINHEIRO','CARTAO','PIX']}};
    try{await AKIPlatform.criarLoja(body);status('globalStatus','Estabelecimento criado com sucesso.',true);await loadStores();}catch(err){showGlobal(err.message||'Não foi possível criar o estabelecimento.');}
  });
  $('categoryForm').addEventListener('submit',async e=>{e.preventDefault();const f=new FormData(e.currentTarget);try{await AKIPlatform.criarCategoria({nome:String(f.get('nome')).trim(),ordem:Number(f.get('ordem'))});e.currentTarget.reset();e.currentTarget.elements.ordem.value='0';status('categoryStatus','Categoria salva no backend.',true);await loadCatalog();}catch(err){status('categoryStatus',err.message||'Erro ao salvar categoria.');}});
  $('productForm').addEventListener('submit',async e=>{e.preventDefault();const f=new FormData(e.currentTarget);const body={categoriaId:String(f.get('categoriaId')),nome:String(f.get('nome')).trim(),descricao:String(f.get('descricao')||'').trim(),precoBase:Number(f.get('precoBase')),modoDeControle:String(f.get('modoDeControle')),ordem:Number(f.get('ordem'))};try{await AKIPlatform.criarProduto(body);e.currentTarget.reset();e.currentTarget.elements.ordem.value='0';status('productStatus','Produto salvo no backend como rascunho. Publique-o para ativar.',true);await loadCatalog();}catch(err){status('productStatus',err.message||'Erro ao salvar produto.');}});
  $('productList').addEventListener('click',async e=>{const pub=e.target.closest('[data-publish]');const stock=e.target.closest('[data-stock]');try{if(pub){await AKIPlatform.publicarProduto(pub.dataset.publish);showGlobal('Produto publicado.',true);}else if(stock){await AKIPlatform.marcarDisponibilidade(stock.dataset.stock,stock.dataset.state);showGlobal('Disponibilidade atualizada.',true);}else return;await loadCatalog();}catch(err){showGlobal(err.message||'A operação foi recusada pela API.');}});
  document.querySelectorAll('[data-logout]').forEach(el=>el.addEventListener('click',e=>{e.preventDefault();AKIPlatform.logout();window.location.href='../index.html';}));
  if(!AKIPlatform.token){showGlobal('Você ainda não está autenticado. Entre pelo index.html para usar o catálogo conectado.');$('storeSelect').innerHTML='<option value="">Faça login para carregar</option>';}
  else loadStores();
})();
