import type { ItemDoMenu } from '../aki/Casca';
import { caminhoDaLoja } from './caminhos';
import type { LojaDoUsuario } from './tipos';

/**
 * O menu lateral de uma loja, pelo papel e pelas permissões que o servidor informou. Mora fora
 * da `CascaDaLoja` para o arquivo do componente exportar só componente (o recarregamento do
 * Vite exige) e para a lista de lojas reaproveitar o mesmo menu (02/11/2026).
 */
export function menuDa(loja: LojaDoUsuario): ItemDoMenu[] {
  const base = caminhoDaLoja(loja.estabelecimentoId);
  const tem = (permissao: LojaDoUsuario['permissoes'][number]) =>
    loja.permissoes.includes(permissao);
  const cadastra = tem('CRIAR_PRODUTO') || tem('ALTERAR_PRODUTO');

  const entregas = tem('VER_ENTREGA')
    ? [
        {
          chave: 'entregas',
          rotulo: 'Minhas entregas',
          icone: 'two_wheeler',
          para: `${base}/entregas`,
        },
      ]
    : [];

  if (loja.papel === 'COLABORADOR') {
    // O entregador puro (VER_ENTREGA sem VER_PEDIDO) vê só o app dele.
    if (!tem('VER_PEDIDO')) return entregas;
    return [
      { chave: 'dashboard', rotulo: 'Painel', icone: 'dashboard', para: base },
      { chave: 'pedidos', rotulo: 'Pedidos', icone: 'receipt_long', para: `${base}/pedidos` },
      ...entregas,
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
    { chave: 'pedidos', rotulo: 'Pedidos', icone: 'receipt_long', para: `${base}/pedidos` },
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
      chave: 'fechamento',
      rotulo: 'Fechamento do dia',
      icone: 'point_of_sale',
      para: `${base}/fechamento`,
    },
    { chave: 'whatsapp', rotulo: 'WhatsApp', icone: 'chat', para: `${base}#whatsapp` },
    {
      chave: 'configuracoes',
      rotulo: 'Configurações',
      icone: 'settings',
      para: `${base}/configuracoes`,
    },
    // A porta para a segunda loja (passagem humana de 19/10, item 3). O `de` deixa a lista
    // com o menu desta loja na lateral, para voltar a ela sem procurar o ícone.
    {
      chave: 'lojas',
      rotulo: 'Minhas lojas',
      icone: 'storefront',
      para: `/painel?todas=1&de=${loja.estabelecimentoId}`,
    },
  ];
}
