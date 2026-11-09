import type { FormaDePagamento, PedidoDaLoja } from '../api/pedidos';
import type { Pedido as PedidoDoTicket } from '../aki/pedidos';
import { IMAGEM_PADRAO } from '../painel/catalogo';
import { classeDoStatus, ROTULO_DA_FORMA, ROTULO_DO_STATUS } from './regras';

/**
 * As contas do dia, a partir dos pedidos — para o dashboard, o fechamento e o app do
 * entregador. **Nada aqui é guardado**: é a soma do que o servidor devolveu. O fechamento que
 * fica registrado é o do servidor: o `order-service` apura e grava o caixa do dia desde
 * 01/11/2026 (`FechamentoDoDia`).
 *
 * ## A regra do repasse (decisão de 31/10/2026)
 *
 * O entregador recebe **a taxa de entrega de cada pedido que entregou**. O dinheiro que ele
 * recebeu na porta é da loja. O acerto do fim do dia é a diferença:
 *
 * `acerto = dinheiro recebido − repasse`
 *
 * Positivo: o entregador devolve esse valor à loja. Negativo: a loja paga a diferença a ele.
 */

const r2 = (valor: number) => Math.round(valor * 100) / 100;

export const concluido = (pedido: Pick<PedidoDaLoja, 'status'>) =>
  pedido.status === 'ENTREGUE' || pedido.status === 'RETIRADO';

/** A forma que vale para as contas: a registrada; sem registro, a que o cliente declarou. */
export const formaQueVale = (pedido: PedidoDaLoja): FormaDePagamento =>
  pedido.pagamentoRegistrado?.formaEfetiva ?? pedido.pagamentoDeclarado.forma;

export interface AcertoDoEntregador {
  readonly chave: string;
  readonly nome: string;
  readonly comConta: boolean;
  readonly entregas: number;
  readonly emRota: number;
  readonly repasse: number;
  readonly dinheiroRecebido: number;
  readonly cartaoRecebido: number;
  readonly acerto: number;
}

export function acertosDosEntregadores(pedidos: readonly PedidoDaLoja[]): AcertoDoEntregador[] {
  const por = new Map<string, AcertoDoEntregador>();
  for (const pedido of pedidos) {
    if (pedido.entregador === null || pedido.status === 'CANCELADO') continue;
    const chave = pedido.entregadorId ?? `nome:${pedido.entregador}`;
    const atual = por.get(chave) ?? {
      chave,
      nome: pedido.entregador,
      comConta: pedido.entregadorId !== null,
      entregas: 0,
      emRota: 0,
      repasse: 0,
      dinheiroRecebido: 0,
      cartaoRecebido: 0,
      acerto: 0,
    };
    const entregue = pedido.status === 'ENTREGUE';
    const forma = pedido.pagamentoRegistrado?.formaEfetiva;
    const dinheiro = entregue && forma === 'DINHEIRO' ? pedido.total : 0;
    const repasse = entregue ? pedido.taxaDeEntrega : 0;
    const novo = {
      ...atual,
      entregas: atual.entregas + (entregue ? 1 : 0),
      emRota: atual.emRota + (pedido.status === 'SAIU_PARA_ENTREGA' ? 1 : 0),
      repasse: r2(atual.repasse + repasse),
      dinheiroRecebido: r2(atual.dinheiroRecebido + dinheiro),
      cartaoRecebido: r2(
        atual.cartaoRecebido + (entregue && forma === 'CARTAO' ? pedido.total : 0),
      ),
    };
    por.set(chave, { ...novo, acerto: r2(novo.dinheiroRecebido - novo.repasse) });
  }
  return [...por.values()].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
}

export interface ResumoDoDia {
  readonly pedidos: number;
  readonly concluidos: number;
  readonly cancelados: number;
  readonly novos: number;
  readonly naCozinha: number;
  readonly emEntrega: number;
  readonly vendas: number;
  readonly taxas: number;
  readonly ticketMedio: number;
  readonly porForma: Record<FormaDePagamento, number>;
  /** As vendas concluídas em faixas de duas horas, das 8h às 24h. */
  readonly porHorario: number[];
}

export const FAIXAS_DE_HORARIO = ['08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h'];

export function resumoDoDia(pedidos: readonly PedidoDaLoja[]): ResumoDoDia {
  const feitos = pedidos.filter(concluido);
  const vendas = r2(feitos.reduce((soma, p) => soma + p.total, 0));
  const porForma: Record<FormaDePagamento, number> = { PIX: 0, CARTAO: 0, DINHEIRO: 0 };
  const porHorario = FAIXAS_DE_HORARIO.map(() => 0);
  for (const pedido of feitos) {
    const forma = formaQueVale(pedido);
    porForma[forma] = r2(porForma[forma] + pedido.total);
    const hora = new Date(pedido.criadoEm).getHours();
    const faixa = Math.floor((hora - 8) / 2);
    if (faixa >= 0 && faixa < porHorario.length) {
      porHorario[faixa] = r2((porHorario[faixa] ?? 0) + pedido.total);
    }
  }
  const contar = (...quais: PedidoDaLoja['status'][]) =>
    pedidos.filter((p) => quais.includes(p.status)).length;
  return {
    pedidos: pedidos.length,
    concluidos: feitos.length,
    cancelados: contar('CANCELADO'),
    novos: contar('RECEBIDO'),
    naCozinha: contar('CONFIRMADO', 'EM_PREPARO', 'PRONTO'),
    emEntrega: contar('SAIU_PARA_ENTREGA', 'AGUARDANDO_CLIENTE'),
    vendas,
    taxas: r2(feitos.reduce((soma, p) => soma + p.taxaDeEntrega, 0)),
    ticketMedio: feitos.length === 0 ? 0 : r2(vendas / feitos.length),
    porForma,
    porHorario,
  };
}

/** O pedido no formato do comprovante do protótipo (`Ticket`), que a tela reaproveita. */
export function paraOTicket(pedido: PedidoDaLoja): PedidoDoTicket {
  return {
    number: pedido.numero,
    date: pedido.criadoEm,
    customer: pedido.cliente.nome,
    status: ROTULO_DO_STATUS[pedido.status],
    statusClass: classeDoStatus(pedido.status),
    items: pedido.itens.map((item, i) => ({
      id: `${item.produtoId}-${i}`,
      name: item.nome,
      price: item.precoUnitario,
      image: IMAGEM_PADRAO,
      qty: item.quantidade,
    })),
    subtotal: pedido.subtotal,
    fee: pedido.taxaDeEntrega,
    total: pedido.total,
    paymentMethod: ROTULO_DA_FORMA[formaQueVale(pedido)],
    cashChange:
      pedido.pagamentoDeclarado.trocoPara === null
        ? ''
        : String(pedido.pagamentoDeclarado.trocoPara),
  };
}
