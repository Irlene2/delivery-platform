import { useState } from 'react';
import { Outlet } from 'react-router';
import { useRecurso } from '../api/useRecurso';
import { emDemonstracao, lojaDeDemonstracao } from '../aki/demo';
import type { ContextoDoPainel } from '../painel/contextoDoPainel';
import type { LojaDoUsuario } from '../painel/tipos';

/**
 * A raiz do painel: busca as lojas da pessoa (`GET /me/estabelecimentos`) e as
 * entrega às telas filhas. No modo demonstração, a loja é a de exemplo.
 */
export function PainelPage() {
  const [geracao, definirGeracao] = useState(0);
  const recarregar = () => definirGeracao((atual) => atual + 1);

  if (emDemonstracao()) {
    const contexto: ContextoDoPainel = {
      lojas: [lojaDeDemonstracao()],
      recarregarLojas: recarregar,
    };
    return <Outlet context={contexto} />;
  }
  return <LojasDoServidor key={geracao} recarregarLojas={recarregar} />;
}

function LojasDoServidor({ recarregarLojas }: { recarregarLojas: () => void }) {
  const lojas = useRecurso<LojaDoUsuario[]>('/api/v1/me/estabelecimentos');

  if (lojas.estado === 'carregando') {
    return (
      <div className="empty-menu" style={{ marginTop: '20vh' }}>
        <strong>Carregando as suas lojas…</strong>
      </div>
    );
  }
  if (lojas.estado === 'erro') {
    return (
      <div className="empty-menu" role="alert" style={{ marginTop: '20vh' }}>
        <span className="material-symbols-outlined">cloud_off</span>
        <strong>Não deu para carregar as suas lojas agora</strong>
        <p>Verifique se o servidor está no ar e recarregue a página.</p>
      </div>
    );
  }
  const contexto: ContextoDoPainel = { lojas: lojas.dados, recarregarLojas };
  return <Outlet context={contexto} />;
}
