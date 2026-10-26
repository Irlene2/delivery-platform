import type { ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { Casca, type ItemDoMenu } from '../aki/Casca';
import { caminhoDaLoja } from './caminhos';
import { useLojaAtual } from './lojaAtual';
import type { LojaDoUsuario } from './tipos';

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
  | 'configuracoes';

function menuDa(loja: LojaDoUsuario): ItemDoMenu[] {
  const base = caminhoDaLoja(loja.estabelecimentoId);
  const tem = (permissao: LojaDoUsuario['permissoes'][number]) =>
    loja.permissoes.includes(permissao);
  const cadastra = tem('CRIAR_PRODUTO') || tem('ALTERAR_PRODUTO');

  if (loja.papel === 'COLABORADOR') {
    return [
      { chave: 'dashboard', rotulo: 'Painel', icone: 'dashboard', para: base },
      { chave: 'pedidos', rotulo: 'Pedidos', icone: 'receipt_long', para: `${base}#pedidos` },
      ...(cadastra
        ? [
            {
              chave: 'catalogo',
              rotulo: 'Catálogo',
              icone: 'restaurant_menu',
              para: `${base}/produtos`,
            },
          ]
        : []),
      { chave: 'cardapio', rotulo: 'Cardápio', icone: 'restaurant_menu', para: `${base}/cardapio` },
      {
        chave: 'estoque',
        rotulo: 'Consultar estoque',
        icone: 'inventory_2',
        para: `${base}/disponibilidade`,
      },
      ...(tem('GERENCIAR_EQUIPE')
        ? [{ chave: 'equipe', rotulo: 'Equipe', icone: 'group', para: `${base}/equipe` }]
        : []),
    ];
  }

  return [
    { chave: 'dashboard', rotulo: 'Dashboard', icone: 'dashboard', para: base },
    { chave: 'pedidos', rotulo: 'Pedidos', icone: 'receipt_long', para: `${base}#pedidos` },
    { chave: 'catalogo', rotulo: 'Catálogo', icone: 'restaurant_menu', para: `${base}/produtos` },
    { chave: 'cardapio', rotulo: 'Ver cardápio', icone: 'storefront', para: `${base}/cardapio` },
    { chave: 'estoque', rotulo: 'Estoque', icone: 'inventory_2', para: `${base}/disponibilidade` },
    ...(tem('GERENCIAR_EQUIPE')
      ? [{ chave: 'equipe', rotulo: 'Equipe', icone: 'group', para: `${base}/equipe` }]
      : []),
    ...(tem('CONFIGURAR_LOJA')
      ? [{ chave: 'horario', rotulo: 'Horário', icone: 'schedule', para: `${base}/horario` }]
      : []),
    {
      chave: 'whatsapp',
      rotulo: 'WhatsApp',
      icone: 'chat',
      para: `${base}#whatsapp`,
      selo: <span className="nav-badge">novo</span>,
    },
    { chave: 'pagamentos', rotulo: 'Pagamentos', icone: 'payments', para: `${base}#pagamentos` },
    {
      chave: 'configuracoes',
      rotulo: 'Configurações',
      icone: 'settings',
      para: `${base}#configuracoes`,
    },
    // A porta para a segunda loja (passagem humana de 19/10, item 3).
    { chave: 'lojas', rotulo: 'Minhas lojas', icone: 'storefront', para: '/painel?todas=1' },
  ];
}

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
          {!colaborador && (
            <button className="icon-btn notification" type="button" aria-label="Notificações">
              <span className="material-symbols-outlined">notifications</span>
              <b>4</b>
            </button>
          )}
          <div className="avatar">{colaborador ? 'CO' : 'AD'}</div>
          <span className="role">{colaborador ? 'Colaborador' : 'Administrador'}</span>
        </>
      }
    >
      {children}
    </Casca>
  );
}
