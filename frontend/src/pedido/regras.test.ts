import {
  classeDoStatus,
  etapasDoAcompanhamento,
  hojeLocal,
  linkDoWhatsApp,
  mensagemDoWhatsApp,
  proximoPasso,
  quandoAbre,
} from './regras';

describe('o próximo passo do pedido', () => {
  it('na entrega, o pronto sai para entrega', () => {
    expect(proximoPasso({ status: 'PRONTO', modalidade: 'ENTREGA' })?.status).toBe(
      'SAIU_PARA_ENTREGA',
    );
  });

  it('na retirada, o pronto espera o cliente no balcão', () => {
    expect(proximoPasso({ status: 'PRONTO', modalidade: 'RETIRADA' })?.status).toBe(
      'AGUARDANDO_CLIENTE',
    );
  });

  it.each(['ENTREGUE', 'RETIRADO', 'CANCELADO'] as const)('%s não tem próximo passo', (status) => {
    expect(proximoPasso({ status, modalidade: 'ENTREGA' })).toBeNull();
  });
});

describe('a linha do tempo do cliente', () => {
  it('em preparo: a primeira etapa feita, a segunda em andamento', () => {
    const etapas = etapasDoAcompanhamento({ status: 'EM_PREPARO', modalidade: 'ENTREGA' });
    expect(etapas.map((etapa) => etapa.estado)).toEqual(['done', 'current', '', '']);
  });

  it('entregue: tudo feito, nada em andamento', () => {
    const etapas = etapasDoAcompanhamento({ status: 'ENTREGUE', modalidade: 'ENTREGA' });
    expect(etapas.map((etapa) => etapa.estado)).toEqual(['done', 'done', 'done', 'done']);
  });

  it('a retirada fala em balcão, e não em entrega', () => {
    const etapas = etapasDoAcompanhamento({ status: 'AGUARDANDO_CLIENTE', modalidade: 'RETIRADA' });
    expect(etapas[2]?.rotulo).toBe('Pronto para retirar');
    expect(etapas[2]?.estado).toBe('current');
  });
});

describe('o WhatsApp da loja', () => {
  it('o link wa.me leva só os dígitos do telefone, com o DDI', () => {
    expect(
      linkDoWhatsApp('+5521999990000', 'oi').startsWith('https://wa.me/5521999990000?text='),
    ).toBe(true);
  });

  it('a mensagem leva o número, o status e o link do acompanhamento', () => {
    const texto = mensagemDoWhatsApp(
      {
        numero: '000007',
        status: 'SAIU_PARA_ENTREGA',
        total: 104.8,
        cliente: { nome: 'Maria Souza', telefone: '+5521999990000' },
        motivoDoCancelamento: null,
      },
      'Pizzaria da Marli',
      'http://localhost:5173/pedido/abc',
    );
    expect(
      texto.startsWith('Olá, Maria! Aqui é da Pizzaria da Marli: seu pedido #000007 saiu'),
    ).toBe(true);
    expect(texto.endsWith('Acompanhe: http://localhost:5173/pedido/abc')).toBe(true);
  });
});

describe('cores e datas', () => {
  it('cancelado é vermelho; concluído, verde', () => {
    expect(classeDoStatus('CANCELADO')).toBe('red');
    expect(classeDoStatus('RETIRADO')).toBe('green');
  });

  it('o dia de hoje no formato do ?dia=', () => {
    expect(hojeLocal(new Date(2026, 10, 5, 23, 30))).toBe('2026-11-05');
  });
});

describe('quando a loja abre', () => {
  // 01/11/2026 é um domingo; as datas são do relógio local, como a tela as mostra.
  const agora = new Date(2026, 10, 1, 15, 0);

  it('mais tarde, hoje', () => {
    expect(quandoAbre(new Date(2026, 10, 1, 18, 0).toISOString(), agora)).toBe('hoje às 18:00');
  });

  it('amanhã de manhã', () => {
    expect(quandoAbre(new Date(2026, 10, 2, 11, 30).toISOString(), agora)).toBe('amanhã às 11:30');
  });

  it('mais adiante na semana, pelo nome do dia', () => {
    expect(quandoAbre(new Date(2026, 10, 7, 18, 0).toISOString(), agora)).toBe(
      'no sábado às 18:00',
    );
    expect(quandoAbre(new Date(2026, 10, 4, 18, 0).toISOString(), agora)).toBe(
      'na quarta-feira às 18:00',
    );
  });
});
