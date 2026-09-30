import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { guardar } from '../auth/armazenamento';
import { SessaoProvider } from '../auth/SessaoProvider';
import { ErroDaApi } from './cliente';
import { useRecurso } from './useRecurso';

vi.mock('./cliente', async () => {
  const real = await vi.importActual<typeof import('./cliente')>('./cliente');
  return { ...real, chamar: vi.fn() };
});

// `chamar` é genérica, e `vi.mocked` de função genérica briga com o typecheck.
// O elenco para `Mock` é o que compila — e o que este arquivo precisa é da
// interface do dublê, não do tipo de retorno da função real.
const { chamar } = await import('./cliente');
const chamarFalso = chamar as unknown as Mock;

// O hook lê o token da sessão, então ele roda dentro do provedor, com uma
// sessão guardada — o `preparo.ts` limpa o sessionStorage antes de cada caso.
beforeEach(() => {
  guardar({ token: 't.o.k', expiraEm: Date.now() + 1_800_000 });
});

afterEach(() => {
  chamarFalso.mockReset();
});

const wrapper = SessaoProvider;

describe('useRecurso', () => {
  it('começa carregando e chega a pronto', async () => {
    chamarFalso.mockResolvedValue({ nome: 'Pizzaria' });

    const { result } = renderHook(
      () => useRecurso<{ nome: string }>('/api/v1/me/estabelecimentos'),
      { wrapper },
    );

    expect(result.current.estado).toBe('carregando');
    await waitFor(() => expect(result.current.estado).toBe('pronto'));
    expect(result.current).toEqual({ estado: 'pronto', dados: { nome: 'Pizzaria' } });
  });

  it('o token da sessão vai junto — sem ele a rota responde 401 e o cliente derruba a sessão', async () => {
    chamarFalso.mockResolvedValue([]);

    renderHook(() => useRecurso<unknown>('/api/v1/me/estabelecimentos'), { wrapper });

    await waitFor(() =>
      expect(chamarFalso).toHaveBeenCalledWith('/api/v1/me/estabelecimentos', { token: 't.o.k' }),
    );
  });

  it('sem sessão, não chama a API', () => {
    sessionStorage.clear();

    renderHook(() => useRecurso<unknown>('/api/v1/me/estabelecimentos'), { wrapper });

    expect(chamarFalso).not.toHaveBeenCalled();
  });

  it('caminho nulo não chama a API e fica carregando', () => {
    const { result } = renderHook(() => useRecurso<unknown>(null), { wrapper });

    expect(chamarFalso).not.toHaveBeenCalled();
    expect(result.current.estado).toBe('carregando');
  });

  it('erro da API vira estado de erro, com o status preservado', async () => {
    chamarFalso.mockRejectedValue(new ErroDaApi(403));

    const { result } = renderHook(() => useRecurso<unknown>('/qualquer'), { wrapper });

    await waitFor(() => expect(result.current.estado).toBe('erro'));
    const atual = result.current;
    expect(atual.estado === 'erro' && atual.erro).toBeInstanceOf(ErroDaApi);
    expect(atual.estado === 'erro' && (atual.erro as ErroDaApi).status).toBe(403);
  });

  /**
   * O caso que justifica este arquivo.
   *
   * A pessoa troca de loja duas vezes. A primeira busca é lenta, a segunda é
   * rápida, e a lenta responde por último. Sem a guarda `atual`, o estado final
   * seria o da loja ERRADA — dado verdadeiro, no lugar errado.
   *
   * Sem a limpeza do `useEffect`, este caso fica vermelho. É o único aqui que
   * não passaria por construção.
   */
  it('resposta atrasada da busca anterior é descartada', async () => {
    let terminarALenta: (valor: string) => void = () => {};
    const lenta = new Promise<string>((resolve) => {
      terminarALenta = resolve;
    });

    chamarFalso.mockImplementation((caminho: string) =>
      caminho === '/loja-a' ? lenta : Promise.resolve('produtos da B'),
    );

    const { result, rerender } = renderHook(({ caminho }) => useRecurso<string>(caminho), {
      initialProps: { caminho: '/loja-a' },
      wrapper,
    });

    rerender({ caminho: '/loja-b' });
    await waitFor(() =>
      expect(result.current).toEqual({ estado: 'pronto', dados: 'produtos da B' }),
    );

    // Dentro de act(): sem ele, a atualização da resposta atrasada não é
    // aplicada antes do expect, e o caso passava com a guarda desligada —
    // medido na W-B, com a limpeza do efeito comentada.
    await act(async () => {
      terminarALenta('produtos da A');
      await lenta;
    });

    expect(result.current).toEqual({ estado: 'pronto', dados: 'produtos da B' });
  });

  it('trocar de caminho volta a carregando antes de responder', async () => {
    chamarFalso.mockResolvedValue('qualquer');

    const { result, rerender } = renderHook(({ caminho }) => useRecurso<string>(caminho), {
      initialProps: { caminho: '/um' },
      wrapper,
    });
    await waitFor(() => expect(result.current.estado).toBe('pronto'));

    chamarFalso.mockImplementation(() => new Promise(() => {}));
    rerender({ caminho: '/dois' });

    expect(result.current.estado).toBe('carregando');
  });
});
