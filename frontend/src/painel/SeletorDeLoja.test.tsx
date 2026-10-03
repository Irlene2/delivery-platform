import { render, screen } from '@testing-library/react';
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
