import type { PedidoDaLoja } from '../api/pedidos';
import { apurar, fechamentoEmCsv, periodoEmCsv, somaDoPeriodo } from './caixa';

const entregue: PedidoDaLoja = {
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
  pagamentoRegistrado: {
    formaEfetiva: 'DINHEIRO',
    valorRecebido: 50,
    trocoDevolvido: 5,
    pixConferido: false,
    registradoEm: '2026-11-05T21:00:00Z',
  },
  entregador: 'Carlos',
  entregadorId: 'u-carlos',
  motivoDoCancelamento: null,
  criadoEm: '2026-11-05T21:00:00Z',
  atualizadoEm: '2026-11-05T21:30:00Z',
};

describe('o caixa do dia na demonstração', () => {
  it('apura como o servidor: dinheiro, repasse, acerto e a diferença da contagem', () => {
    const f = apurar([entregue], { dia: '2026-11-05', dinheiroContado: 40 });
    expect(f.vendas).toBe(45);
    expect(f.dinheiro).toBe(45);
    expect(f.repasseTotal).toBe(5);
    expect(f.acertos[0]?.acerto).toBe(40);
    expect(f.diferencaDeCaixa).toBe(-5);
  });

  it('com pedido em andamento não fecha', () => {
    let frase = '';
    try {
      apurar([{ ...entregue, status: 'EM_PREPARO' }], { dia: '2026-11-05' });
    } catch (erro) {
      frase = erro instanceof Error ? erro.message : '';
    }
    expect(frase.includes('em andamento') || frase.length > 0).toBe(true);
  });

  it('o CSV usa ponto e vírgula e vírgula decimal', () => {
    const csv = fechamentoEmCsv(apurar([entregue], { dia: '2026-11-05' }));
    expect(csv.includes('Vendas;45,00')).toBe(true);
    expect(csv.includes('"Carlos";1;5,00;45,00;0,00;40,00')).toBe(true);
  });
});

describe('as vendas de um período', () => {
  it('soma os dias fechados, sem erro de centavos', () => {
    const um = apurar([entregue], { dia: '2026-11-04' });
    const outro = apurar([{ ...entregue, total: 10.1 }], { dia: '2026-11-05' });
    const soma = somaDoPeriodo([outro, um]);
    expect(soma.dias).toBe(2);
    expect(soma.vendas).toBe(55.1);
    expect(soma.repasse).toBe(10);
  });

  it('o CSV do período tem um dia por linha, em ordem, e o total', () => {
    const csv = periodoEmCsv([
      apurar([entregue], { dia: '2026-11-05' }),
      apurar([entregue], { dia: '2026-11-04' }),
    ]);
    const linhas = csv.split('\r\n');
    expect(linhas[1]?.startsWith('04/11/2026;')).toBe(true);
    expect(linhas[3]).toBe('Total (2 dias);2;0;90,00;90,00;0,00;0,00;10,00;10,00');
  });
});
