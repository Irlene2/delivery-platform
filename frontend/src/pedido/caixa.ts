import {
  fecharCaixa,
  historicoDeFechamentos,
  lerFechamento,
  type FechamentoGravado,
  type NovoFechamento,
  type PedidoDaLoja,
} from '../api/pedidos';
import { ErroDaApi } from '../api/cliente';
import { gravar, ler } from '../aki/guardado';
import { acertosDosEntregadores, resumoDoDia } from './fechamento';
import { pedidosDaDemonstracao } from './demonstracao';

/**
 * O caixa do dia — fechar, conferir e o histórico (ADR-077, emenda de 01/11/2026). Do servidor,
 * que apura e grava; ou da demonstração, que soma os pedidos desta aba com as mesmas regras.
 */
export interface FonteDoCaixa {
  doDia(dia: string): Promise<FechamentoGravado | null>;
  fechar(novo: NovoFechamento): Promise<FechamentoGravado>;
  historico(): Promise<FechamentoGravado[]>;
}

export function caixaDoServidor(estabelecimentoId: string, token: string): FonteDoCaixa {
  return {
    doDia: (dia) => lerFechamento(estabelecimentoId, dia, token),
    fechar: (novo) => fecharCaixa(estabelecimentoId, novo, token),
    historico: () => historicoDeFechamentos(estabelecimentoId, token),
  };
}

const CHAVE = 'aki.demo.fechamentos';
const r2 = (valor: number) => Math.round(valor * 100) / 100;

/** A mesma apuração do `FechamentoDoDia` do servidor, sobre os pedidos da demonstração. */
export function apurar(
  pedidos: readonly PedidoDaLoja[],
  novo: NovoFechamento,
  agora: Date = new Date(),
): FechamentoGravado {
  const andamento = pedidos.filter(
    (um) => !['ENTREGUE', 'RETIRADO', 'CANCELADO'].includes(um.status),
  ).length;
  if (andamento > 0) {
    throw new ErroDaApi(
      409,
      `${andamento === 1 ? 'há 1 pedido do dia em andamento' : `há ${andamento} pedidos do dia em andamento`}; conclua ou cancele antes de fechar o caixa`,
    );
  }
  const resumo = resumoDoDia(pedidos);
  const acertos = acertosDosEntregadores(pedidos)
    .filter((um) => um.entregas > 0)
    .map((um) => ({
      entregadorId: um.comConta ? um.chave : null,
      nome: um.nome,
      entregas: um.entregas,
      repasse: um.repasse,
      dinheiro: um.dinheiroRecebido,
      cartao: um.cartaoRecebido,
      acerto: um.acerto,
    }));
  const contado = novo.dinheiroContado ?? null;
  return {
    id: `demo-${novo.dia}`,
    dia: novo.dia,
    fechadoEm: agora.toISOString(),
    pedidos: pedidos.length,
    concluidos: resumo.concluidos,
    cancelados: resumo.cancelados,
    vendas: resumo.vendas,
    taxas: resumo.taxas,
    dinheiro: resumo.porForma.DINHEIRO,
    cartao: resumo.porForma.CARTAO,
    pix: resumo.porForma.PIX,
    dinheiroContado: contado,
    diferencaDeCaixa: contado === null ? null : r2(contado - resumo.porForma.DINHEIRO),
    repasseTotal: r2(acertos.reduce((soma, um) => soma + um.repasse, 0)),
    observacao: novo.observacao?.trim() ? novo.observacao.trim() : null,
    acertos,
  };
}

export function caixaDaDemonstracao(): FonteDoCaixa {
  const todos = () => ler<FechamentoGravado[]>(CHAVE, []);
  return {
    doDia: (dia) => Promise.resolve(todos().find((um) => um.dia === dia) ?? null),
    fechar(novo) {
      try {
        if (todos().some((um) => um.dia === novo.dia)) {
          throw new ErroDaApi(
            409,
            'o caixa deste dia já foi fechado; veja o fechamento no histórico',
          );
        }
        const fechado = apurar(pedidosDaDemonstracao(), novo);
        gravar(CHAVE, [fechado, ...todos()].slice(0, 30));
        return Promise.resolve(fechado);
      } catch (erro) {
        return Promise.reject(erro instanceof Error ? erro : new Error(String(erro)));
      }
    },
    historico: () => Promise.resolve(todos()),
  };
}

/** O fechamento em CSV, para abrir na planilha (separador `;`, decimal com vírgula). */
export function fechamentoEmCsv(f: FechamentoGravado): string {
  const n = (valor: number | null) => (valor === null ? '' : valor.toFixed(2).replace('.', ','));
  const texto = (valor: string) => `"${valor.replace(/"/g, '""')}"`;
  const linhas = [
    ['Dia', f.dia.split('-').reverse().join('/')],
    ['Fechado em', new Date(f.fechadoEm).toLocaleString('pt-BR')],
    ['Pedidos', String(f.pedidos)],
    ['Concluídos', String(f.concluidos)],
    ['Cancelados', String(f.cancelados)],
    ['Vendas', n(f.vendas)],
    ['Dinheiro', n(f.dinheiro)],
    ['Cartão', n(f.cartao)],
    ['Pix', n(f.pix)],
    ['Taxas de entrega', n(f.taxas)],
    ['Repasse aos entregadores', n(f.repasseTotal)],
    ['Dinheiro contado', n(f.dinheiroContado)],
    ['Diferença de caixa', n(f.diferencaDeCaixa)],
    ['Observação', texto(f.observacao ?? '')],
    [],
    ['Entregador', 'Entregas', 'Repasse', 'Dinheiro recebido', 'Cartão recebido', 'Acerto'],
    ...f.acertos.map((a) => [
      texto(a.nome),
      String(a.entregas),
      n(a.repasse),
      n(a.dinheiro),
      n(a.cartao),
      n(a.acerto),
    ]),
  ];
  return linhas.map((linha) => linha.join(';')).join('\r\n');
}
