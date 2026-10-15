import { cleanup, render, screen, waitFor } from '@testing-library/react';
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

// `chamar` é genérica, e `vi.mocked` de função genérica briga com o typecheck.
const { chamar } = await import('../api/cliente');
const chamarFalso = chamar as unknown as Mock;

const TOKEN = 'tok_DoConvite-123';
const CONVITE = `/convite/${TOKEN}`;
const PUBLICO = {
  nomeDaLoja: 'Pizzaria da Marli',
  papel: 'COLABORADOR',
  expiraEm: '2026-10-22T15:00:00Z',
};
const LOJA_NOVA = {
  estabelecimentoId: 'p1',
  nome: 'Pizzaria da Marli',
  papel: 'COLABORADOR',
  permissoes: ['VER_PEDIDO'],
};

/** Desmonta antes de zerar o dublê — a lição da W-F no CadastroDeLojaPage.test. */
afterEach(() => {
  cleanup();
  chamarFalso.mockReset();
});

beforeEach(() => {
  sessionStorage.clear();
});

function OndeEstou() {
  return <p data-testid="onde-estou">{useLocation().pathname}</p>;
}

function onde() {
  return screen.getByTestId('onde-estou');
}

function montar(endereco = CONVITE) {
  return render(
    <MemoryRouter initialEntries={[endereco]}>
      <SessaoProvider>
        <App />
        <OndeEstou />
      </SessaoProvider>
    </MemoryRouter>,
  );
}

function comSessao() {
  guardar({ token: 't.o.k', expiraEm: Date.now() + 1_800_000 });
}

type Opcoes = { metodo?: string; token?: string } | undefined;

function caminhosPedidos(): string[] {
  return (chamarFalso.mock.calls as [string, Opcoes][]).map(([caminho]) => caminho);
}

/** O dublê: a leitura pública, o aceite, o login e as lojas — cada um com a sua resposta. */
function servidor({
  leitura = () => Promise.resolve(PUBLICO),
  aceite = () => Promise.resolve({ estabelecimentoId: 'p1', papel: 'COLABORADOR', permissoes: [] }),
}: {
  leitura?: () => Promise<unknown>;
  aceite?: () => Promise<unknown>;
} = {}) {
  let aceitou = false;
  chamarFalso.mockImplementation((caminho: string, opcoes: Opcoes) => {
    if (caminho === `/api/v1/convites/${TOKEN}`) return leitura();
    if (caminho === `/api/v1/convites/${TOKEN}/aceite`) {
      return aceite().then((r) => {
        aceitou = true;
        return r;
      });
    }
    if (caminho === '/api/v1/auth/verification-code' || caminho === '/api/v1/auth/signup') {
      return Promise.resolve(undefined);
    }
    if (caminho === '/api/v1/auth/login') {
      return Promise.resolve({ accessToken: 'n.o.v', tokenType: 'Bearer', expiresIn: 1800 });
    }
    if (caminho === '/api/v1/me/estabelecimentos') {
      return Promise.resolve(aceitou ? [LOJA_NOVA] : []);
    }
    if (caminho.includes('/catalog/')) {
      return Promise.resolve(caminho.endsWith('/categorias') ? [] : { conteudo: [], total: 0 });
    }
    void opcoes;
    return Promise.reject(new Error(`caminho inesperado: ${caminho}`));
  });
}

