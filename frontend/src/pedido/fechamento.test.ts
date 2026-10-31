import type { PedidoDaLoja } from '../api/pedidos';
import { acertosDosEntregadores, resumoDoDia } from './fechamento';

const recebidoEmDinheiro = {
  formaEfetiva: 'DINHEIRO' as const,
  valorRecebido: 50,
  trocoDevolvido: 5,
  pixConferido: false,
  registradoEm: '2026-11-05T21:00:00Z',
};
const recebidoEmCartao = { ...recebidoEmDinheiro, formaEfetiva: 'CARTAO' as const };

const base: PedidoDaLoja = {
  id: 'p',
  numero: '000001',
  codigoDeAcompanhamento: 'c',
  status: 'ENTREGUE',
  modalidade: 'ENTREGA',
  cliente: { nome: 'Maria', telefone: '+5521999990000' },
  endereco: { logradouro: 'Rua A', bairro: 'Centro', complemento: null },
  itens: [],
  subtotal: 40,
  taxaDeEntrega: 5,
  total: 45,
  pagamentoDeclarado: { forma: 'DINHEIRO', trocoPara: 50 },
  pagamentoRegistrado: recebidoEmDinheiro,
  entregador: 'Carlos',
  entregadorId: 'u-carlos',
  motivoDoCancelamento: null,
  criadoEm: '2026-11-05T21:00:00Z',
  atualizadoEm: '2026-11-05T21:30:00Z',
};

describe('o acerto do entregador', () => {
  it('repasse é a taxa de cada entrega; o dinheiro recebido é da loja', () => {
    const cartao = { ...base, id: 'q', pagamentoRegistrado: recebidoEmCartao };
    const [carlos] = acertosDosEntregadores([base, cartao]);
    expect(carlos?.entregas).toBe(2);
    expect(carlos?.repasse).toBe(10);
    expect(carlos?.dinheiroRecebido).toBe(45);
    // 45 em dinheiro na mão − 10 de repasse = devolve 35 à loja.
    expect(carlos?.acerto).toBe(35);
  });

  it('só cartão: a loja paga o repasse inteiro (acerto negativo)', () => {
    const cartao = { ...base, pagamentoRegistrado: recebidoEmCartao };
    expect(acertosDosEntregadores([cartao])[0]?.acerto).toBe(-5);
  });

  it('em rota ainda não conta repasse, e cancelado não entra', () => {
    const emRota = {
      ...base,
      id: 'r',
      status: 'SAIU_PARA_ENTREGA' as const,
      pagamentoRegistrado: null,
    };
    const cancelado = { ...base, id: 's', status: 'CANCELADO' as const };
    const [carlos] = acertosDosEntregadores([emRota, cancelado]);
    expect(carlos?.emRota).toBe(1);
    expect(carlos?.repasse).toBe(0);
  });
});

describe('o resumo do dia', () => {
  it('vendas e formas contam só o concluído', () => {
    const novo = { ...base, id: 'n', status: 'RECEBIDO' as const, pagamentoRegistrado: null };
    const resumo = resumoDoDia([base, novo]);
    expect(resumo.pedidos).toBe(2);
    expect(resumo.vendas).toBe(45);
    expect(resumo.porForma.DINHEIRO).toBe(45);
    expect(resumo.novos).toBe(1);
    expect(resumo.ticketMedio).toBe(45);
  });
});
