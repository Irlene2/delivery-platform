import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { App } from '../App';
import { ErroDaApi } from '../api/cliente';
import { guardar } from '../auth/armazenamento';
import { SessaoProvider } from '../auth/SessaoProvider';

vi.mock('../api/cliente', async () => {
  const real = await vi.importActual<typeof import('../api/cliente')>('../api/cliente');
  return { ...real, chamar: vi.fn() };
});

const { chamar } = await import('../api/cliente');
const chamarFalso = chamar as unknown as Mock;

beforeEach(() => {
  guardar({ token: 't.o.k', expiraEm: Date.now() + 1_800_000 });
});

afterEach(() => {
  chamarFalso.mockReset();
});

/** Mostra onde a navegação terminou — os casos que afirmam destino leem daqui. */
function OndeEstou() {
  return <p data-testid="onde-estou">{useLocation().pathname}</p>;
}

function montar() {
  render(
    <MemoryRouter initialEntries={['/painel/lojas/nova']}>
      <SessaoProvider>
        <App />
        <OndeEstou />
      </SessaoProvider>
    </MemoryRouter>,
  );
}

const LOJA_NOVA = {
  estabelecimentoId: 'loja-nova-7',
  nome: 'Cantina do Zé',
  papel: 'ADMINISTRADOR',
  permissoes: ['VER_PRODUTO', 'ALTERAR_PRODUTO'],
};

/** GET das lojas (vazio até criar), POST do cadastro (o que o caso mandar), e cardápio vazio — seções e produtos. */
function servidor(aoCriar: () => Promise<unknown>) {
  let criada = false;
  chamarFalso.mockImplementation((caminho: string, opcoes?: { metodo?: string }) => {
    if (opcoes?.metodo === 'POST') {
      return aoCriar().then((resposta) => {
        criada = true;
        return resposta;
      });
    }
    if (caminho === '/api/v1/me/estabelecimentos') {
      return Promise.resolve(criada ? [LOJA_NOVA] : []);
    }
    // O cardápio compõe as seções com os produtos desde a W-F (ADR-067).
    if (caminho.endsWith('/catalog/categorias')) return Promise.resolve([]);
    return Promise.resolve({ conteudo: [], pagina: 0, tamanho: 20, total: 0 });
  });
}

async function preencher() {
  await userEvent.type(await screen.findByLabelText('Nome da loja'), 'Cantina do Zé');
  await userEvent.type(screen.getByLabelText('CPF ou CNPJ'), '12.345.678/0001-95');
  await userEvent.type(screen.getByLabelText('Telefone da loja'), '(81) 98765-4321');
  await userEvent.type(screen.getByLabelText('Endereço'), 'Rua da Aurora, 10');
  await userEvent.type(screen.getByLabelText('Bairro'), 'Boa Vista');
  await userEvent.type(screen.getByLabelText('Fundo máximo de troco (R$)'), '50');
}

function marcar(modalidade: string, forma: string) {
  return userEvent.click(
    within(screen.getByRole('group', { name: `Formas de pagar na ${modalidade}` })).getByRole(
      'checkbox',
      { name: forma },
    ),
  );
}

function corpoEnviado(): unknown {
  const chamada = (chamarFalso.mock.calls as [string, { metodo?: string; corpo?: unknown }][]).find(
    ([, opcoes]) => opcoes?.metodo === 'POST',
  );
  return chamada?.[1].corpo;
}

