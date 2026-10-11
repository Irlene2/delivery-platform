import { render, screen, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { guardar } from '../auth/armazenamento';
import { SessaoProvider } from '../auth/SessaoProvider';
import { CardapioPorSecao } from './CardapioPorSecao';

vi.mock('../api/cliente', async () => {
  const real = await vi.importActual<typeof import('../api/cliente')>('../api/cliente');
  return { ...real, chamar: vi.fn() };
});

// `chamar` é genérica, e `vi.mocked` de função genérica briga com o typecheck.
const { chamar } = await import('../api/cliente');
const chamarFalso = chamar as unknown as Mock;

beforeEach(() => {
  guardar({ token: 't.o.k', expiraEm: Date.now() + 1_800_000 });
});

afterEach(() => {
  chamarFalso.mockReset();
});

const BEBIDAS = { id: 's-beb', nome: 'Bebidas', ordem: 1, ativa: true };
const PIZZAS = { id: 's-piz', nome: 'Pizzas', ordem: 0, ativa: true };

function produto(id: string, nome: string, categoriaId: string, ordem: number, extra = {}) {
  return {
    id,
    nome,
    categoriaId,
    ordem,
    precoBase: 10,
    vendavel: true,
    estadoDePublicacao: 'ATIVO',
    ...extra,
  };
}

/** O dublê das duas rotas que a tela compõe. */
function responder(secoes: unknown[], produtos: unknown[]) {
  chamarFalso.mockImplementation((caminho: string) => {
    if (caminho.endsWith('/catalog/categorias')) return Promise.resolve(secoes);
    if (caminho.includes('/catalog/produtos?')) {
      return Promise.resolve({
        conteudo: produtos,
        pagina: 0,
        tamanho: 100,
        total: produtos.length,
      });
    }
    return Promise.reject(new Error(`caminho inesperado: ${caminho}`));
  });
}

function montar() {
  render(
    <SessaoProvider>
      <CardapioPorSecao estabelecimentoId="p1" />
    </SessaoProvider>,
  );
}

/** Os títulos dos grupos, na ordem em que aparecem. */
function grupos(): string[] {
  return screen.getAllByRole('heading', { level: 3 }).map((titulo) => titulo.textContent ?? '');
}

function nomesEm(grupo: string): string[] {
  const secao = screen.getByRole('region', { name: grupo });
  return within(secao)
    .queryAllByRole('listitem')
    .map((item) => item.textContent ?? '');
}

describe('CardapioPorSecao', () => {
  it('pede as duas rotas: as seções e os produtos, na página do teto', async () => {
    responder([], []);

    montar();

    await screen.findByText(/cardápio está vazio/i);
    const caminhos = (chamarFalso.mock.calls as [string][]).map(([caminho]) => caminho);
    expect(caminhos).toContain('/api/v1/merchants/p1/catalog/categorias');
    expect(caminhos).toContain('/api/v1/merchants/p1/catalog/produtos?page=0&size=100');
  });

  /**
   * Duas seções, e numa delas dois produtos: com uma de cada, um agrupamento que
   * ignorasse o `categoriaId` passaria igual.
   */
  it('A1 · agrupa: seção de ordem menor primeiro, e dentro dela o produto de ordem menor', async () => {
    responder(
      [BEBIDAS, PIZZAS],
      [
        produto('1', 'Guaraná', 's-beb', 0),
        produto('2', 'Calabresa', 's-piz', 1),
        produto('3', 'Margherita', 's-piz', 0),
      ],
    );

    montar();

    await screen.findByText('Margherita');
    expect(grupos()).toEqual(['Pizzas', 'Bebidas']);
    const pizzas = nomesEm('Pizzas');
    expect(pizzas).toHaveLength(2);
    expect(pizzas[0]).toContain('Margherita');
    expect(pizzas[1]).toContain('Calabresa');
    expect(nomesEm('Bebidas')).toHaveLength(1);
    expect(nomesEm('Bebidas')[0]).toContain('Guaraná');
  });

  it('A2 · o produto cuja seção não existe cai em "Sem seção", no fim', async () => {
    responder(
      [PIZZAS],
      [produto('1', 'Margherita', 's-piz', 0), produto('9', 'Órfão', 's-apagada', 0)],
    );

    montar();

    await screen.findByText('Órfão');
    expect(grupos()).toEqual(['Pizzas', 'Sem seção']);
    expect(nomesEm('Sem seção')[0]).toContain('Órfão');
  });

  it('A3 · sem produto órfão, o grupo "Sem seção" não aparece', async () => {
    responder([PIZZAS], [produto('1', 'Margherita', 's-piz', 0)]);

    montar();

    await screen.findByText('Margherita');
    expect(screen.queryByRole('heading', { name: 'Sem seção' })).not.toBeInTheDocument();
  });

  it('A4 · seção vazia aparece, e diz que não tem produto', async () => {
    responder([PIZZAS, BEBIDAS], [produto('1', 'Margherita', 's-piz', 0)]);

    montar();

    await screen.findByText('Margherita');
    const bebidas = screen.getByRole('region', { name: 'Bebidas' });
    expect(within(bebidas).getByText(/nenhum produto nesta seção/i)).toBeInTheDocument();
  });

  /**
   * Um estado que a regra não produziria por si — ATIVO e não vendável é possível, mas a
   * tela não tem como saber disso sem o servidor. Ela mostra o que veio.
   */
  it('A5 · o estado é lido: ATIVO e vendavel false mostra "No cardápio" e "não vendável"', async () => {
    responder([PIZZAS], [produto('1', 'Margherita', 's-piz', 0, { vendavel: false })]);

    montar();

    const linha = (await screen.findByText('Margherita')).closest('li') as HTMLElement;
    expect(within(linha).getByText('No cardápio')).toBeInTheDocument();
    expect(within(linha).getByText('não vendável')).toBeInTheDocument();
  });

  it('o rascunho mostra "Rascunho" e não a marca de não vendável', async () => {
    responder(
      [PIZZAS],
      [produto('1', 'Pizza doce', 's-piz', 0, { vendavel: false, estadoDePublicacao: 'RASCUNHO' })],
    );

    montar();

    const linha = (await screen.findByText('Pizza doce')).closest('li') as HTMLElement;
    expect(within(linha).getByText('Rascunho')).toBeInTheDocument();
    expect(within(linha).queryByText('não vendável')).not.toBeInTheDocument();
  });

  it('estado de publicação desconhecido não ganha rótulo inventado', async () => {
    responder(
      [PIZZAS],
      [produto('1', 'Margherita', 's-piz', 0, { estadoDePublicacao: 'ARQUIVADO' })],
    );

    montar();

    const linha = (await screen.findByText('Margherita')).closest('li') as HTMLElement;
    expect(within(linha).queryByText(/Rascunho|No cardápio|Fora do cardápio/)).toBeNull();
  });

  it('cardápio vazio convida a criar a primeira seção', async () => {
    responder([], []);

    montar();

    expect(await screen.findByText(/Comece criando uma seção/)).toBeInTheDocument();
  });
});
