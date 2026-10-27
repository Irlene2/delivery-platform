import { chamar } from './cliente';

/**
 * O pedido da entrega de 15/11 (ADR-077, `docs/entrega-15-11/pedido.md`).
 *
 * ## Tipos escritos à mão, e por quê
 *
 * Os outros serviços têm o contrato em `contracts/openapi/` e os tipos **gerados** em
 * `generated/`. O `order` ainda não tem o contrato commitado — está escrito como gatilho no
 * `pedido.md` §6. Até ele existir, estes tipos espelham os DTOs do `order-service`
 * (`api/dto/*`) campo a campo. No dia do contrato, este bloco vira
 * `components['schemas'][...]` do `generated/order.ts`, e o `typecheck` aponta o que divergiu.
 *
 * Valor em dinheiro chega como **número** (o `BigDecimal` do Jackson), com duas casas.
 */

export type StatusDoPedido =
  | 'RECEBIDO'
  | 'CONFIRMADO'
  | 'EM_PREPARO'
  | 'PRONTO'
  | 'SAIU_PARA_ENTREGA'
  | 'AGUARDANDO_CLIENTE'
  | 'ENTREGUE'
  | 'RETIRADO'
  | 'CANCELADO';

export type Modalidade = 'ENTREGA' | 'RETIRADA';
export type FormaDePagamento = 'DINHEIRO' | 'CARTAO' | 'PIX';

export interface ItemDoPedido {
  readonly produtoId: string;
  readonly nome: string;
  readonly precoUnitario: number;
  readonly quantidade: number;
  readonly observacao: string | null;
  readonly subtotal: number;
}

export interface PagamentoDeclarado {
  readonly forma: FormaDePagamento;
  readonly trocoPara: number | null;
}

/** O que o cliente vê — sem telefone e sem endereço (ADR-013). */
export interface PedidoPublico {
  readonly id: string;
  readonly estabelecimentoId: string;
  readonly numero: string;
  readonly codigoDeAcompanhamento: string;
  readonly status: StatusDoPedido;
  readonly modalidade: Modalidade;
  readonly itens: readonly ItemDoPedido[];
  readonly subtotal: number;
  readonly taxaDeEntrega: number;
  readonly total: number;
  readonly pagamento: PagamentoDeclarado;
  readonly criadoEm: string;
  readonly atualizadoEm: string;
}

export interface PagamentoRegistrado {
  readonly formaEfetiva: FormaDePagamento;
  readonly valorRecebido: number | null;
  readonly trocoDevolvido: number | null;
  readonly pixConferido: boolean;
  readonly registradoEm: string;
}

/** O pedido inteiro, como a loja o opera. */
export interface PedidoDaLoja {
  readonly id: string;
  readonly numero: string;
  /** O código do acompanhamento: vai no link que a loja manda pelo WhatsApp. */
  readonly codigoDeAcompanhamento: string;
  readonly status: StatusDoPedido;
  readonly modalidade: Modalidade;
  readonly cliente: { readonly nome: string; readonly telefone: string };
  readonly endereco: {
    readonly logradouro: string;
    readonly bairro: string;
    readonly complemento: string | null;
  } | null;
  readonly itens: readonly ItemDoPedido[];
  readonly subtotal: number;
  readonly taxaDeEntrega: number;
  readonly total: number;
  readonly pagamentoDeclarado: PagamentoDeclarado;
  readonly pagamentoRegistrado: PagamentoRegistrado | null;
  readonly entregador: string | null;
  readonly motivoDoCancelamento: string | null;
  readonly criadoEm: string;
  readonly atualizadoEm: string;
}

export interface ConfiguracaoDePedidos {
  readonly taxaDeEntrega: number;
  readonly aceitaEntrega: boolean;
  readonly aceitaRetirada: boolean;
}