describe('CadastroDeLojaPage', () => {
  it('A1 · os campos são o corpo da rota, e nada além — sem horário, sem área', async () => {
    servidor(() => Promise.resolve(LOJA_NOVA));

    montar();
    await screen.findByLabelText('Nome da loja');

    // Seis campos de texto, dois seletores, uma caixa de troco e a matriz 2 × 3.
    expect(screen.getAllByRole('textbox')).toHaveLength(6);
    expect(screen.getAllByRole('combobox')).toHaveLength(2);
    expect(screen.getAllByRole('checkbox')).toHaveLength(1 + 2 * 3);
    expect(screen.queryByLabelText(/horário de funcionamento/i)).toBeNull();
    expect(screen.queryByLabelText(/área/i)).toBeNull();
  });

  it('A2 · o fuso lista as 16 zonas do contrato, e já vem em São Paulo', async () => {
    servidor(() => Promise.resolve(LOJA_NOVA));

    montar();
    const fuso = await screen.findByRole('combobox', { name: 'Fuso horário' });

    expect(within(fuso).getAllByRole('option')).toHaveLength(16);
    expect(fuso).toHaveValue('America/Sao_Paulo');
    expect(within(fuso).getByRole('option', { name: 'Fernando de Noronha' })).toHaveValue(
      'America/Noronha',
    );
  });

  it('A3 · enviar manda exatamente o corpo do contrato', async () => {
    servidor(() => Promise.resolve(LOJA_NOVA));

    montar();
    await preencher();
    await marcar('entrega', 'Pix');
    await marcar('retirada', 'Dinheiro');
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar loja' }));

    await waitFor(() => expect(corpoEnviado()).toBeDefined());
    expect(corpoEnviado()).toEqual({
      identificacao: {
        nome: 'Cantina do Zé',
        documento: '12.345.678/0001-95',
        telefone: '(81) 98765-4321',
        enderecoTextual: 'Rua da Aurora, 10',
        bairro: 'Boa Vista',
        fusoHorario: 'America/Sao_Paulo',
      },
      politicaDeTroco: { fundoMaximoDeTroco: 50, aceitaPedidoSemTrocoDisponivel: false },
      tipoDeOperacao: 'PRODUCAO',
      metodosPorModalidade: { ENTREGA: ['PIX'], RETIRADA: ['DINHEIRO'] },
    });
  });

  it('A4 · o 201 leva ao cardápio da loja criada — pelo id da resposta', async () => {
    servidor(() => Promise.resolve(LOJA_NOVA));

    montar();
    await preencher();
    await marcar('retirada', 'Dinheiro');
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar loja' }));

    await waitFor(() =>
      expect(screen.getByTestId('onde-estou')).toHaveTextContent(
        '/painel/lojas/loja-nova-7/produtos',
      ),
    );
    // A lista do painel foi relida: a loja nova já está nela.
    expect(await screen.findByText('Cantina do Zé')).toBeInTheDocument();
  });

  it('A5 · o 400 mostra o detail que veio, e o formulário continua preenchido', async () => {
    servidor(() =>
      Promise.reject(new ErroDaApi(400, 'documento inválido: informe um CPF ou um CNPJ')),
    );

    montar();
    await preencher();
    await marcar('retirada', 'Dinheiro');
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar loja' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'documento inválido: informe um CPF ou um CNPJ',
    );
    expect(screen.getByLabelText('CPF ou CNPJ')).toHaveValue('12.345.678/0001-95');
    expect(screen.getByTestId('onde-estou')).toHaveTextContent('/painel/lojas/nova');
  });

  it('A6 · o 400 sem detail mostra a mensagem do cliente', async () => {
    servidor(() => Promise.reject(new ErroDaApi(400, undefined)));

    montar();
    await preencher();
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar loja' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'A loja não foi cadastrada. Confira os campos e tente de novo.',
    );
  });

  it('A7 · a tela não antecipa a M12: matriz vazia, botão habilitado, e a requisição acontece', async () => {
    servidor(() =>
      Promise.reject(
        new ErroDaApi(400, 'escolha ao menos uma modalidade, e uma forma de pagar para cada uma'),
      ),
    );

    montar();
    await preencher();
    const botao = screen.getByRole('button', { name: 'Cadastrar loja' });
    expect(botao).toBeEnabled();
    await userEvent.click(botao);

    await waitFor(() => expect(corpoEnviado()).toBeDefined());
    expect((corpoEnviado() as { metodosPorModalidade: unknown }).metodosPorModalidade).toEqual({});
    expect(await screen.findByRole('alert')).toHaveTextContent('escolha ao menos uma modalidade');
  });
});
