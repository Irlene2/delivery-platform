import { ErroDaApi } from '../api/cliente';
import type {
  ConfiguracaoDePedidos,
  EntregadorDaEquipe,
  EntregadorEscolhido,
  NovoPedido,
  PagamentoInformado,
  PedidoDaLoja,
  PedidoPublico,
  StatusDoPedido,
} from '../api/pedidos';
import { gravar, ler } from '../aki/guardado';
import { fonteDaDemonstracao } from '../painel/catalogo';
import { aceitaPagamento, encerrado, ROTULO_DO_STATUS } from './regras';

/**
 * O `order` de mentira do modo demonstração: as mesmas regras e as mesmas frases do
 * servidor nos casos que a demonstração exercita, guardado nesta aba. Serve para ensaiar
 * e para o vídeo de reserva sem back-end — **não** para conferir regra: quem confere é o
 * `PedidoTest` do `order-service`.
 *
 * As recusas saem como `ErroDaApi` com o `detail` do servidor, para a tela tratá-las igual.
 */

const CHAVE_PEDIDOS = 'aki.demo.pedidos';
const CHAVE_CONFIG = 'aki.demo.configDePedidos';
export const PREFIXO_DO_CODIGO_DEMO = 'demo-';

interface Guardado {
  readonly pedido: PedidoDaLoja;
  readonly codigo: string;
}

const todos = (): Guardado[] => ler<Guardado[]>(CHAVE_PEDIDOS, []);
const salvarTodos = (lista: Guardado[]) => gravar(CHAVE_PEDIDOS, lista.slice(0, 60));

const recusa = (detalhe: string) => new ErroDaApi(400, detalhe);
const conflito = (detalhe: string) => new ErroDaApi(409, detalhe);
const reais = (valor: number) =>
  valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const duasCasas = (valor: number) => Math.round(valor * 100) / 100;

export function configuracaoDaDemonstracao(): ConfiguracaoDePedidos {
  return ler<ConfiguracaoDePedidos>(CHAVE_CONFIG, {
    taxaDeEntrega: 5,
    aceitaEntrega: true,
    aceitaRetirada: true,
  });
}

export function definirConfiguracaoDaDemonstracao(
  configuracao: ConfiguracaoDePedidos,
): ConfiguracaoDePedidos {
  if (configuracao.taxaDeEntrega < 0) throw recusa('a taxa de entrega não pode ser negativa');
  if (!configuracao.aceitaEntrega && !configuracao.aceitaRetirada) {
    throw recusa('a loja precisa aceitar entrega, retirada ou as duas');
  }
  gravar(CHAVE_CONFIG, configuracao);
  return configuracao;
}

export function publico(pedido: PedidoDaLoja, codigo: string): PedidoPublico {
  return {
    id: pedido.id,
    estabelecimentoId: 'demo',
    numero: pedido.numero,
    codigoDeAcompanhamento: codigo,
    status: pedido.status,
    modalidade: pedido.modalidade,
    itens: pedido.itens,
    subtotal: pedido.subtotal,
    taxaDeEntrega: pedido.taxaDeEntrega,
    total: pedido.total,
    pagamento: pedido.pagamentoDeclarado,
    criadoEm: pedido.criadoEm,
    atualizadoEm: pedido.atualizadoEm,
  };
}

