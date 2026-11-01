import type {
  FormaDePagamento,
  Modalidade,
  PedidoDaLoja,
  PedidoPublico,
  StatusDoPedido,
} from '../api/pedidos';

/**
 * O que a tela precisa saber do pedido para **desenhar**: rótulo, cor, os botões que fazem
 * sentido agora e o texto do WhatsApp. Quem **decide** é o servidor — um botão que aparece
 * aqui e a regra recusa lá vira a frase do servidor na tela, não um erro silencioso.
 *
 * A tabela de setas é a do `pedido.md` §4, repetida de propósito: é ela que escolhe os botões.
 */

export const ROTULO_DO_STATUS: Record<StatusDoPedido, string> = {
  RECEBIDO: 'Pedido recebido',
  CONFIRMADO: 'Confirmado',
  EM_PREPARO: 'Em preparo',
  PRONTO: 'Pronto',
  SAIU_PARA_ENTREGA: 'Saiu para entrega',
  AGUARDANDO_CLIENTE: 'Pronto para retirar',
  ENTREGUE: 'Entregue',
  RETIRADO: 'Retirado',
  CANCELADO: 'Cancelado',
};

/** As três cores de etiqueta do protótipo (`.status.orange|blue|green`) e a do cancelado. */
export function classeDoStatus(status: StatusDoPedido): 'orange' | 'blue' | 'green' | 'red' {
  switch (status) {
    case 'SAIU_PARA_ENTREGA':
    case 'AGUARDANDO_CLIENTE':
      return 'blue';
    case 'ENTREGUE':
    case 'RETIRADO':
      return 'green';
    case 'CANCELADO':
      return 'red';
    default:
      return 'orange';
  }
}

export const ROTULO_DA_FORMA: Record<FormaDePagamento, string> = {
  PIX: 'Pix',
  CARTAO: 'Cartão',
  DINHEIRO: 'Dinheiro',
};

export const ROTULO_DA_MODALIDADE: Record<Modalidade, string> = {
  ENTREGA: 'Entrega',
  RETIRADA: 'Retirada no balcão',
};

export const encerrado = (status: StatusDoPedido): boolean =>
  status === 'ENTREGUE' || status === 'RETIRADO' || status === 'CANCELADO';

export const cancelavel = (status: StatusDoPedido): boolean =>
  status === 'RECEBIDO' || status === 'CONFIRMADO' || status === 'EM_PREPARO';

/** O pagamento se registra do pronto até antes de encerrar (`pedido.md` §5). */
export const aceitaPagamento = (status: StatusDoPedido): boolean =>
  status === 'PRONTO' || status === 'SAIU_PARA_ENTREGA' || status === 'AGUARDANDO_CLIENTE';

/** O próximo passo do fluxo feliz, com o texto do botão. `null` quando não há (encerrado). */
export function proximoPasso(
  pedido: Pick<PedidoDaLoja, 'status' | 'modalidade'>,
): { status: StatusDoPedido; rotulo: string; icone: string } | null {
  const entrega = pedido.modalidade === 'ENTREGA';
  switch (pedido.status) {
    case 'RECEBIDO':
      return { status: 'CONFIRMADO', rotulo: 'Confirmar pedido', icone: 'check_circle' };
    case 'CONFIRMADO':
      return { status: 'EM_PREPARO', rotulo: 'Iniciar preparo', icone: 'restaurant' };
    case 'EM_PREPARO':
      return { status: 'PRONTO', rotulo: 'Marcar como pronto', icone: 'room_service' };
    case 'PRONTO':
      return entrega
        ? { status: 'SAIU_PARA_ENTREGA', rotulo: 'Saiu para entrega', icone: 'two_wheeler' }
        : { status: 'AGUARDANDO_CLIENTE', rotulo: 'Avisar que está pronto', icone: 'storefront' };
    case 'SAIU_PARA_ENTREGA':
      return { status: 'ENTREGUE', rotulo: 'Marcar como entregue', icone: 'task_alt' };
    case 'AGUARDANDO_CLIENTE':
      return { status: 'RETIRADO', rotulo: 'Cliente retirou', icone: 'task_alt' };
    default:
      return null;
  }
}

/**
 * As etapas da linha do tempo do cliente. Quatro, como no protótipo: o cliente não precisa
 * distinguir "recebido" de "confirmado" para saber que está andando.
 */
