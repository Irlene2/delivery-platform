import {
  acompanharPedido,
  definirConfiguracao,
  fazerPedido,
  informarEntregador,
  lerConfiguracao,
  lerConfiguracaoPublica,
  listarPedidos,
  mudarStatus,
  registrarPagamento,
  type ConfiguracaoDePedidos,
  type NovoPedido,
  type PagamentoInformado,
  type PedidoDaLoja,
  type PedidoPublico,
  type StatusDoPedido,
} from '../api/pedidos';
import { chamar } from '../api/cliente';
import { estadoConhecido } from '../api/disponibilidade';
import type { components } from '../api/generated/catalog';
import { ID_DA_LOJA_DEMO } from '../aki/demo';
import {
  fonteDaDemonstracao,
  separarDescricao,
  type FonteDoCatalogo,
  type ItemDoCatalogo,
} from '../painel/catalogo';
import {
  acompanharNaDemonstracao,
  avancarNaDemonstracao,
  configuracaoDaDemonstracao,
  definirConfiguracaoDaDemonstracao,
  entregadorNaDemonstracao,
  fazerPedidoNaDemonstracao,
  pagamentoNaDemonstracao,
  pedidosDaDemonstracao,
  PREFIXO_DO_CODIGO_DEMO,
} from './demonstracao';

/**
 * De onde vêm os pedidos — o servidor ou a demonstração —, com a mesma forma para as telas.
 * É o padrão do `FonteDoCatalogo`: a tela não sabe se há servidor do outro lado.
 */

// ═══════════════════════ a vitrine (o cliente, sem conta) ═══════════════════════

export interface Vitrine {
  readonly cardapio: FonteDoCatalogo;
  configuracao(): Promise<ConfiguracaoDePedidos>;
  fazer(novo: NovoPedido): Promise<PedidoPublico>;
}

type CardapioDoContrato = components['schemas']['CardapioPublicoResponse'];

const somenteLeitura = () => Promise.reject(new Error('o cardápio público é só leitura'));

/** O cardápio público como `FonteDoCatalogo`: o mesmo cartão e a mesma janela do painel. */
function cardapioPublico(estabelecimentoId: string): FonteDoCatalogo {
  return {
    editaTudo: false,
    podeCriar: false,
    podeAlterar: false,
    async carregar() {
      const cardapio = await chamar<CardapioDoContrato>(
        `/api/v1/storefront/${estabelecimentoId}/menu`,
      );
      const tipos = [...(cardapio.categorias ?? [])]
        .sort((a, b) => (a.ordem ?? 0) - (b.ordem ?? 0))
        .map((secao) => ({
          id: String(secao.id ?? ''),
          nome: secao.nome ?? '',
          ordem: secao.ordem ?? 0,
          icone: 'restaurant_menu',
        }));
      const itens: ItemDoCatalogo[] = (cardapio.produtos ?? []).map((produto) => {
        const { texto, ingredientes } = separarDescricao(produto.descricao);
        const tipoId = String(produto.categoriaId ?? '');
        return {
          id: String(produto.id ?? ''),
          nome: produto.nome ?? '',
          tipoId,
          tipo: tipos.find((tipo) => tipo.id === tipoId)?.nome ?? '',
          descricao: texto,
          ingredientes,
          preco: produto.precoBase ?? 0,
          ordem: produto.ordem ?? 0,
          controle: null,
          // Tudo o que o cardápio público devolve está publicado.
          situacao: 'ATIVO',
          disponibilidade: estadoConhecido(produto.disponibilidade) ?? 'DISPONIVEL',
          vendavel: produto.vendavel === true,
          imagem: '',
          selo: '',
          grupos: [],
        };
      });
      return { tipos, itens };
    },
    // A descrição já vem no cardápio: abrir não precisa de outra ida ao servidor.
    abrir: (item) => Promise.resolve(item),
    salvar: somenteLeitura,
    marcar: somenteLeitura,
    marcarOpcao: somenteLeitura,
  };
}

export function vitrineDe(estabelecimentoId: string): Vitrine {
  if (estabelecimentoId === ID_DA_LOJA_DEMO) {
    return {
      cardapio: fonteDaDemonstracao(),
      configuracao: () => Promise.resolve(configuracaoDaDemonstracao()),
      fazer: fazerPedidoNaDemonstracao,
    };
  }
  return {
    cardapio: cardapioPublico(estabelecimentoId),
    configuracao: () => lerConfiguracaoPublica(estabelecimentoId),
    fazer: (novo) => fazerPedido(estabelecimentoId, novo),
  };
}

/** O acompanhamento: o código da demonstração tem prefixo próprio, e não vai ao servidor. */
export function acompanhar(codigo: string): Promise<PedidoPublico> {
  if (codigo.startsWith(PREFIXO_DO_CODIGO_DEMO)) {
    try {
      return Promise.resolve(acompanharNaDemonstracao(codigo));
    } catch (erro) {
      return Promise.reject(erro instanceof Error ? erro : new Error(String(erro)));
    }
  }
  return acompanharPedido(codigo);
}

// ═══════════════════════ o painel (a loja, com token) ═══════════════════════

export interface FonteDePedidos {
  listar(filtro: { status?: StatusDoPedido; dia?: string }): Promise<PedidoDaLoja[]>;
  avancar(pedidoId: string, status: StatusDoPedido, motivo?: string): Promise<PedidoDaLoja>;
  entregador(pedidoId: string, nome: string): Promise<PedidoDaLoja>;
  pagamento(pedidoId: string, pagamento: PagamentoInformado): Promise<PedidoDaLoja>;
  configuracao(): Promise<ConfiguracaoDePedidos>;
  definirConfiguracao(configuracao: ConfiguracaoDePedidos): Promise<ConfiguracaoDePedidos>;
}

/** Envolve uma função síncrona que pode lançar numa promessa — a forma das chamadas reais. */
function emPromessa<T>(ato: () => T): Promise<T> {
  try {
    return Promise.resolve(ato());
  } catch (erro) {
    return Promise.reject(erro instanceof Error ? erro : new Error(String(erro)));
  }
}

export function pedidosDaDemonstracaoComoFonte(): FonteDePedidos {
  return {
    // Na demonstração não há "dia": todos os pedidos desta aba.
    listar: (filtro) => emPromessa(() => pedidosDaDemonstracao(filtro.status)),
    avancar: (id, status, motivo) => emPromessa(() => avancarNaDemonstracao(id, status, motivo)),
    entregador: (id, nome) => emPromessa(() => entregadorNaDemonstracao(id, nome)),
    pagamento: (id, pagamento) => emPromessa(() => pagamentoNaDemonstracao(id, pagamento)),
    configuracao: () => emPromessa(configuracaoDaDemonstracao),
    definirConfiguracao: (c) => emPromessa(() => definirConfiguracaoDaDemonstracao(c)),
  };
}

export function pedidosDoServidor(estabelecimentoId: string, token: string): FonteDePedidos {
  return {
    listar: (filtro) => listarPedidos(estabelecimentoId, token, filtro),
    avancar: (id, status, motivo) => mudarStatus(estabelecimentoId, id, status, motivo, token),
    entregador: (id, nome) => informarEntregador(estabelecimentoId, id, nome, token),
    pagamento: (id, pagamento) => registrarPagamento(estabelecimentoId, id, pagamento, token),
    configuracao: () => lerConfiguracao(estabelecimentoId, token),
    definirConfiguracao: (c) => definirConfiguracao(estabelecimentoId, c, token),
  };
}
