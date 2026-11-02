import { useEffect } from 'react';
import { Navigate, Outlet } from 'react-router';
import { toast } from '../aki/toast';
import { caminhoDaLoja } from './caminhos';
import { useContextoDoPainel } from './contextoDoPainel';
import { DashboardAdmin } from './DashboardAdmin';
import { DashboardColaborador } from './DashboardColaborador';
import { useLojaAtual } from './lojaAtual';

/**
 * Uma loja do painel (ADR-061: a loja mora na URL). Confere que a loja da URL é
 * uma das da pessoa e passa o contexto adiante para as telas da loja.
 *
 * A loja que não é da pessoa leva à lista dela — e, para quem ainda não tem loja, ao
 * "Cadastrar minha loja". Acontece quando outra conta entra no mesmo navegador: a volta do
 * login aponta para a loja da conta anterior (passagem de 02/11/2026).
 */
export function LojaDoPainel() {
  const contexto = useContextoDoPainel();
  const { loja } = useLojaAtual();
  const fora = loja === undefined;
  const semLoja = contexto.lojas.length === 0;

  useEffect(() => {
    if (!fora) return;
    toast(
      semLoja
        ? 'Bem-vindo! Cadastre a sua loja para começar.'
        : 'Essa loja não está entre as suas. Escolha uma das suas lojas.',
    );
  }, [fora, semLoja]);

  if (fora) return <Navigate to="/painel?todas=1" replace />;
  return <Outlet context={contexto} />;
}

/**
 * A página inicial da loja: o painel do administrador ou o do colaborador, pelo papel. O
 * entregador puro — `VER_ENTREGA` sem `VER_PEDIDO` — vai direto para as entregas dele.
 */
export function InicioDaLoja() {
  const { loja } = useLojaAtual();
  const permissoes = loja?.permissoes ?? [];
  if (permissoes.includes('VER_ENTREGA') && !permissoes.includes('VER_PEDIDO')) {
    return <Navigate to={`${caminhoDaLoja(loja?.estabelecimentoId ?? '')}/entregas`} replace />;
  }
  return loja?.papel === 'COLABORADOR' ? <DashboardColaborador /> : <DashboardAdmin />;
}
