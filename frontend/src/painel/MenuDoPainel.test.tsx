import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { MenuDoPainel } from './MenuDoPainel';
import { secoesDe } from './secoes';
import type { Permissao } from './tipos';

function montar(permissoes: Permissao[], aoEscolher = vi.fn()) {
  render(<MenuDoPainel permissoes={permissoes} atual="cardapio" aoEscolher={aoEscolher} />);
  return aoEscolher;
}

describe('MenuDoPainel', () => {
  /**
   * O caso que prova que o menu vem do servidor, e não de um perfil escolhido
   * no cliente. Sem ele, um menu fixo passaria em todos os outros deste arquivo.
   */
  it('quem não tem VER_PRODUTO não vê o Cardápio', () => {
    montar(['VER_PEDIDO', 'ALTERAR_STATUS']);

    expect(screen.queryByRole('button', { name: 'Cardápio' })).not.toBeInTheDocument();
  });

  it('quem tem VER_PRODUTO vê o Cardápio', () => {
    montar(['VER_PRODUTO']);

    expect(screen.getByRole('button', { name: 'Cardápio' })).toBeInTheDocument();
  });

  it('permissão sem tela não vira item de menu', () => {
    montar(['GERENCIAR_EQUIPE', 'VER_VENDAS', 'VER_ENTREGA']);

    expect(screen.queryAllByRole('button')).toHaveLength(0);
    expect(screen.getByText(/ainda não tem nenhuma permissão com tela/i)).toBeInTheDocument();
  });

  it('sem permissão nenhuma, diz isso em vez de um menu vazio', () => {
    montar([]);

    expect(screen.getByText(/ainda não tem nenhuma permissão com tela/i)).toBeInTheDocument();
  });

  it('a seção atual é marcada para o leitor de tela, e não só pela cor', () => {
    montar(['VER_PRODUTO']);

    expect(screen.getByRole('button', { name: 'Cardápio' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('clicar avisa a seção', async () => {
    const aoEscolher = montar(['VER_PRODUTO']);

    await userEvent.click(screen.getByRole('button', { name: 'Cardápio' }));

    expect(aoEscolher).toHaveBeenCalledWith('cardapio');
  });

  it('permissão repetida não duplica o item', () => {
    expect(secoesDe(['VER_PRODUTO', 'VER_PRODUTO'])).toHaveLength(1);
  });

  it('a primeira seção de quem tem várias permissões é determinística', () => {
    expect(secoesDe(['VER_PEDIDO', 'VER_PRODUTO'])[0]?.secao).toBe('cardapio');
  });
});
