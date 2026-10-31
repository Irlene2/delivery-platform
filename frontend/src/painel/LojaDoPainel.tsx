import { Link, Navigate, Outlet } from 'react-router';
import { Casca } from '../aki/Casca';
import { caminhoDaLoja } from './caminhos';
import { useContextoDoPainel } from './contextoDoPainel';
import { DashboardAdmin } from './DashboardAdmin';
import { DashboardColaborador } from './DashboardColaborador';
import { useLojaAtual } from './lojaAtual';

/**
 * Uma loja do painel (ADR-061: a loja mora na URL). Confere que a loja da URL é
 * uma das da pessoa e passa o contexto adiante para as telas da loja.
 */
export function LojaDoPainel() {
  const contexto = useContextoDoPainel();
  const { loja } = useLojaAtual();

  if (loja === undefined) {
    return (
      <Casca
        menu={[{ chave: 'lojas', rotulo: 'Minhas lojas', icone: 'storefront', para: '/painel' }]}
        atual="lojas"
        titulo="Painel"
        subtitulo="Loja não encontrada."
        rodape={<b>AKI Delivery</b>}
        acoes={<div className="avatar">AK</div>}
      >
        <div className="empty-menu" role="alert">
          <span className="material-symbols-outlined">store</span>
          <strong>Esta loja não está entre as suas</strong>
          <p>
            <Link to="/painel">Voltar às suas lojas</Link>
          </p>
        </div>
      </Casca>
    );
  }
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