export function etapasDoAcompanhamento(pedido: Pick<PedidoPublico, 'status' | 'modalidade'>) {
  const entrega = pedido.modalidade === 'ENTREGA';
  const etapas = [
    { rotulo: 'Pedido recebido', texto: 'A loja recebeu seu pedido.', icone: 'check_circle' },
    { rotulo: 'Em preparo', texto: 'Estamos preparando.', icone: 'restaurant' },
    entrega
      ? { rotulo: 'Em entrega', texto: 'A caminho.', icone: 'two_wheeler' }
      : { rotulo: 'Pronto para retirar', texto: 'Pode vir buscar.', icone: 'storefront' },
    entrega
      ? { rotulo: 'Entregue', texto: 'Pedido finalizado.', icone: 'task_alt' }
      : { rotulo: 'Retirado', texto: 'Pedido finalizado.', icone: 'task_alt' },
  ];
  const indice: Record<StatusDoPedido, number> = {
    RECEBIDO: 0,
    CONFIRMADO: 0,
    EM_PREPARO: 1,
    PRONTO: 1,
    SAIU_PARA_ENTREGA: 2,
    AGUARDANDO_CLIENTE: 2,
    ENTREGUE: 3,
    RETIRADO: 3,
    CANCELADO: -1,
  };
  const atual = indice[pedido.status];
  // Encerrado com sucesso: tudo feito, nenhuma etapa "em andamento".
  const final = pedido.status === 'ENTREGUE' || pedido.status === 'RETIRADO';
  return etapas.map((etapa, i) => ({
    ...etapa,
    estado: final || i < atual ? 'done' : i === atual ? 'current' : '',
  }));
}

/** O texto que a loja manda ao cliente pelo WhatsApp, por status. */
export function mensagemDoWhatsApp(
  pedido: Pick<PedidoDaLoja, 'numero' | 'status' | 'total' | 'cliente' | 'motivoDoCancelamento'>,
  nomeDaLoja: string,
  linkDoAcompanhamento: string | null,
): string {
  const primeiroNome = pedido.cliente.nome.split(' ')[0] ?? '';
  const total = pedido.total.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  const por: Record<StatusDoPedido, string> = {
    RECEBIDO: `recebemos seu pedido #${pedido.numero} (${total}). Já já confirmamos! 🍔`,
    CONFIRMADO: `seu pedido #${pedido.numero} foi confirmado. Total: ${total}.`,
    EM_PREPARO: `seu pedido #${pedido.numero} está em preparo. 👨‍🍳`,
    PRONTO: `seu pedido #${pedido.numero} está pronto!`,
    SAIU_PARA_ENTREGA: `seu pedido #${pedido.numero} saiu para entrega. O entregador está a caminho! 🛵`,
    AGUARDANDO_CLIENTE: `seu pedido #${pedido.numero} está pronto para retirar no balcão. 🛍️`,
    ENTREGUE: `seu pedido #${pedido.numero} foi entregue. Obrigado pela preferência! ❤️`,
    RETIRADO: `obrigado por retirar o pedido #${pedido.numero}. Volte sempre! ❤️`,
    CANCELADO: `seu pedido #${pedido.numero} foi cancelado${pedido.motivoDoCancelamento ? `: ${pedido.motivoDoCancelamento}` : ''}.`,
  };
  const acompanhe = linkDoAcompanhamento === null ? '' : `\nAcompanhe: ${linkDoAcompanhamento}`;
  return `Olá, ${primeiroNome}! Aqui é da ${nomeDaLoja}: ${por[pedido.status]}${acompanhe}`;
}

/** O link `wa.me` (ADR-077): só dígitos, com o DDI que o servidor já normalizou. */
export function linkDoWhatsApp(telefone: string, texto: string): string {
  return `https://wa.me/${telefone.replace(/\D/g, '')}?text=${encodeURIComponent(texto)}`;
}

/** O dia de hoje no fuso de quem está na tela, como o `?dia=` da lista espera (AAAA-MM-DD). */
export function hojeLocal(agora: Date = new Date()): string {
  const doisDigitos = (n: number) => String(n).padStart(2, '0');
  return `${agora.getFullYear()}-${doisDigitos(agora.getMonth() + 1)}-${doisDigitos(agora.getDate())}`;
}

const DIAS = [
  'domingo',
  'segunda-feira',
  'terça-feira',
  'quarta-feira',
  'quinta-feira',
  'sexta-feira',
  'sábado',
];

/**
 * Quando a loja abre, no relógio de quem olha: "hoje às 18:00", "amanhã às 11:00", "no sábado às
 * 18:00". É a mesma conta da frase que o `order` devolve ao recusar o pedido.
 */
export function quandoAbre(proximaAbertura: string, agora: Date = new Date()): string {
  const abre = new Date(proximaAbertura);
  const doisDigitos = (n: number) => String(n).padStart(2, '0');
  const hora = ` às ${doisDigitos(abre.getHours())}:${doisDigitos(abre.getMinutes())}`;
  const amanha = new Date(agora);
  amanha.setDate(agora.getDate() + 1);
  if (hojeLocal(abre) === hojeLocal(agora)) return `hoje${hora}`;
  if (hojeLocal(abre) === hojeLocal(amanha)) return `amanhã${hora}`;
  const dia = abre.getDay();
  return `${dia === 0 || dia === 6 ? 'no' : 'na'} ${DIAS[dia] ?? ''}${hora}`;
}
