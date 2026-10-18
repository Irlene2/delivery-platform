import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

import { ErroDaApi } from '../api/cliente';
import { guardar } from '../auth/armazenamento';
import { SessaoProvider } from '../auth/SessaoProvider';
import { FRASE_DO_VAZIO, HorarioDaSemana } from './HorarioDaSemana';

vi.mock('../api/cliente', async () => {
  const real = await vi.importActual<typeof import('../api/cliente')>('../api/cliente');
  return { ...real, chamar: vi.fn() };
});

// `chamar` é genérica, e `vi.mocked` de função genérica briga com o typecheck.
const { chamar } = await import('../api/cliente');
const chamarFalso = chamar as unknown as Mock;

const CAMINHO = '/api/v1/merchants/p1/settings/horario';
const SEXTA = { horarioDeFuncionamento: { FRIDAY: [{ inicio: '18:00', fim: '23:00' }] } };
const VAZIO = { horarioDeFuncionamento: {} };

type Chamada = [string, { metodo?: string; corpo?: unknown; token?: string } | undefined];

beforeEach(() => {
  guardar({ token: 't.o.k', expiraEm: Date.now() + 1_800_000 });
});

afterEach(() => {
  // A desmontagem antes do reset: um efeito pendente da tela que sai não pode cair no
  // dublê já zerado do teste seguinte (medido na W-G).
  cleanup();
  chamarFalso.mockReset();
});

/** O GET devolve `lido`; o PUT, o que `gravar` decidir. */
function servidor(lido: unknown, gravar?: (corpo: unknown) => Promise<unknown>) {
  chamarFalso.mockImplementation(
    (_caminho: string, opcoes?: { metodo?: string; corpo?: unknown }) =>
      opcoes?.metodo === 'PUT'
        ? (gravar ?? ((corpo) => Promise.resolve(corpo)))(opcoes.corpo)
        : Promise.resolve(lido),
  );
}

function montar() {
  return render(
    <SessaoProvider>
      <HorarioDaSemana estabelecimentoId="p1" />
    </SessaoProvider>,
  );
}

function puts(): Chamada[] {
  return (chamarFalso.mock.calls as Chamada[]).filter(([, opcoes]) => opcoes?.metodo === 'PUT');
}

function campo(rotulo: string): HTMLInputElement {
  return screen.getByLabelText<HTMLInputElement>(rotulo);
}

function digitar(rotulo: string, valor: string) {
  fireEvent.change(campo(rotulo), { target: { value: valor } });
}

async function salvar() {
  await userEvent.click(screen.getByRole('button', { name: 'Salvar horário' }));
}

