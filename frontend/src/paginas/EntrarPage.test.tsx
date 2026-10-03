import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router';
import { SessaoProvider } from '../auth/SessaoProvider';
import { EntrarPage } from './EntrarPage';

/**
 * A tela de entrar, atravessando o cliente HTTP de verdade até um `fetch` de
 * mentira — e não com o cliente dublado.
 *
 * <p>É a mesma escolha do `AutorizacaoComercialHttpIT` do back: dublar a camada
 * de baixo prova que a de cima foi chamada; dublar o transporte prova o que
 * saiu no fio. Aqui isso importa porque o erro mais provável desta tela é
 * mandar o campo errado no corpo.
 */
describe('EntrarPage', () => {
  let chamadas: Array<{ url: string; init: RequestInit }>;

  function montar() {
    render(
      <MemoryRouter initialEntries={['/entrar']}>
        <SessaoProvider>
          <Routes>
            <Route path="/entrar" element={<EntrarPage />} />
            <Route path="/painel" element={<h1>Painel</h1>} />
          </Routes>
        </SessaoProvider>
      </MemoryRouter>,
    );
  }

  beforeEach(() => {
    chamadas = [];
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function fetchQueResponde(status: number, corpo?: string) {
    return vi.fn((url: string, init: RequestInit) => {
      chamadas.push({ url, init });
      return Promise.resolve(
        new Response(corpo ?? null, { status, headers: { 'Content-Type': 'application/json' } }),
      );
    });
  }

  /**
   * O corpo que saiu, tipado.
   *
   * `init.body` é `BodyInit | null`, e passá-lo por `String()` daria
   * `[object Object]` para qualquer coisa que não fosse string — o teste passaria
   * a comparar lixo com lixo. A conferência de tipo aqui é o que garante que a
   * asserção olha o JSON de verdade.
   */
  function corpoEnviado(indice: number): unknown {
    const corpo = chamadas[indice]?.init.body;
    if (typeof corpo !== 'string') throw new Error('o corpo enviado não é texto');
    return JSON.parse(corpo);
  }

  function sessaoGuardada(): { token: string; expiraEm: number } | null {
    const cru = sessionStorage.getItem('delivery.sessao');
    if (cru === null) return null;
    return JSON.parse(cru) as { token: string; expiraEm: number };
  }

  it('manda telefone e senha — os nomes que o contrato declara', async () => {
    vi.stubGlobal(
      'fetch',
      fetchQueResponde(200, '{"accessToken":"t.o.k","tokenType":"Bearer","expiresIn":1800}'),
    );
    montar();

    await userEvent.type(screen.getByLabelText('Telefone'), '21999990000');
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-de-verdade');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(chamadas).toHaveLength(1));
    expect(chamadas[0]?.url).toContain('/api/v1/auth/login');
    // Os nomes: `telefone` e `senha`. Não `email`, não `username`, não
    // `password` — e é exatamente aqui que um protótipo copiado de referência
    // erraria sem que nada mais reclamasse.
    expect(corpoEnviado(0)).toEqual({
      telefone: '21999990000',
      senha: 'senha-de-verdade',
    });
  });

  it('login bem-sucedido guarda a sessão e navega para o painel', async () => {
    vi.stubGlobal(
      'fetch',
      fetchQueResponde(200, '{"accessToken":"t.o.k","tokenType":"Bearer","expiresIn":1800}'),
    );
    montar();

    await userEvent.type(screen.getByLabelText('Telefone'), '21999990000');
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-de-verdade');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('heading', { name: 'Painel' })).toBeInTheDocument();
    expect(sessaoGuardada()?.token).toBe('t.o.k');
  });

  it('a expiração guardada vem do expiresIn, e não de um palpite', async () => {
    // 1800 segundos. Se alguém trocar por minutos ou esquecer o × 1000, a
    // sessão morre em 1,8 segundo ou dura 30 mil minutos, e nenhum outro teste
    // pega isso.
    vi.stubGlobal(
      'fetch',
      fetchQueResponde(200, '{"accessToken":"t.o.k","tokenType":"Bearer","expiresIn":1800}'),
    );
    const antes = Date.now();
    montar();

    await userEvent.type(screen.getByLabelText('Telefone'), '21999990000');
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-de-verdade');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await screen.findByRole('heading', { name: 'Painel' });
    const expiraEm = sessaoGuardada()?.expiraEm ?? 0;
    expect(expiraEm - antes).toBeGreaterThanOrEqual(1_800_000);
    expect(expiraEm - antes).toBeLessThan(1_830_000);
  });

  it('o 401 do login mostra o detail do servidor, e não um texto inventado', async () => {
    vi.stubGlobal('fetch', fetchQueResponde(401, '{"detail":"telefone ou senha inválidos"}'));
    montar();

    await userEvent.type(screen.getByLabelText('Telefone'), '21999990000');
    await userEvent.type(screen.getByLabelText('Senha'), 'errada12');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('telefone ou senha inválidos');
    expect(sessionStorage.getItem('delivery.sessao')).toBeNull();
  });

  it('servidor fora mostra mensagem de servidor, e não de senha errada', async () => {
    // A confusão custa caro no primeiro dia: com o gateway no ar e o CORS
    // errado, o navegador dá "Failed to fetch" e uma tela que dissesse
    // "telefone ou senha inválidos" mandaria a pessoa procurar no lugar errado.
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))),
    );
    montar();

    await userEvent.type(screen.getByLabelText('Telefone'), '21999990000');
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-de-verdade');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('falar com o servidor');
  });

  it('campo vazio não sai do navegador', async () => {
    vi.stubGlobal('fetch', fetchQueResponde(200, '{}'));
    montar();

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(chamadas).toHaveLength(0);
  });

  it('não há seletor de perfil nesta tela', () => {
    // O protótipo de 28/09 tinha um `<select>` "Tipo de acesso" que escolhia
    // Administrador, Colaborador, Entregador ou Cliente e redirecionava. Este
    // teste é o que faz alguém parar antes de reintroduzi-lo: a permissão vem do
    // vínculo, nunca do que o cliente declara.
    vi.stubGlobal('fetch', fetchQueResponde(200, '{}'));
    montar();

    expect(screen.queryByRole('combobox')).toBeNull();
  });
});
