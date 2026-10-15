import { Navigate, useNavigate } from 'react-router';

import { caminhoDaLoja } from './caminhos';
import { useContextoDoPainel } from './contextoDoPainel';
import { SeletorDeLoja } from './SeletorDeLoja';

/**
 * O `/painel` resolve por quantas lojas a pessoa tem (ADR-061 §3).
 *
 * - **zero** — o convite que já existia, **sem redirecionar**: zero lojas também é
 *   quem foi convidada e não aceitou, quem foi suspensa, quem saiu da última loja, e
 *   *"ainda não faz parte de nenhuma loja"* é verdade nos quatro casos. O convite
 *   ganhou um link para o cadastro;
 * - **uma** — vai direto a ela;
 * - **mais de uma** — o seletor, e a escolha vira endereço.
 */
export function InicioDoPainel() {
  const { lojas } = useContextoDoPainel();
  const navegar = useNavigate();

  const unica = lojas.length === 1 ? lojas[0] : undefined;
  if (unica !== undefined) {
    return <Navigate to={caminhoDaLoja(unica.estabelecimentoId)} replace />;
  }

  return (
    <SeletorDeLoja
      lojas={lojas}
      selecionada={null}
      aoSelecionar={(estabelecimentoId) => void navegar(caminhoDaLoja(estabelecimentoId))}
    />
  );
}