describe('HorarioDaSemana · A, a tela monta', () => {
  it('1 · mostra os sete dias, na ordem da semana, mesmo os sem faixa', async () => {
    servidor(SEXTA);
    montar();

    await screen.findByRole('button', { name: 'Salvar horário' });
    expect(
      screen.getAllByRole('group').map((dia) => dia.querySelector('legend')?.textContent),
    ).toEqual([
      'Segunda-feira',
      'Terça-feira',
      'Quarta-feira',
      'Quinta-feira',
      'Sexta-feira',
      'Sábado',
      'Domingo',
    ]);
  });

  it('2 · mostra as faixas que a leitura devolveu, no dia certo', async () => {
    servidor({
      horarioDeFuncionamento: {
        MONDAY: [{ inicio: '11:00:00', fim: '14:00:00' }],
        FRIDAY: [{ inicio: '18:00', fim: '23:00' }],
      },
    });
    montar();

    expect(
      await screen.findByLabelText<HTMLInputElement>('Segunda-feira, faixa 1: abre às'),
    ).toHaveValue('11:00');
    expect(campo('Segunda-feira, faixa 1: fecha às')).toHaveValue('14:00');
    expect(campo('Sexta-feira, faixa 1: abre às')).toHaveValue('18:00');
    expect(campo('Sexta-feira, faixa 1: fecha às')).toHaveValue('23:00');
    expect(screen.queryByLabelText('Terça-feira, faixa 1: abre às')).toBeNull();
    expect(chamarFalso.mock.calls[0]?.[0]).toBe(CAMINHO);
  });

  it('3 · loja sem horário: a frase, e nenhuma faixa', async () => {
    servidor(VAZIO);
    montar();

    expect(await screen.findByText('Esta loja não abre por horário.')).toBeInTheDocument();
    expect(screen.queryAllByLabelText(/abre às/)).toHaveLength(0);
  });

  it('4 · carregando não é vazio: nem a frase do vazio, nem o formulário', () => {
    chamarFalso.mockReturnValue(new Promise(() => {}));
    montar();

    expect(screen.getByText('Carregando o horário…')).toBeInTheDocument();
    expect(screen.queryByText('Esta loja não abre por horário.')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Salvar horário' })).toBeNull();
  });

  it('5 · erro na leitura: a frase do servidor, e nenhum formulário', async () => {
    chamarFalso.mockRejectedValue(new ErroDaApi(500, 'o servidor tropeçou'));
    montar();

    expect((await screen.findByRole('alert')).textContent).toBe('o servidor tropeçou');
    expect(screen.queryByRole('button', { name: 'Salvar horário' })).toBeNull();
    expect(screen.queryByText('Esta loja não abre por horário.')).toBeNull();
  });
});

describe('HorarioDaSemana · B, editar', () => {
  it('6 · acrescentar faixa num dia: só aquele dia ganhou', async () => {
    servidor(VAZIO);
    montar();

    await userEvent.click(
      await screen.findByRole('button', { name: 'Acrescentar faixa em Segunda-feira' }),
    );

    expect(campo('Segunda-feira, faixa 1: abre às')).toBeInTheDocument();
    expect(screen.queryAllByLabelText(/abre às/)).toHaveLength(1);
  });

  it('7 · remover a última faixa de um dia deixa o dia sem faixa, e o corpo sem o dia', async () => {
    servidor({
      horarioDeFuncionamento: {
        MONDAY: [{ inicio: '11:00', fim: '14:00' }],
        FRIDAY: [{ inicio: '18:00', fim: '23:00' }],
      },
    });
    montar();

    await userEvent.click(
      await screen.findByRole('button', { name: 'Remover a faixa 1 de Sexta-feira' }),
    );
    expect(screen.queryByLabelText('Sexta-feira, faixa 1: abre às')).toBeNull();

    await salvar();
    await waitFor(() => expect(puts()).toHaveLength(1));
    expect(puts()[0]?.[1]?.corpo).toEqual({
      horarioDeFuncionamento: { MONDAY: [{ inicio: '11:00', fim: '14:00' }] },
    });
  });

  it('8 · fim antes do início é aceito e nomeado: cruza a meia-noite, sem erro', async () => {
    servidor(VAZIO);
    montar();
    await userEvent.click(
      await screen.findByRole('button', { name: 'Acrescentar faixa em Terça-feira' }),
    );

    digitar('Terça-feira, faixa 1: abre às', '18:00');
    digitar('Terça-feira, faixa 1: fecha às', '02:00');

    expect(screen.getByText('Cruza a meia-noite: fecha no dia seguinte.')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).toBeNull();
    await salvar();
    await waitFor(() => expect(puts()).toHaveLength(1));
    expect(screen.getByText('Horário salvo.')).toBeInTheDocument();
  });

  it('9 · faixas sobrepostas são aceitas, sem aviso de erro', async () => {
    servidor(VAZIO);
    montar();
    const acrescentar = await screen.findByRole('button', {
      name: 'Acrescentar faixa em Segunda-feira',
    });
    await userEvent.click(acrescentar);
    await userEvent.click(acrescentar);

    digitar('Segunda-feira, faixa 1: abre às', '11:00');
    digitar('Segunda-feira, faixa 1: fecha às', '15:00');
    digitar('Segunda-feira, faixa 2: abre às', '14:00');
    digitar('Segunda-feira, faixa 2: fecha às', '18:00');
    await salvar();

    await waitFor(() => expect(puts()).toHaveLength(1));
    expect(screen.queryAllByRole('alert').map((aviso) => aviso.textContent)).toEqual([
      'Horário salvo.',
    ]);
  });

  it('10 · início igual ao fim não é barrado pela tela: ela manda, e mostra a recusa', async () => {
    const frase = 'uma faixa de horário não pode terminar na mesma hora em que começa';
    servidor(VAZIO, () => Promise.reject(new ErroDaApi(400, frase)));
    montar();
    await userEvent.click(
      await screen.findByRole('button', { name: 'Acrescentar faixa em Segunda-feira' }),
    );

    digitar('Segunda-feira, faixa 1: abre às', '10:00');
    digitar('Segunda-feira, faixa 1: fecha às', '10:00');
    await salvar();

    await waitFor(() => expect(puts()).toHaveLength(1));
    expect((await screen.findByRole('alert')).textContent).toBe(frase);
  });
});

describe('HorarioDaSemana · C, salvar', () => {
  it('11 e 12 · manda o corpo do contrato, e só os dias com faixa', async () => {
    servidor(SEXTA);
    montar();
    await userEvent.click(
      await screen.findByRole('button', { name: 'Acrescentar faixa em Sábado' }),
    );
    digitar('Sábado, faixa 1: abre às', '11:00');
    digitar('Sábado, faixa 1: fecha às', '15:00');

    await salvar();

    await waitFor(() => expect(puts()).toHaveLength(1));
    const [caminho, opcoes] = puts()[0] as Chamada;
    expect(caminho).toBe(CAMINHO);
    expect(opcoes).toEqual({
      metodo: 'PUT',
      token: 't.o.k',
      corpo: {
        horarioDeFuncionamento: {
          FRIDAY: [{ inicio: '18:00', fim: '23:00' }],
          SATURDAY: [{ inicio: '11:00', fim: '15:00' }],
        },
      },
    });
  });

  it('13 · salvar sem nenhuma faixa pede confirmação, com a frase da consequência', async () => {
    servidor(SEXTA);
    montar();
    await userEvent.click(
      await screen.findByRole('button', { name: 'Remover a faixa 1 de Sexta-feira' }),
    );

    await salvar();

    expect(screen.getByText(FRASE_DO_VAZIO)).toBeInTheDocument();
    expect(puts()).toHaveLength(0);

    await userEvent.click(screen.getByRole('button', { name: 'Salvar sem horário' }));
    await waitFor(() => expect(puts()).toHaveLength(1));
    expect(puts()[0]?.[1]?.corpo).toEqual({ horarioDeFuncionamento: {} });
  });

  it('14 · depois de salvar, a tela mostra o que voltou — não o que foi digitado', async () => {
    // O servidor normaliza: devolve só a sexta, e a segunda que foi digitada some.
    servidor(VAZIO, () => Promise.resolve(SEXTA));
    montar();
    await userEvent.click(
      await screen.findByRole('button', { name: 'Acrescentar faixa em Segunda-feira' }),
    );
    digitar('Segunda-feira, faixa 1: abre às', '11:00');
    digitar('Segunda-feira, faixa 1: fecha às', '14:00');

    await salvar();

    await screen.findByText('Horário salvo.');
    expect(screen.queryByLabelText('Segunda-feira, faixa 1: abre às')).toBeNull();
    expect(campo('Sexta-feira, faixa 1: abre às')).toHaveValue('18:00');
  });

  it('15 · a recusa do 400 vai à tela como veio', async () => {
    const frase = 'a mesma faixa de horário aparece duas vezes no mesmo dia';
    servidor(SEXTA, () => Promise.reject(new ErroDaApi(400, frase)));
    montar();
    await screen.findByRole('button', { name: 'Salvar horário' });

    await salvar();

    expect((await screen.findByRole('alert')).textContent).toBe(frase);
  });

  it('16 · 403 na escrita mostra a frase, e o que foi digitado não se perde', async () => {
    servidor(SEXTA, () => Promise.reject(new ErroDaApi(403, 'sem acesso a este estabelecimento')));
    montar();
    await screen.findByLabelText('Sexta-feira, faixa 1: fecha às');
    digitar('Sexta-feira, faixa 1: fecha às', '22:30');

    await salvar();

    expect((await screen.findByRole('alert')).textContent).toBe(
      'Você não tem permissão para mudar o horário desta loja.',
    );
    expect(campo('Sexta-feira, faixa 1: fecha às')).toHaveValue('22:30');
    expect(screen.queryByText('Horário salvo.')).toBeNull();
  });
});
