import { Link, Navigate, useLocation } from 'react-router';
import { Casca } from '../aki/Casca';
import { menuDa } from './CascaDaLoja';
import { caminhoDaLoja, NOVA_LOJA } from './caminhos';
import { useContextoDoPainel } from './contextoDoPainel';

/**
 * `/painel`: com uma loja, vai direto a ela; com várias, a pessoa escolhe; sem
 * nenhuma, o caminho de quem é dono — cadastrar a própria loja — ou esperar um
 * convite.
 */
export function InicioDoPainel() {
  const { lojas } = useContextoDoPainel();
  // `?todas=1` (o "Minhas lojas" do menu) mostra a lista mesmo com uma loja só.
  const parametros = new URLSearchParams(useLocation().search);
  const todas = parametros.has('todas');
  // Vindo de uma loja ("Minhas lojas" do menu dela), a lateral continua com o menu dela.
  const deOnde = lojas.find((loja) => loja.estabelecimentoId === parametros.get('de'));
  const unica = lojas.length === 1 ? lojas[0] : undefined;
  if (unica !== undefined && !todas)
    return <Navigate to={caminhoDaLoja(unica.estabelecimentoId)} replace />;

  return (
    <Casca
      menu={
        deOnde !== undefined
          ? menuDa(deOnde)
          : [{ chave: 'lojas', rotulo: 'Minhas lojas', icone: 'storefront', para: '/painel' }]
      }
      atual="lojas"
      titulo="Painel"
      subtitulo={
        lojas.length === 0 ? 'Você ainda não faz parte de nenhuma loja.' : 'Escolha a loja.'
      }
      rodape={
        <>
          <b>{deOnde?.nome ?? 'AKI Delivery'}</b>
          <br />
          Gestão da operação
        </>
      }
      acoes={<div className="avatar">AK</div>}
    >
      {lojas.length === 0 ? (
        <section className="panel quick-panel" style={{ padding: '20px' }}>
          <div className="table-title">
            <div>
              <h2>Você ainda não faz parte de nenhuma loja</h2>
              <p className="admin-muted">
                Quem administra a loja precisa te convidar — o convite chega como um link. Se a loja
                é sua, cadastre-a.
              </p>
            </div>
          </div>
          <div className="quick-list">
            <Link className="quick-card" to={NOVA_LOJA}>
              <span className="material-symbols-outlined">add_business</span>
              <div>
                <strong>Cadastrar minha loja</strong>
                <small>Nome, documento, endereço e formas de pagamento</small>
              </div>
            </Link>
          </div>
        </section>
      ) : (
        <section className="panel quick-panel" style={{ padding: '20px' }}>
          <div className="table-title">
            <h2>Suas lojas</h2>
          </div>
          <div className="quick-list">
            {lojas.map((loja) => (
              <Link
                key={loja.estabelecimentoId}
                className="quick-card"
                to={caminhoDaLoja(loja.estabelecimentoId)}
              >
                <span className="material-symbols-outlined">storefront</span>
                <div>
                  <strong>{loja.nome}</strong>
                  <small>
                    {loja.papel === 'ADMINISTRADOR'
                      ? 'Você administra esta loja'
                      : 'Você faz parte desta equipe'}
                  </small>
                </div>
              </Link>
            ))}
            <Link className="quick-card" to={NOVA_LOJA}>
              <span className="material-symbols-outlined">add_business</span>
              <div>
                <strong>Cadastrar outra loja</strong>
                <small>Mais um estabelecimento na sua conta</small>
              </div>
            </Link>
          </div>
        </section>
      )}
    </Casca>
  );
}