describe('ConvitePage', () => {
  it('B7 · monta sem sessão e o único caminho pedido é o do convite, sem token', async () => {
    servidor();

    montar();

    await screen.findByTestId('nome-da-loja');
    expect(caminhosPedidos()).toEqual([`/api/v1/convites/${TOKEN}`]);
    expect((chamarFalso.mock.calls[0] as [string, Opcoes])[1]).toBeUndefined();
    expect(onde()).toHaveTextContent(CONVITE);
  });

  it('B8 · mostra o que o GET devolveu: a loja, o papel e a validade', async () => {
    servidor();

    montar();

    expect(await screen.findByTestId('nome-da-loja')).toHaveTextContent('Pizzaria da Marli');
    expect(screen.getByText('Você entra como colaborador.')).toBeInTheDocument();
    expect(screen.getByText(/Este convite vale até .*2026/)).toBeInTheDocument();
  });

  /**
   * O que não veio não aparece. O papel **veio** — a rota pública o devolve, sempre
   * COLABORADOR (medido na W-G, ADR-070 §4 emendada); o caso B9 da especificação, que
   * mandava afirmar a ausência dele, cai diante da medição.
   */
  it('B9 · e não mostra o que não veio: telefone, quem convidou, o token', async () => {
    servidor();

    montar();
    await screen.findByTestId('nome-da-loja');

    const tela = document.body.textContent ?? '';
    expect(tela).not.toMatch(/\(\d{2}\)|\+55/);
    expect(tela).not.toMatch(/convidou|Marli te/);
    expect(tela.replace(CONVITE, '')).not.toContain(TOKEN);
  });

  it('B10 · sem sessão, oferece entrar e criar conta, e não aceitar', async () => {
    servidor();

    montar();
    await screen.findByTestId('nome-da-loja');

    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Criar conta' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Aceitar convite' })).toBeNull();
  });

  it('B11 · com sessão, oferece aceitar, e não entrar', async () => {
    comSessao();
    servidor();

    montar();
    await screen.findByTestId('nome-da-loja');

    expect(screen.getByRole('button', { name: 'Aceitar convite' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Entrar' })).toBeNull();
  });

  it('B12 · o 404 mostra a frase do servidor, igual, e sem o token na tela', async () => {
    const frase = 'este convite não vale mais; peça outro a quem te convidou';
    servidor({ leitura: () => Promise.reject(new ErroDaApi(404, frase)) });

    montar();

    expect((await screen.findByRole('alert')).textContent).toBe(frase);
    expect((document.body.textContent ?? '').replace(CONVITE, '')).not.toContain(TOKEN);
  });

  it('C13 · entrar leva o destino, e o destino é esta URL, com o token', async () => {
    servidor();

    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(onde()).toHaveTextContent('/entrar'));
    // O destino viaja no state do roteador, como o ExigeSessao já fazia: o login, ao
    // terminar, o lê — e o C14 prova que ele foi lido, não só escrito.
  });

  /** O caminho inteiro: sem ele, o teste prova que o destino foi escrito, e não lido. */
  it('C14 · voltar do login cai de novo na página do convite', async () => {
    servidor();

    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Entrar' }));
    await userEvent.type(await screen.findByLabelText(/telefone/i), '(81) 98765-4321');
    await userEvent.type(screen.getByLabelText(/senha/i), 'segredo123');
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => expect(onde()).toHaveTextContent(CONVITE));
    expect(await screen.findByRole('button', { name: 'Aceitar convite' })).toBeInTheDocument();
  });

  it('criar conta também leva o destino: o link do login para o cadastro o repassa', async () => {
    servidor();

    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Entrar' }));
    await userEvent.click(await screen.findByRole('link', { name: 'Criar conta' }));

    await waitFor(() => expect(onde()).toHaveTextContent('/cadastrar'));
  });

  /**
   * O cadastro também volta: até a W-G ele navegava sempre para o /painel, e passou a
   * honrar o mesmo state.de. O caminho inteiro — convite, criar conta, código, dados —,
   * porque o destino escrito e não lido passaria por um teste mais curto.
   */
  it('criar conta pelo convite volta para a página do convite', async () => {
    servidor();

    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Criar conta' }));
    await userEvent.type(await screen.findByLabelText('Telefone'), '(81) 98765-4321');
    await userEvent.click(screen.getByRole('button', { name: 'Pedir código' }));
    await userEvent.type(await screen.findByLabelText('Código de verificação'), '123456');
    await userEvent.type(screen.getByLabelText('Seu nome'), 'Bia');
    await userEvent.type(screen.getByLabelText('Senha'), 'segredo123');
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta e entrar' }));

    await waitFor(() => expect(onde()).toHaveTextContent(CONVITE));
    expect(await screen.findByRole('button', { name: 'Aceitar convite' })).toBeInTheDocument();
  });

  /** O dublê devolve as lojas sem a nova antes do aceite, e com ela depois. */
  it('C15 · aceitar leva ao painel, e a loja nova está na lista — relida', async () => {
    comSessao();
    servidor();

    montar();
    await userEvent.click(await screen.findByRole('button', { name: 'Aceitar convite' }));

    await waitFor(() => expect(onde()).toHaveTextContent('/painel/lojas/p1'));
    expect(await screen.findByText('Pizzaria da Marli')).toBeInTheDocument();
    expect(caminhosPedidos()).toContain('/api/v1/me/estabelecimentos');
  });

  it('C16 · as duas recusas de 409 vão à tela como vieram, e oferecem o painel', async () => {
    const frases = [
      'quem te convidou não pode mais conceder este acesso; peça um novo convite',
      'você já faz parte da equipe desta loja',
    ];
    for (const frase of frases) {
      comSessao();
      servidor({ aceite: () => Promise.reject(new ErroDaApi(409, frase)) });

      const montada = montar();
      await userEvent.click(await screen.findByRole('button', { name: 'Aceitar convite' }));

      expect((await screen.findByRole('alert')).textContent).toBe(frase);
      expect(screen.getByRole('link', { name: 'Ir para o painel' })).toHaveAttribute(
        'href',
        '/painel',
      );
      montada.unmount();
      chamarFalso.mockReset();
    }
  });
});
