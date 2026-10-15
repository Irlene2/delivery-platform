import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ErroDaApi } from '../api/cliente';
import { guardar } from '../auth/armazenamento';
import { SessaoProvider } from '../auth/SessaoProvider';
import { NovoProduto } from './NovoProduto';

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

const PIZZAS = { id: 's-piz', nome: 'Pizzas', ordem: 0, ativa: true };
const BEBIDAS = { id: 's-beb', nome: 'Bebidas', ordem: 1, ativa: true };

/** Pizzas tem produtos até a ordem 7; Bebidas, até a 2. */
const PRODUTOS = [
  { id: '1', nome: 'Margherita', categoriaId: 's-piz', ordem: 7, precoBase: 49.9 },
  { id: '2', nome: 'Calabresa', categoriaId: 's-piz', ordem: 3, precoBase: 52 },
  { id: '3', nome: 'Guaraná', categoriaId: 's-beb', ordem: 2, precoBase: 6 },
];

type Opcoes = { metodo?: string; corpo?: unknown } | undefined;

function servidor(secoes: unknown[], aoCriar: () => Promise<unknown> = () => Promise.resolve({})) {
  chamarFalso.mockImplementation((caminho: string, opcoes: Opcoes) => {
    if (opcoes?.metodo === 'POST') return aoCriar();
    if (caminho.endsWith('/catalog/categorias')) return Promise.resolve(secoes);
    if (caminho.includes('/catalog/produtos?')) {
      return Promise.resolve({ conteudo: PRODUTOS, pagina: 0, tamanho: 100, total: 3 });
    }
    return Promise.reject(new Error(`caminho inesperado: ${caminho}`));
  });
}

function corpoEnviado(): Record<string, unknown> | undefined {
  const post = (chamarFalso.mock.calls as [string, Opcoes][]).find(
    ([, opcoes]) => opcoes?.metodo === 'POST',
  );
  return post?.[1]?.corpo as Record<string, unknown> | undefined;
}

function montar() {
  const aoCriar = vi.fn();
  const aoVoltar = vi.fn();
  render(
    <SessaoProvider>
      <NovoProduto estabelecimentoId="p1" aoCriar={aoCriar} aoVoltar={aoVoltar} />
    </SessaoProvider>,
  );
  return { aoCriar, aoVoltar };
}

async function preencher(nome = 'Coca-Cola lata', preco = '6,00') {
  await userEvent.type(await screen.findByLabelText('Nome'), nome);
  await userEvent.type(screen.getByLabelText('Preço'), preco);
}

describe('NovoProduto', () => {
  it('C9 · o seletor de seção é alimentado pela lista de seções, pelos nomes', async () => {
    servidor([PIZZAS, BEBIDAS]);

    montar();
    const secao = await screen.findByRole('combobox', { name: 'Seção' });

    expect(
      within(secao)
        .getAllByRole('option')
        .map((opcao) => opcao.textContent),
    ).toEqual(['Pizzas', 'Bebidas']);
  });

  /** A segunda seção, cada uma com produtos de ordem diferente: um 0 fixo não passa. */
  it('C10 · a ordem enviada é a maior da seção escolhida, mais um', async () => {
    servidor([PIZZAS, BEBIDAS]);

    montar();
    await userEvent.selectOptions(await screen.findByRole('combobox', { name: 'Seção' }), 's-beb');
    await preencher();
    await userEvent.click(screen.getByRole('button', { name: 'Criar produto' }));

    await waitFor(() => expect(corpoEnviado()).toBeDefined());
    expect(corpoEnviado()).toEqual({
      categoriaId: 's-beb',
      nome: 'Coca-Cola lata',
      precoBase: 6,
      modoDeControle: 'QUALITATIVO',
      ordem: 3,
    });
  });

  it('C11 · não há campo de ordem na tela', async () => {
    servidor([PIZZAS]);

    montar();
    await screen.findByLabelText('Nome');

    expect(screen.queryByLabelText(/ordem/i)).toBeNull();
    expect(screen.getAllByRole('textbox')).toHaveLength(3);
  });

  it('C12 · o 201 volta ao cardápio', async () => {
    servidor([PIZZAS], () => Promise.resolve({ id: '9', estadoDePublicacao: 'RASCUNHO' }));

    const { aoCriar } = montar();
    await preencher();
    await userEvent.click(screen.getByRole('button', { name: 'Criar produto' }));

    await waitFor(() => expect(aoCriar).toHaveBeenCalled());
  });

  it('C13 · a recusa do servidor vai à tela como está', async () => {
    const frase =
      'a seção escolhida não está no cardápio desta loja; escolha uma das seções da lista';
    servidor([PIZZAS], () => Promise.reject(new ErroDaApi(400, frase)));

    montar();
    await preencher();
    await userEvent.click(screen.getByRole('button', { name: 'Criar produto' }));

    expect((await screen.findByRole('alert')).textContent).toBe(frase);
  });

  it('C14 · a loja sem seção não é um beco: diz o que falta e oferece a volta', async () => {
    servidor([]);

    const { aoVoltar } = montar();

    expect(await screen.findByText(/não tem nenhuma/)).toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: 'Seção' })).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Voltar ao cardápio' }));
    expect(aoVoltar).toHaveBeenCalled();
  });

  it('C18 · a mensagem de erro sai quando o campo muda', async () => {
    servidor([PIZZAS], () =>
      Promise.reject(new ErroDaApi(400, 'o preço do produto não pode ser negativo')),
    );

    montar();
    await preencher('Água', '-1');
    await userEvent.click(screen.getByRole('button', { name: 'Criar produto' }));
    expect(await screen.findByText(/não pode ser negativo/)).toBeInTheDocument();

    await userEvent.clear(screen.getByLabelText('Preço'));
    await userEvent.type(screen.getByLabelText('Preço'), '2');

    expect(screen.queryByText(/não pode ser negativo/)).not.toBeInTheDocument();
  });

  it('a descrição vai quando preenchida', async () => {
    servidor([PIZZAS]);

    montar();
    await preencher();
    await userEvent.type(screen.getByLabelText('Descrição (opcional)'), '350 ml');
    await userEvent.click(screen.getByRole('button', { name: 'Criar produto' }));

    await waitFor(() => expect(corpoEnviado()).toBeDefined());
    expect(corpoEnviado()?.descricao).toBe('350 ml');
  });
});