/** O carrinho fechado. **Sem preço**: o servidor lê o preço no cardápio. */
export interface NovoPedido {
  readonly cliente: { readonly nome: string; readonly telefone: string };
  readonly modalidade: Modalidade;
  readonly endereco?: {
    readonly logradouro: string;
    readonly bairro: string;
    readonly complemento?: string;
  };
  readonly itens: readonly {
    readonly produtoId: string;
    readonly quantidade: number;
    readonly observacao?: string;
  }[];
  readonly pagamento: { readonly forma: FormaDePagamento; readonly trocoPara?: number };
}

export interface PagamentoInformado {
  readonly formaEfetiva: FormaDePagamento;
  readonly valorRecebido?: number;
  readonly pixConferido?: boolean;
}

// ─────────────── a vitrine: pública, NUNCA com token ───────────────
// Um token vencido numa rota pública faria o gateway responder 401 a quem só quer
// pedir uma pizza. Por isso estas quatro funções não aceitam token.

export function lerConfiguracaoPublica(estabelecimentoId: string): Promise<ConfiguracaoDePedidos> {
  return chamar<ConfiguracaoDePedidos>(`/api/v1/storefront/${estabelecimentoId}/orders/settings`);
}

export function fazerPedido(estabelecimentoId: string, corpo: NovoPedido): Promise<PedidoPublico> {
  return chamar<PedidoPublico>(`/api/v1/storefront/${estabelecimentoId}/orders`, {
    metodo: 'POST',
    corpo,
  });
}

export function acompanharPedido(codigo: string): Promise<PedidoPublico> {
  return chamar<PedidoPublico>(`/api/v1/storefront/orders/${encodeURIComponent(codigo)}`);
}

// ─────────────── o painel: com token ───────────────

const doPainel = (estabelecimentoId: string) => `/api/v1/merchants/${estabelecimentoId}/orders`;

export function listarPedidos(
  estabelecimentoId: string,
  token: string,
  filtro: { status?: StatusDoPedido; dia?: string } = {},
): Promise<PedidoDaLoja[]> {
  const busca = new URLSearchParams();
  if (filtro.status !== undefined) busca.set('status', filtro.status);
  if (filtro.dia !== undefined) busca.set('dia', filtro.dia);
  const texto = busca.toString();
  const sufixo = texto === '' ? '' : `?${texto}`;
  return chamar<PedidoDaLoja[]>(`${doPainel(estabelecimentoId)}${sufixo}`, { token });
}

export function mudarStatus(
  estabelecimentoId: string,
  pedidoId: string,
  status: StatusDoPedido,
  motivo: string | undefined,
  token: string,
): Promise<PedidoDaLoja> {
  return chamar<PedidoDaLoja>(`${doPainel(estabelecimentoId)}/${pedidoId}/status`, {
    metodo: 'PUT',
    corpo: motivo === undefined ? { status } : { status, motivo },
    token,
  });
}

export function informarEntregador(
  estabelecimentoId: string,
  pedidoId: string,
  nome: string,
  token: string,
): Promise<PedidoDaLoja> {
  return chamar<PedidoDaLoja>(`${doPainel(estabelecimentoId)}/${pedidoId}/courier`, {
    metodo: 'PUT',
    corpo: { nome },
    token,
  });
}

export function registrarPagamento(
  estabelecimentoId: string,
  pedidoId: string,
  pagamento: PagamentoInformado,
  token: string,
): Promise<PedidoDaLoja> {
  return chamar<PedidoDaLoja>(`${doPainel(estabelecimentoId)}/${pedidoId}/settlement`, {
    metodo: 'PUT',
    corpo: pagamento,
    token,
  });
}

export function lerConfiguracao(
  estabelecimentoId: string,
  token: string,
): Promise<ConfiguracaoDePedidos> {
  return chamar<ConfiguracaoDePedidos>(`${doPainel(estabelecimentoId)}/settings`, { token });
}

export function definirConfiguracao(
  estabelecimentoId: string,
  configuracao: ConfiguracaoDePedidos,
  token: string,
): Promise<ConfiguracaoDePedidos> {
  return chamar<ConfiguracaoDePedidos>(`${doPainel(estabelecimentoId)}/settings`, {
    metodo: 'PUT',
    corpo: configuracao,
    token,
  });
}
