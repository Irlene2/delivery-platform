/**
 * Leitura e escrita no `sessionStorage`, tolerando navegador que recusa guardar.
 *
 * `sessionStorage`, e não `localStorage`: é a regra do projeto (ADR-047, e a
 * regra `no-restricted-globals` do ESLint). Aqui moram só coisas da aba: o modo
 * demonstração, o tema, o carrinho e os dados de exemplo.
 */
export function ler<T>(chave: string, padrao: T): T {
  try {
    const cru = sessionStorage.getItem(chave);
    return cru === null ? padrao : (JSON.parse(cru) as T);
  } catch {
    return padrao;
  }
}

export function gravar(chave: string, valor: unknown): void {
  try {
    sessionStorage.setItem(chave, JSON.stringify(valor));
  } catch {
    // Aba privada ou cota: segue sem guardar.
  }
}

export function apagar(...chaves: string[]): void {
  try {
    for (const chave of chaves) sessionStorage.removeItem(chave);
  } catch {
    // Nada a fazer.
  }
}
