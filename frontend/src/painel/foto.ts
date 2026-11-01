/**
 * A foto do produto, reduzida no navegador antes de ir ao servidor (ADR-077, emenda de
 * 01/11/2026): o lado maior fica em 800 px e o arquivo vira JPEG. Uma foto de celular tem 3 a
 * 5 MB; reduzida, fica entre 60 e 200 KB — dentro do limite de 600 KB do catálogo.
 */
const LADO_MAXIMO = 800;
const QUALIDADE = 0.82;

export async function reduzirFoto(arquivo: File): Promise<string> {
  if (!arquivo.type.startsWith('image/')) throw new Error('Escolha um arquivo de imagem.');
  const original = await new Promise<HTMLImageElement>((resolver, recusar) => {
    const imagem = new Image();
    const url = URL.createObjectURL(arquivo);
    imagem.onload = () => {
      URL.revokeObjectURL(url);
      resolver(imagem);
    };
    imagem.onerror = () => {
      URL.revokeObjectURL(url);
      recusar(new Error('Não foi possível abrir esta imagem.'));
    };
    imagem.src = url;
  });
  const escala = Math.min(1, LADO_MAXIMO / Math.max(original.width, original.height));
  const tela = document.createElement('canvas');
  tela.width = Math.max(1, Math.round(original.width * escala));
  tela.height = Math.max(1, Math.round(original.height * escala));
  const contexto = tela.getContext('2d');
  if (contexto === null) throw new Error('Este navegador não consegue preparar a foto.');
  contexto.fillStyle = '#fff';
  contexto.fillRect(0, 0, tela.width, tela.height);
  contexto.drawImage(original, 0, 0, tela.width, tela.height);
  return tela.toDataURL('image/jpeg', QUALIDADE);
}

/** `data:image/jpeg;base64,AAAA` → o tipo e o Base64; `null` quando não é uma foto nova. */
export function partesDaFoto(dataUrl: string): { tipo: string; conteudo: string } | null {
  const achado = /^data:(image\/[a-z]+);base64,(.+)$/.exec(dataUrl);
  if (achado === null) return null;
  const [, tipo, conteudo] = achado;
  if (tipo === undefined || conteudo === undefined) return null;
  return { tipo, conteudo };
}