export async function fazerPedidoNaDemonstracao(novo: NovoPedido): Promise<PedidoPublico> {
  if (novo.cliente.nome.trim() === '') throw recusa('informe o nome do cliente');
  if (novo.cliente.telefone.replace(/\D/g, '').length < 10) {
    throw recusa('o telefone não parece válido; use DDD e número, como (21) 99999-0000');
  }
  if (novo.itens.length === 0) throw recusa('o carrinho está vazio; escolha ao menos um produto');
  const configuracao = configuracaoDaDemonstracao();
  const entrega = novo.modalidade === 'ENTREGA';
  if (entrega && !configuracao.aceitaEntrega) {
    throw recusa('esta loja não está fazendo entregas; escolha retirar no balcão');
  }
  if (!entrega && !configuracao.aceitaRetirada) {
    throw recusa('esta loja não aceita retirada no balcão; escolha entrega');
  }
  if (
    entrega &&
    (novo.endereco === undefined ||
      novo.endereco.logradouro.trim() === '' ||
      novo.endereco.bairro.trim() === '')
  ) {
    throw recusa('para entrega, informe o endereço e o bairro');
  }

  const catalogo = await fonteDaDemonstracao().carregar();
  const itens = novo.itens.map((pedido) => {
    const produto = catalogo.itens.find(
      (um) => um.id === pedido.produtoId && um.situacao === 'ATIVO',
    );
    if (produto === undefined) {
      throw recusa(
        'um dos produtos do carrinho não está mais no cardápio; atualize a página e monte o pedido de novo',
      );
    }
    if (!produto.vendavel)
      throw recusa(`«${produto.nome}» acabou; tire do carrinho para continuar`);
    return {
      produtoId: produto.id,
      nome: produto.nome,
      precoUnitario: produto.preco,
      quantidade: pedido.quantidade,
      observacao: pedido.observacao ?? null,
      subtotal: duasCasas(produto.preco * pedido.quantidade),
    };
  });
  const subtotal = duasCasas(itens.reduce((soma, item) => soma + item.subtotal, 0));
  const taxaDeEntrega = entrega ? configuracao.taxaDeEntrega : 0;
  const total = duasCasas(subtotal + taxaDeEntrega);
  const troco = novo.pagamento.trocoPara;
  if (troco !== undefined) {
    if (novo.pagamento.forma !== 'DINHEIRO')
      throw recusa('troco só existe no pagamento em dinheiro');
    if (troco < total) {
      throw recusa(`o troco para ${reais(troco)} é menor que o total de ${reais(total)}`);
    }
  }

  const lista = todos();
  const numero = String(lista.length + 1).padStart(6, '0');
  const agora = new Date().toISOString();
  const codigo = `${PREFIXO_DO_CODIGO_DEMO}${Date.now().toString(36)}`;
  const pedido: PedidoDaLoja = {
    id: `demo-pedido-${Date.now().toString(36)}`,
    numero,
    codigoDeAcompanhamento: codigo,
    status: 'RECEBIDO',
    modalidade: novo.modalidade,
    cliente: {
      nome: novo.cliente.nome.trim(),
      telefone: `+55${novo.cliente.telefone.replace(/\D/g, '').replace(/^55(?=\d{10,11}$)/, '')}`,
    },
    endereco:
      entrega && novo.endereco !== undefined
        ? {
            logradouro: novo.endereco.logradouro.trim(),
            bairro: novo.endereco.bairro.trim(),
            complemento: novo.endereco.complemento?.trim() || null,
          }
        : null,
    itens,
    subtotal,
    taxaDeEntrega,
    total,
    pagamentoDeclarado: { forma: novo.pagamento.forma, trocoPara: troco ?? null },
    pagamentoRegistrado: null,
    entregador: null,
    entregadorId: null,
    motivoDoCancelamento: null,
    criadoEm: agora,
    atualizadoEm: agora,
  };
  salvarTodos([{ pedido, codigo }, ...lista]);
  return publico(pedido, codigo);
}

export function acompanharNaDemonstracao(codigo: string): PedidoPublico {
  const achado = todos().find((um) => um.codigo === codigo);
  if (achado === undefined) throw new ErroDaApi(404, 'pedido não encontrado');
  return publico(achado.pedido, achado.codigo);
}

export function pedidosDaDemonstracao(status?: StatusDoPedido): PedidoDaLoja[] {
  return todos()
    .map((um) => um.pedido)
    .filter((pedido) => status === undefined || pedido.status === status);
}

function alterar(pedidoId: string, ato: (pedido: PedidoDaLoja) => PedidoDaLoja): PedidoDaLoja {
  const lista = todos();
  const achado = lista.find((um) => um.pedido.id === pedidoId);
  if (achado === undefined) throw new ErroDaApi(404, 'pedido não encontrado');
  const novo = { ...ato(achado.pedido), atualizadoEm: new Date().toISOString() };
  salvarTodos(lista.map((um) => (um.pedido.id === pedidoId ? { ...um, pedido: novo } : um)));
  return novo;
}

const SETAS: Record<StatusDoPedido, StatusDoPedido[]> = {
  RECEBIDO: ['CONFIRMADO', 'CANCELADO'],
  CONFIRMADO: ['EM_PREPARO', 'CANCELADO'],
  EM_PREPARO: ['PRONTO', 'CANCELADO'],
  PRONTO: ['SAIU_PARA_ENTREGA', 'AGUARDANDO_CLIENTE'],
  SAIU_PARA_ENTREGA: ['ENTREGUE'],
  AGUARDANDO_CLIENTE: ['RETIRADO'],
  ENTREGUE: [],
  RETIRADO: [],
  CANCELADO: [],
};

