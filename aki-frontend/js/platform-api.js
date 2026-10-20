/* Cliente de integração com o gateway do delivery-platform.
 * Ajuste a URL em window.AKI_API_BASE_URL antes de publicar.
 */
(() => {
  const BASE = (window.AKI_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
  const TOKEN_KEY = 'akiPlatformAccessToken';
  const STORE_KEY = 'akiPlatformMerchantId';
  class ApiError extends Error {
    constructor(status, detail) { super(detail || `A API respondeu ${status}`); this.status = status; }
  }
  async function request(path, { method='GET', body, auth=true }={}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (auth && token) headers.Authorization = `Bearer ${token}`;
    let response;
    try {
      response = await fetch(`${BASE}${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), credentials: 'omit' });
    } catch (e) {
      throw new Error(`Não foi possível conectar à API em ${BASE}. Confira se o gateway está ativo e se o CORS permite esta origem.`);
    }
    if (response.status === 401) { sessionStorage.removeItem(TOKEN_KEY); }
    if (!response.ok) {
      let detail = '';
      try { detail = (await response.json()).detail || ''; } catch (_) {}
      throw new ApiError(response.status, detail || (response.status === 401 ? 'Telefone ou senha inválidos, ou sessão expirada.' : `Falha na API (${response.status}).`));
    }
    if (response.status === 204 || response.status === 202) return null;
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  }
  window.AKIPlatform = {
    base: BASE,
    get token() { return sessionStorage.getItem(TOKEN_KEY); },
    get merchantId() { return sessionStorage.getItem(STORE_KEY); },
    set merchantId(v) { if (v) sessionStorage.setItem(STORE_KEY, v); else sessionStorage.removeItem(STORE_KEY); },
    async login(telefone, senha) {
      const result = await request('/api/v1/auth/login', { method:'POST', auth:false, body:{ telefone, senha } });
      sessionStorage.setItem(TOKEN_KEY, result.accessToken);
      return result;
    },
    solicitarCodigo(telefone) { return request('/api/v1/auth/verification-code', { method:'POST', auth:false, body:{ telefone } }); },
    cadastrar({ telefone, codigo, nome, senha }) { return request('/api/v1/auth/signup', { method:'POST', auth:false, body:{ telefone, codigo, nome, senha } }); },
    lojas() { return request('/api/v1/me/estabelecimentos'); },
    criarLoja(body) { return request('/api/v1/me/estabelecimentos', { method:'POST', body }); },
    categorias(id=this.merchantId) { return request(`/api/v1/merchants/${encodeURIComponent(id)}/catalog/categorias`); },
    criarCategoria(body,id=this.merchantId) { return request(`/api/v1/merchants/${encodeURIComponent(id)}/catalog/categorias`, { method:'POST', body }); },
    produtos(id=this.merchantId) { return request(`/api/v1/merchants/${encodeURIComponent(id)}/catalog/produtos`); },
    criarProduto(body,id=this.merchantId) { return request(`/api/v1/merchants/${encodeURIComponent(id)}/catalog/produtos`, { method:'POST', body }); },
    publicarProduto(produtoId,id=this.merchantId) { return request(`/api/v1/merchants/${encodeURIComponent(id)}/catalog/produtos/${encodeURIComponent(produtoId)}/publicacao`, { method:'PUT' }); },
    marcarDisponibilidade(produtoId,estado,id=this.merchantId) { return request(`/api/v1/merchants/${encodeURIComponent(id)}/catalog/produtos/${encodeURIComponent(produtoId)}/disponibilidade`, { method:'PUT', body:{estado} }); },
    logout() { sessionStorage.removeItem(TOKEN_KEY); sessionStorage.removeItem(STORE_KEY); }
  };
})();
