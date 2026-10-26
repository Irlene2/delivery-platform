import { juntarDescricao, lerPreco, separarDescricao } from './catalogo';

describe('ingredientes na descrição', () => {
  it('junta o texto e a linha de ingredientes', () => {
    expect(juntarDescricao('Cremoso', ['banana', 'granola'])).toBe(
      'Cremoso\n\nIngredientes: banana, granola',
    );
  });

  it('sem ingredientes, fica só o texto; sem nada, fica vazio', () => {
    expect(juntarDescricao('  Cremoso  ', [])).toBe('Cremoso');
    expect(juntarDescricao('', [])).toBe('');
  });

  it('separa de volta, inclusive quando só há ingredientes', () => {
    expect(separarDescricao('Cremoso\n\nIngredientes: banana, granola')).toEqual({
      texto: 'Cremoso',
      ingredientes: ['banana', 'granola'],
    });
    expect(separarDescricao('Ingredientes: pão')).toEqual({ texto: '', ingredientes: ['pão'] });
  });

  it('descrição sem a linha não perde nada', () => {
    expect(separarDescricao('Molho de tomate, mussarela')).toEqual({
      texto: 'Molho de tomate, mussarela',
      ingredientes: [],
    });
    expect(separarDescricao(undefined)).toEqual({ texto: '', ingredientes: [] });
  });
});

describe('lerPreco', () => {
  it.each([
    ['25,90', 25.9],
    ['25.90', 25.9],
    ['R$ 1.234,56', 1234.56],
    ['0', 0],
  ])('%s vira %d', (texto, valor) => {
    expect(lerPreco(texto)).toBe(valor);
  });

  it.each(['', 'abc', '-1', '1,999'])('recusa %j', (texto) => {
    expect(lerPreco(texto)).toBeNull();
  });
});
