/** Valores e datas como o protótipo `aki_delivery` mostrava. */
export const dinheiro = (valor: number | string): string =>
  Number(valor).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const dataEHora = (iso: string): string =>
  new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
