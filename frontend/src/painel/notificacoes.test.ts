import type { PedidoDaLoja } from '../api/pedidos';
import { naoVistas, notificacoesDosPedidos } from './notificacoes';

const base: PedidoDaLoja = {
  id: 'p1',
  numero: '000001',
  codigoDeAcompanhamento: 'c',
  status: 'RECEBIDO',
  modalidade: 'ENTREGA',
  cliente: { nome: 'Maria', telefone: '+5521999990000' },
  endereco: { logradouro: 'Rua A', bairro: 'Centro', complemento: null },
  itens: [],
  subtotal: 40,
  taxaDeEntrega: 5,
  total: 45,
  pagamentoDeclarado: { forma: 'DINHEIRO', trocoPara: null },
  pagamentoRegistrado: null,
  entregador: null,
  entregadorId: null,
  motivoDoCancelamento: null,
  criadoEm: '2026-11-02T12:00:00Z',
  atualizadoEm: '2026-11-02T12:00:00Z',
};

describe('notificações do administrador', () => {
  it('o pedido novo vira um aviso', () => {
    const avisos = notificacoesDosPedidos([base]);
    expect(avisos.map((a) => a.chave)).toEqual(['p1:novo']);
    expect(avisos[0]?.titulo).toBe('Pedido novo #000001');
  });

  it('o que a própria loja faz (aceitar, preparar, pronto) não avisa', () => {
    const avisos = notificacoesDosPedidos([{ ...base, status: 'PRONTO' }]);
    expect(avisos.map((a) => a.chave)).toEqual(['p1:novo']);
  });

  it('o entregador entregou e registrou: dois avisos, o mais novo primeiro', () => {
    const avisos = notificacoesDosPedidos([
      {
        ...base,
        status: 'ENTREGUE',
        entregador: 'Carlos',
        atualizadoEm: '2026-11-02T13:00:00Z',
        pagamentoRegistrado: {
          formaEfetiva: 'DINHEIRO',
          valorRecebido: 50,
          trocoDevolvido: 5,
          pixConferido: false,
          registradoEm: '2026-11-02T13:00:00Z',
        },
      },
    ]);
    expect(avisos.map((a) => a.chave)).toEqual(['p1:entregue', 'p1:pago', 'p1:novo']);
  });

  it('o Pix informado pelo entregador avisa que precisa conferir', () => {
    const avisos = notificacoesDosPedidos([
      {
        ...base,
        status: 'ENTREGUE',
        pagamentoRegistrado: {
          formaEfetiva: 'PIX',
          valorRecebido: null,
          trocoDevolvido: null,
          pixConferido: false,
          registradoEm: '2026-11-02T13:00:00Z',
        },
      },
    ]);
    expect(avisos.some((a) => a.chave === 'p1:pix')).toBe(true);
  });

  it('o contador é só do que não foi visto', () => {
    const avisos = notificacoesDosPedidos([{ ...base, status: 'CANCELADO' }]);
    expect(naoVistas(avisos, new Set(['p1:novo'])).map((a) => a.chave)).toEqual(['p1:cancelado']);
  });
});
