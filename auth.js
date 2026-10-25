/* AKI Delivery — autenticação apenas no frontend/localStorage. */
(function(){
  const roleMap={CLIENTE:'Cliente',ADMINISTRADOR:'Administrador',ENTREGADOR:'Entregador',COLABORADOR:'Colaborador'};
  const pageName=location.pathname.split('/').pop().toLowerCase();
  const publicPages=new Set(['cadastro.html']);
  const token=localStorage.getItem('aki_access_token');
  // Protótipo visual: páginas podem ser abertas diretamente sem autenticação.

  const stored=(()=>{try{return JSON.parse(localStorage.getItem('aki_user')||'null')}catch{return null}})();
  const demo=stored||{id:'demo-user',name:'Usuário demonstração',email:'demo@aki.delivery',phone:'Não informado',role_name:'CLIENTE',demo:true};

  window.AKI_AUTH={
    token:token||'frontend-demo',user:demo,api:null,
    logout(){
      localStorage.removeItem('aki_access_token');
      localStorage.removeItem('aki_user');
      location.href=location.pathname.includes('/pages/')?'../index.html':'index.html';
    }
  };

  document.addEventListener('DOMContentLoaded',()=>{
    document.querySelectorAll('[data-user-name]').forEach(el=>el.textContent=demo.name||'Cliente');
    document.querySelectorAll('[data-user-role]').forEach(el=>el.textContent=roleMap[demo.role_name]||'Cliente');
    document.querySelectorAll('[data-user-email]').forEach(el=>el.textContent=demo.email||'—');
    document.querySelectorAll('[data-user-phone]').forEach(el=>el.textContent=demo.phone||'Não informado');
    document.querySelectorAll('[data-user-avatar]').forEach(el=>el.textContent=(demo.name||'UD').split(/\s+/).map(x=>x[0]).slice(0,2).join('').toUpperCase());
    document.querySelectorAll('[data-logout]').forEach(el=>el.addEventListener('click',e=>{e.preventDefault();window.AKI_AUTH.logout();}));
  });
})();
