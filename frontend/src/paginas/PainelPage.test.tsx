import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { guardar } from '../auth/armazenamento';
import { SessaoProvider } from '../auth/SessaoProvider';
import { PainelPage } from './PainelPage';

vi.mock('../api/cliente', async () => {
  const real = await vi.importActual<typeof import('../api/cliente')>('../api/cliente');
  return { ...real, chamar: vi.fn() };
});

// `chamar` é genérica, e `vi.mocked` de função genérica briga com o typecheck.
// O elenco para `Mock` é o que compila — e o que este arquivo precisa é da
// interface do dublê, não do tipo de retorno da função real.
const { chamar } = await import('../api/cliente');
const chamarFalso = chamar as unknown as Mock;

beforeEach(() => {
  guardar({ token: 't.o.k', expiraEm: Date.now() + 1_800_000 });
});

afterEach(() => {
  chamarFalso.mockReset();
});

function montar() {
  render(
    <SessaoProvider>
      <PainelPage />
    </SessaoProvider>,
  );
}

const PIZZARIA = {
  estabelecimentoId: 'p1',
  nome: 'Pizzaria da Marli',
  papel: 'ADMINISTRADOR',
  permissoes: ['VER_PRODUTO', 'GERENCIAR_EQUIPE'],
};

/** A Bia atende: vê pedido e mexe em status. **Não** vê produto. */
const PADARIA = {
  estabelecimentoId: 'a1',
  nome: 'Ana Pães',
  papel: 'COLABORADOR',
  permissoes: ['VER_PEDIDO', 'ALTERAR_STATUS'],
};

function responder(lojas: unknown[], produtos: unknown[] = []) {
  chamarFalso.mockImplementation((caminho: string) =>
    caminho === '/api/v1/me/estabelecimentos'
      ? Promise.resolve(lojas)
      : Promise.resolve({ conteudo: produtos, pagina: 0, tamanho: 20, total: produtos.length }),
  );
}

describe('PainelPage', () => {
  it('sem vínculo nenhum, mostra o convite e não busca produto', async () => {
    responder([]);

    montar();

    expect(await screen.findByText(/ainda não faz parte de nenhuma loja/i)).toBeInTheDocument();
    expect(chamarFalso).toHaveBeenCalledTimes(1);
  });

  it('com uma loja, escolhe sozinho e já pede o cardápio dela', async () => {
    responder([PIZZARIA]);

    montar();

    await waitFor(() =>
      expect(chamarFalso).toHaveBeenCalledWith(
        '/api/v1/merchants/p1/catalog/produtos?page=0&size=20',
        { token: 't.o.k' },
      ),
    );
    expect(screen.getByText('Pizzaria da Marli')).toBeInTheDocument();
  });

  /**
   * O caso que justifica a derivação em vez de um `useEffect`: a Bia não tem
   * `VER_PRODUTO` na padaria, então a seção "cardapio" que estava aberta na
   * pizzaria **não pode continuar aberta** depois da troca. Com estado bruto,
   * a tela pediria um cardápio que ela não pode ver e mostraria um 403.
   */
  it('trocar para uma loja sem VER_PRODUTO fecha a seção do cardápio', async () => {
    responder([PADARIA, PIZZARIA]);

    montar();

    await screen.findByRole('combobox', { name: /loja/i });
    await userEvent.selectOptions(screen.getByRole('combobox', { name: /loja/i }), 'p1');
    await screen.findByRole('button', { name: 'Cardápio' });

    await userEvent.selectOptions(screen.getByRole('combobox', { name: /loja/i }), 'a1');

    expect(screen.queryByRole('button', { name: 'Cardápio' })).not.toBeInTheDocument();
    expect(screen.getByText(/ainda não tem nenhuma permissão com tela/i)).toBeInTheDocument();
  });

  it('o menu muda quando a loja muda — é a mesma pessoa com outro vínculo', async () => {
    responder([PADARIA, PIZZARIA]);

    montar();

    await screen.findByRole('combobox', { name: /loja/i });
    expect(screen.queryByRole('button', { name: 'Cardápio' })).not.toBeInTheDocument();

    await userEvent.selectOptions(screen.getByRole('combobox', { name: /loja/i }), 'p1');

    expect(await screen.findByRole('button', { name: 'Cardápio' })).toBeInTheDocument();
  });

  it('a sessão e o botão de sair, que vieram da W-A, continuam no cabeçalho', async () => {
    responder([]);

    montar();

    expect(await screen.findByText(/expira em aproximadamente 30 min/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Sair' }));
    expect(sessionStorage.getItem('delivery.sessao')).toBeNull();
  });

  it('erro ao listar as lojas não vira tela em branco', async () => {
    chamarFalso.mockRejectedValue(new Error('rede'));

    montar();

    expect(await screen.findByRole('alert')).toBeInTheDocument();
  });
});