export function avancarNaDemonstracao(
  pedidoId: string,
  destino: StatusDoPedido,
  motivo?: string,
): PedidoDaLoja {
  return alterar(pedidoId, (pedido) => {
    const naoPode = conflito(
      `um pedido «${ROTULO_DO_STATUS[pedido.status]}» não pode ir para «${ROTULO_DO_STATUS[destino]}»`,
    );
    if (!SETAS[pedido.status].includes(destino)) throw naoPode;
    if (destino === 'SAIU_PARA_ENTREGA' && pedido.modalidade !== 'ENTREGA') throw naoPode;
    if (destino === 'AGUARDANDO_CLIENTE' && pedido.modalidade !== 'RETIRADA') throw naoPode;
    if (destino === 'SAIU_PARA_ENTREGA' && pedido.entregador === null) {
      throw conflito('informe o entregador antes de marcar que o pedido saiu');
    }
    if ((destino === 'ENTREGUE' || destino === 'RETIRADO') && pedido.pagamentoRegistrado === null) {
      throw conflito('registre o pagamento antes de concluir o pedido');
    }
    if (destino === 'CANCELADO') {
      const texto = motivo?.trim() ?? '';
      if (texto === '') throw recusa('para cancelar, informe o motivo');
      return { ...pedido, status: destino, motivoDoCancelamento: texto };
    }
    return { ...pedido, status: destino };
  });
}

/** Os entregadores da equipe de exemplo. "Você" é quem entra como entregador na demonstração. */
export const ENTREGADORES_DA_DEMONSTRACAO: readonly EntregadorDaEquipe[] = [
  { usuarioId: 'demo-entregador', nome: 'Você (entregador de exemplo)' },
  { usuarioId: 'demo-carlos', nome: 'Carlos Moto' },
];

export function entregadorNaDemonstracao(
  pedidoId: string,
  escolhido: EntregadorEscolhido,
): PedidoDaLoja {
  return alterar(pedidoId, (pedido) => {
    if (pedido.modalidade !== 'ENTREGA') {
      throw recusa('este pedido é de retirada e não tem entregador');
    }
    if (encerrado(pedido.status)) throw conflito('este pedido já foi encerrado');
    if ('usuarioId' in escolhido) {
      const da = ENTREGADORES_DA_DEMONSTRACAO.find((um) => um.usuarioId === escolhido.usuarioId);
      if (da === undefined) {
        throw recusa(
          'este entregador não está na equipe da loja; peça para ele abrir a área de entregas uma vez e informar o nome',
        );
      }
      return { ...pedido, entregador: da.nome, entregadorId: da.usuarioId };
    }
    if (escolhido.nome.trim() === '') throw recusa('informe o nome do entregador');
    return { ...pedido, entregador: escolhido.nome.trim(), entregadorId: null };
  });
}

export function pagamentoNaDemonstracao(
  pedidoId: string,
  informado: PagamentoInformado,
): PedidoDaLoja {
  return alterar(pedidoId, (pedido) => {
    if (!aceitaPagamento(pedido.status)) {
      throw conflito(
        encerrado(pedido.status)
          ? `este pedido já foi encerrado («${ROTULO_DO_STATUS[pedido.status]}»)`
          : `o pagamento é registrado a partir de «Pronto»; este pedido está «${ROTULO_DO_STATUS[pedido.status]}»`,
      );
    }
    let valorRecebido: number | null = null;
    let trocoDevolvido: number | null = null;
    if (informado.formaEfetiva === 'DINHEIRO') {
      if (informado.valorRecebido === undefined) {
        throw recusa('no dinheiro, informe quanto foi recebido para calcular o troco');
      }
      if (informado.valorRecebido < pedido.total) {
        throw recusa(
          `o valor recebido (${reais(informado.valorRecebido)}) é menor que o total do pedido (${reais(pedido.total)})`,
        );
      }
      valorRecebido = informado.valorRecebido;
      trocoDevolvido = duasCasas(informado.valorRecebido - pedido.total);
    }
    if (informado.formaEfetiva === 'PIX' && informado.pixConferido !== true) {
      throw recusa(
        'confira o Pix no extrato da conta antes de registrar; comprovante na tela do cliente não vale',
      );
    }
    return {
      ...pedido,
      pagamentoRegistrado: {
        formaEfetiva: informado.formaEfetiva,
        valorRecebido,
        trocoDevolvido,
        pixConferido: informado.formaEfetiva === 'PIX',
        registradoEm: new Date().toISOString(),
      },
    };
  });
}
