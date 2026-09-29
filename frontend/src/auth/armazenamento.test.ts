import { describe, expect, it, vi } from 'vitest';
import { esquecer, guardar, recuperar } from './armazenamento';
import type { Sessao } from './armazenamento';

/**
 * O armazenamento da sessão, e os casos que só aparecem quando alguém recarrega
 * a página no pior momento possível.
 *
 * Cada teste aqui falha por conta própria: nenhum confere só o que ele mesmo
 * acabou de escrever pelo mesmo caminho que está sendo testado.
 */
describe('armazenamento da sessão', () => {
  const daquiAMeiaHora = (): Sessao => ({ token: 'um.token', expiraEm: Date.now() + 1_800_000 });

  it('o que entra é o que sai', () => {
    guardar(daquiAMeiaHora());

    const lida = recuperar();

    expect(lida?.token).toBe('um.token');
  });

  it('token expirado não volta — e é apagado, não só escondido', () => {
    // Ler o `sessionStorage` direto é o ponto: se `recuperar` só devolvesse
    // `null` sem limpar, a próxima requisição da página iria com o token morto
    // e tomaria 401. Este teste falha se a limpeza sair.
    sessionStorage.setItem(
      'delivery.sessao',
      JSON.stringify({ token: 'morto', expiraEm: Date.now() - 1 }),
    );

    expect(recuperar()).toBeNull();
    expect(sessionStorage.getItem('delivery.sessao')).toBeNull();
  });

  it('expiração exatamente agora já não vale', () => {
    // A fronteira: `<=` e não `<`. Com `<`, um token que expira neste
    // milissegundo seria aceito e falharia na requisição seguinte.
    sessionStorage.setItem(
      'delivery.sessao',
      JSON.stringify({ token: 'na-fronteira', expiraEm: Date.now() }),
    );

    expect(recuperar()).toBeNull();
  });

  it.each([
    ['não é JSON', 'isto não é json'],
    ['é JSON e não é objeto', '"só uma string"'],
    ['é objeto sem token', JSON.stringify({ expiraEm: Date.now() + 1000 })],
    ['tem token vazio', JSON.stringify({ token: '', expiraEm: Date.now() + 1000 })],
    ['tem expiraEm que não é número', JSON.stringify({ token: 'x', expiraEm: 'amanhã' })],
    ['tem expiraEm infinito', JSON.stringify({ token: 'x', expiraEm: Infinity })],
  ])('conteúdo que %s é descartado e apagado', (_caso, cru) => {
    sessionStorage.setItem('delivery.sessao', cru);

    expect(recuperar()).toBeNull();
    expect(sessionStorage.getItem('delivery.sessao')).toBeNull();
  });

  it('esquecer apaga', () => {
    guardar(daquiAMeiaHora());
    esquecer();

    expect(sessionStorage.getItem('delivery.sessao')).toBeNull();
  });

  it('guardar não estoura quando o navegador recusa armazenar', () => {
    // Aba privada, dados de site bloqueados, cota. O acesso ESTOURA em vez de
    // devolver vazio, e sem o try o login inteiro falharia por causa disso.
    const quebrado = vi.spyOn(sessionStorage, 'setItem').mockImplementation(() => {
      throw new DOMException('QuotaExceededError');
    });

    expect(() => guardar(daquiAMeiaHora())).not.toThrow();

    quebrado.mockRestore();
  });

  it('recuperar não estoura quando o navegador recusa ler', () => {
    const quebrado = vi.spyOn(sessionStorage, 'getItem').mockImplementation(() => {
      throw new DOMException('SecurityError');
    });

    expect(recuperar()).toBeNull();

    quebrado.mockRestore();
  });
});
