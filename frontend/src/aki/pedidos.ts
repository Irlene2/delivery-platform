import { useSyncExternalStore } from 'react';
import { PRODUTOS_DE_EXEMPLO } from './exemplos';
import { gravar, ler } from './guardado';

/**
 * Pedidos e carrinho de demonstração, como no protótipo.
 *
 * O back-end ainda não tem o serviço de pedidos (marco 3). Até lá, o carrinho,
 * o checkout e o histórico vivem nesta aba. **Os valores são só exibição**: quando
 * o pedido existir no servidor, o total vem dele (premissas do front, §6).
 */

export interface ItemDoCarrinho {
  readonly id: string;
  readonly name: string;
  readonly price: number;
  readonly image: string;
  qty: number;
}

export interface Pedido {
  readonly number: string;
  readonly date: string;
  readonly customer: string;
  readonly status: string;
  readonly statusClass: string;
  readonly items: readonly ItemDoCarrinho[];
  readonly subtotal: number;
  readonly fee: number;
  readonly total: number;
  readonly paymentMethod?: string;
  readonly cashChange?: string;
}

const CHAVE_CARRINHO = 'aki.carrinho';
const CHAVE_PEDIDOS = 'aki.pedidos';

// ---------- um pequeno "store" para várias partes da tela verem o mesmo carrinho ----------
const ouvintes = new Set<() => void>();
let carrinho: ItemDoCarrinho[] = ler<ItemDoCarrinho[]>(CHAVE_CARRINHO, []);
let pedidos: Pedido[] = ler<Pedido[]>(CHAVE_PEDIDOS, []);

function avisar(): void {
  for (const ouvinte of ouvintes) ouvinte();
}
function assinar(ouvinte: () => void): () => void {
  ouvintes.add(ouvinte);
  return () => ouvintes.delete(ouvinte);
}

export function useCarrinho(): readonly ItemDoCarrinho[] {
  return useSyncExternalStore(assinar, () => carrinho);
}
export function usePedidos(): readonly Pedido[] {
  return useSyncExternalStore(assinar, () => pedidos);
}

function salvarCarrinho(novo: ItemDoCarrinho[]): void {
  carrinho = novo;
  gravar(CHAVE_CARRINHO, carrinho);
  avisar();
}
function salvarPedidos(novos: Pedido[]): void {
  pedidos = novos.slice(0, 30);
  gravar(CHAVE_PEDIDOS, pedidos);
  avisar();
}

export function adicionarAoCarrinho(produto: Omit<ItemDoCarrinho, 'qty'>, quantidade = 1): void {
  const existente = carrinho.find((item) => item.id === produto.id);
  salvarCarrinho(
    existente
      ? carrinho.map((item) =>
          item.id === produto.id ? { ...item, qty: item.qty + quantidade } : item,
        )
      : [...carrinho, { ...produto, qty: quantidade }],
  );
}

export function mudarQuantidade(id: string, delta: number): void {
  salvarCarrinho(
    carrinho
      .map((item) => (item.id === id ? { ...item, qty: item.qty + delta } : item))
      .filter((item) => item.qty > 0),
  );
}

export function retirarDoCarrinho(id: string): void {
  salvarCarrinho(carrinho.filter((item) => item.id !== id));
}

export function limparCarrinho(): void {
  salvarCarrinho([]);
}

export function totaisDoCarrinho(itens: readonly ItemDoCarrinho[]) {
  const subtotal = itens.reduce((total, item) => total + item.price * item.qty, 0);
  const fee = subtotal > 0 ? 5 : 0;
  return { subtotal, fee, total: subtotal + fee };
}

export const quantidadeNoCarrinho = (itens: readonly ItemDoCarrinho[]): number =>
  itens.reduce((soma, item) => soma + item.qty, 0);

function proximoNumero(): string {
  const ultimo = pedidos.reduce(
    (maior, pedido) => Math.max(maior, Number(pedido.number) || 123450),
    123450,
  );
  return String(ultimo + 1).padStart(6, '0');
}

/** Finaliza o carrinho num pedido de demonstração. Devolve o pedido, ou null se vazio. */
export function finalizarPedido(paymentMethod: string, cashChange: string): Pedido | null {
  if (carrinho.length === 0) return null;
  const { subtotal, fee, total } = totaisDoCarrinho(carrinho);
  const pedido: Pedido = {
    number: proximoNumero(),
    date: new Date().toISOString(),
    customer: 'Cliente AKI',
    status: 'Pedido recebido',
    statusClass: 'orange',
    items: carrinho.map((item) => ({ ...item })),
    subtotal,
    fee,
    total,
    paymentMethod,
    cashChange,
  };
  salvarPedidos([pedido, ...pedidos]);
  salvarCarrinho([]);
  return pedido;
}

/** Os quatro pedidos de exemplo do painel, criados uma vez por aba. */
export function semearPedidosDeExemplo(): void {
  if (pedidos.length > 0) return;
  const fazer = (
    number: string,
    customer: string,
    status: string,
    ids: string[],
    date: string,
    statusClass: string,
  ): Pedido => {
    const items = ids
      .map((id) => PRODUTOS_DE_EXEMPLO.find((p) => p.id === id))
      .filter((p) => p !== undefined)
      .map((p) => ({ id: p.id, name: p.nome, price: p.preco, qty: 1, image: p.imagem }));
    const subtotal = items.reduce((soma, item) => soma + item.price * item.qty, 0);
    return {
      number,
      customer,
      status,
      statusClass,
      date,
      items,
      subtotal,
      fee: 5,
      total: subtotal + 5,
    };
  };
  salvarPedidos([
    fazer('123456', 'Cliente', 'Em preparo', ['1', '5'], '2026-09-15T14:32:00', 'orange'),
    fazer('123455', 'Maria S.', 'Em entrega', ['2', '6'], '2026-09-15T14:05:00', 'blue'),
    fazer('123454', 'João P.', 'Concluído', ['11'], '2026-09-15T13:40:00', 'green'),
    fazer('123453', 'Ana C.', 'Em preparo', ['4', '14'], '2026-09-15T13:15:00', 'orange'),
  ]);
}

/** Ao sair da conta, nada do carrinho fica para a próxima pessoa. */
export function esquecerCarrinho(): void {
  salvarCarrinho([]);
}
