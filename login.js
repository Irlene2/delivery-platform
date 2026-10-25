/* Login real pela API do delivery-platform. Não bloqueia a abertura direta das outras telas. */
document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('loginForm');
  const password = document.getElementById('passwordInput');
  const toggle = document.getElementById('passwordToggle');
  const phone = document.getElementById('emailInput');
  const message = document.getElementById('emailError');
  const button = document.getElementById('submitBtn');
  const buttonText = document.getElementById('btnText');
  toggle?.addEventListener('click', () => {
    if (!password) return;
    const show = password.type === 'password';
    password.type = show ? 'text' : 'password';
    const icon = toggle.querySelector('.material-icons');
    if (icon) icon.textContent = show ? 'visibility' : 'visibility_off';
    toggle.setAttribute('aria-label', show ? 'Ocultar senha' : 'Mostrar senha');
  });
  form?.addEventListener('submit', async event => {
    event.preventDefault();
    if (message) { message.textContent = ''; message.className = 'error-message'; }
    const telefone = (phone?.value || '').trim();
    const senha = password?.value || '';
    if (!telefone || telefone.replace(/\D/g, '').length < 10) {
      if (message) message.textContent = 'Informe o telefone com DDD usado no cadastro.';
      else alert('Informe o telefone com DDD usado no cadastro.');
      phone?.focus(); return;
    }
    if (!senha) { alert('Informe sua senha.'); password?.focus(); return; }
    if (button) button.disabled = true;
    if (buttonText) buttonText.textContent = 'Conectando…';
    try {
      await window.AKIPlatform.login(telefone, senha);
      window.location.href = 'pages/administrador.html';
    } catch (error) {
      const text = error?.message || 'Não foi possível entrar.';
      if (message) { message.textContent = text; message.className = 'error-message'; }
      else alert(text);
    } finally {
      if (button) button.disabled = false;
      if (buttonText) buttonText.textContent = 'Entrar';
    }
  });
  document.getElementById('forgotPassword')?.addEventListener('click', event => {
    event.preventDefault(); alert('A API atual não disponibiliza recuperação de senha. Use o fluxo de cadastro/verificação ou entre em contato com o administrador.');
  });
  document.getElementById('googleLogin')?.addEventListener('click', () => alert('Login Google não faz parte do contrato atual da API.'));
  document.getElementById('appleLogin')?.addEventListener('click', () => alert('Login Apple não faz parte do contrato atual da API.'));
});
