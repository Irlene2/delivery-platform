import { partesDaFoto } from './foto';

describe('a foto que vai ao servidor', () => {
  it('separa o tipo e o Base64 de uma foto nova', () => {
    expect(partesDaFoto('data:image/jpeg;base64,AQID')).toEqual({
      tipo: 'image/jpeg',
      conteudo: 'AQID',
    });
  });

  it('a foto que já está no servidor (um endereço) não é reenviada', () => {
    expect(partesDaFoto('http://localhost:8080/api/v1/storefront/x/produtos/y/imagem?v=1')).toBe(
      null,
    );
    expect(partesDaFoto('')).toBeNull();
  });
});
