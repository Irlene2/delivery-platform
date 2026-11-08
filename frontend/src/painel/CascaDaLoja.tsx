import type { ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { Casca } from '../aki/Casca';
import { caminhoDaLoja } from './caminhos';
import { menuDa } from './menuDaLoja';
import { SinoDeNotificacoes } from './SinoDeNotificacoes';
import { useLojaAtual } from './lojaAtual';

/**
 * A moldura do painel de uma loja: o menu do administrador ou o do colaborador
 * do protótipo, conforme o **papel que o servidor informou** para esta loja.
 *
 * Os itens que dependem de permissão (Catálogo, Equipe) só aparecem para quem a
 * tem; esconder é conveniência — quem decide é o servidor, a cada requisição.
 */
export type ChaveDoMenu =
  | 'dashboard'
  | 'pedidos'
  | 'catalogo'
  | 'cardapio'
  | 'estoque'
  | 'equipe'
  | 'horario'
  | 'lojas'
  | 'whatsapp'
  | 'pagamentos'
  | 'fechamento'
  | 'entregas'
  | 'configuracoes';

export function CascaDaLoja({
  atual,
  titulo,
  subtitulo,
  children,
}: {
  atual: ChaveDoMenu;
  titulo: string;
  subtitulo: string;
  children: ReactNode;
}) {
  const { loja, lojas } = useLojaAtual();
  const navegar = useNavigate();
  if (loja === undefined) return null;
  const colaborador = loja.papel === 'COLABORADOR';
  const entregador =
    colaborador &&
    loja.permissoes.includes('VER_ENTREGA') &&
    !loja.permissoes.includes('VER_PEDIDO');

  return (
    <Casca
      menu={menuDa(loja)}
      atual={atual}
      titulo={titulo}
      subtitulo={subtitulo}
      rotuloDoMenu={colaborador ? 'Menu do colaborador' : 'Menu administrativo'}
      rodape={
        colaborador ? (
          <>
            Perfil com acesso limitado
            <br />
            <b>{loja.nome}</b>
          </>
        ) : (
          <>
            <b>{loja.nome}</b>
            <br />
            Gestão da operação
          </>
        )
      }
      acoes={
        <>
          {lojas.length > 1 && (
            <select
              className="select"
              aria-label="Loja"
              value={loja.estabelecimentoId}
              onChange={(evento) => void navegar(caminhoDaLoja(evento.target.value))}
            >
              {lojas.map((uma) => (
                <option key={uma.estabelecimentoId} value={uma.estabelecimentoId}>
                  {uma.nome}
                </option>
              ))}
            </select>
          )}
          {!colaborador && <SinoDeNotificacoes estabelecimentoId={loja.estabelecimentoId} />}
          <div className="avatar">{entregador ? 'EN' : colaborador ? 'CO' : 'AD'}</div>
          <span className="role">
            {entregador ? 'Entregador' : colaborador ? 'Colaborador' : 'Administrador'}
          </span>
        </>
      }
    >
      {children}
    </Casca>
  );
}
