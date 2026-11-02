import type { PedidoDaLoja } from '../api/pedidos';
import { dinheiro } from '../aki/formatos';
import { ROTULO_DA_FORMA } from '../pedido/regras';

/**
 * As notificações do administrador, tiradas dos pedidos do dia (02/11/2026).
 *
 * Não há serviço de notificação: o sino relê os pedidos de tempos em tempos e transforma o
 * que mudou em avisos. Só entra o que **não foi a própria loja que fez** — pedido novo, o
 * entregador saiu ou entregou, o cliente retirou, o pagamento registrado, o Pix a conferir e
 * o cancelamento. Aceitar, preparar e deixar pronto são atos de quem está no painel, e avisar
 * a pessoa do que ela mesma fez é barulho.
 *
 * Cada aviso tem uma chave estável (`pedido:acontecimento`). O sino guarda as chaves já vistas;
 * o contador é o que ainda não foi visto.
 */
export interface Notificacao {
  readonly chave: string;
  readonly pedidoId: string;
  readonly icone: string;
  readonly titulo: string;
  readonly detalhe: string;
  /** Para ordenar: o mais novo primeiro. */
  readonly quando: string;
}

export function notificacoesDosPedidos(pedidos: readonly PedidoDaLoja[]): Notificacao[] {
  const avisos: Notificacao[] = [];
  for (const pedido of pedidos) {
    const n = `#${pedido.numero}`;
    const aviso = (acontecimento: string, icone: string, titulo: string, detalhe: string) =>
      avisos.push({
        chave: `${pedido.id}:${acontecimento}`,
        pedidoId: pedido.id,
        icone,
        titulo,
        detalhe,
        quando: acontecimento === 'novo' ? pedido.criadoEm : pedido.atualizadoEm,
      });

    aviso(
      'novo',
      'receipt_long',
      `Pedido novo ${n}`,
      `${pedido.cliente.nome} · ${dinheiro(pedido.total)}`,
    );
    switch (pedido.status) {
      case 'SAIU_PARA_ENTREGA':
        aviso(
          'saiu',
          'two_wheeler',
          `Pedido ${n} saiu para entrega`,
          pedido.entregador ?? 'Entregador',
        );
        break;
      case 'ENTREGUE':
        aviso('entregue', 'task_alt', `Pedido ${n} entregue`, pedido.entregador ?? 'Entregador');
        break;
      case 'RETIRADO':
        aviso('retirado', 'task_alt', `Pedido ${n} retirado`, pedido.cliente.nome);
        break;
      case 'CANCELADO':
        aviso(
          'cancelado',
          'cancel',
          `Pedido ${n} cancelado`,
          pedido.motivoDoCancelamento ?? pedido.cliente.nome,
        );
        break;
      default:
        break;
    }
    const pago = pedido.pagamentoRegistrado;
    if (pago !== null) {
      if (pago.formaEfetiva === 'PIX' && !pago.pixConferido) {
        aviso(
          'pix',
          'pending',
          `Pix a conferir no pedido ${n}`,
          `${dinheiro(pedido.total)} · confira no extrato`,
        );
      } else {
        aviso(
          'pago',
          'payments',
          `Pagamento do pedido ${n}`,
          `${ROTULO_DA_FORMA[pago.formaEfetiva]} · ${dinheiro(pedido.total)}`,
        );
      }
    }
  }
  return avisos.sort((a, b) => b.quando.localeCompare(a.quando));
}

/** As que ainda não foram vistas, na mesma ordem. */
export function naoVistas(
  avisos: readonly Notificacao[],
  vistas: ReadonlySet<string>,
): Notificacao[] {
  return avisos.filter((aviso) => !vistas.has(aviso.chave));
}
