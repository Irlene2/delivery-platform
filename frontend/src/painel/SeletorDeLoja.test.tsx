import { render as renderizar, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter } from 'react-router';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { SeletorDeLoja } from './SeletorDeLoja';
import type { LojaDoUsuario } from './tipos';

function loja(nome: string, id: string): LojaDoUsuario {
  return {
    estabelecimentoId: id,
    nome,
    papel: 'COLABORADOR',
    permissoes: ['VER_PRODUTO'],
  };
}

/** O convite tem um `Link` para o cadastro desde a W-E, e link precisa de roteador. */
function render(ui: ReactElement) {
  return renderizar(ui, { wrapper: MemoryRouter });
}

describe('SeletorDeLoja', () => {
  /**
   * O estado de todo mundo no instante seguinte ao cadastro — e a primeira
   * tela que o produto mostra a essa pessoa. Se aqui aparecer "nenhum
   * resultado", o começo parece um erro.
   */
  it('sem loja nenhuma, convida em vez de dizer vazio', () => {
    render(<SeletorDeLoja lojas={[]} selecionada={null} aoSelecionar={() => {}} />);

    expect(screen.getByText(/ainda não faz parte de nenhuma loja/i)).toBeInTheDocument();
    expect(screen.getByText(/precisa te convidar/i)).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  // W-E: o convite continua dizendo o que vale para os quatro casos de lista vazia, e
  // ganha o caminho de quem é dono — sem redirecionar ninguém (ADR-061 §3).
  it('o convite leva ao cadastro de loja por um link', () => {
    render(<SeletorDeLoja lojas={[]} selecionada={null} aoSelecionar={() => {}} />);

    expect(screen.getByRole('link', { name: 'Cadastrar minha loja' })).toHaveAttribute(
      'href',
      '/painel/lojas/nova',
    );
  });

  it('com duas lojas e nenhuma escolhida, nenhuma aparece marcada', () => {
    render(
      <SeletorDeLoja
        lojas={[loja('Ana Pães', 'a1'), loja('Pizzaria', 'p1')]}
        selecionada={null}
        aoSelecionar={() => {}}
      />,
    );

    expect(screen.getByRole('combobox', { name: /loja/i })).toHaveValue('');
  });

  it('com uma loja só, mostra o nome e não desenha seletor', () => {
    render(
      <SeletorDeLoja
        lojas={[loja('Pizzaria da Marli', 'p1')]}
        selecionada="p1"
        aoSelecionar={() => {}}
      />,
    );

    expect(screen.getByText('Pizzaria da Marli')).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('o papel aparece em português, e não como valor de enum', () => {
    render(
      <SeletorDeLoja
        lojas={[{ ...loja('Pizzaria', 'p1'), papel: 'ADMINISTRADOR' }]}
        selecionada="p1"
        aoSelecionar={() => {}}
      />,
    );

    expect(screen.getByText(/você administra esta loja/i)).toBeInTheDocument();
    expect(screen.queryByText('ADMINISTRADOR')).not.toBeInTheDocument();
  });

  it('com duas lojas, desenha o seletor com as duas', () => {
    render(
      <SeletorDeLoja
        lojas={[loja('Ana Pães', 'a1'), loja('Pizzaria', 'p1')]}
        selecionada="a1"
        aoSelecionar={() => {}}
      />,
    );

    const seletor = screen.getByRole('combobox', { name: /loja/i });
    expect(seletor).toHaveValue('a1');
    expect(screen.getAllByRole('option')).toHaveLength(2);
  });

  it('escolher uma loja avisa o identificador, e não o nome', async () => {
    const aoSelecionar = vi.fn();
    render(
      <SeletorDeLoja
        lojas={[loja('Ana Pães', 'a1'), loja('Pizzaria', 'p1')]}
        selecionada="a1"
        aoSelecionar={aoSelecionar}
      />,
    );

    await userEvent.selectOptions(screen.getByRole('combobox', { name: /loja/i }), 'p1');

    expect(aoSelecionar).toHaveBeenCalledWith('p1');
  });
});
