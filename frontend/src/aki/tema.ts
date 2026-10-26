import { gravar, ler } from './guardado';

/**
 * Modo claro e escuro do protótipo.
 *
 * O `style.css` responde à classe `.dark` e o `login.css` ao atributo
 * `data-theme="dark"`; os dois vão no `<html>`, então um só estado serve às duas
 * folhas sem mudar nenhuma delas.
 */
const CHAVE = 'aki.tema';

export type Tema = 'claro' | 'escuro';

export function temaAtual(): Tema {
  return ler<Tema>(CHAVE, 'claro') === 'escuro' ? 'escuro' : 'claro';
}

export function aplicarTema(tema: Tema): void {
  const raiz = document.documentElement;
  raiz.classList.toggle('dark', tema === 'escuro');
  raiz.setAttribute('data-theme', tema === 'escuro' ? 'dark' : 'light');
  gravar(CHAVE, tema);
}

export function alternarTema(): Tema {
  const proximo: Tema = temaAtual() === 'escuro' ? 'claro' : 'escuro';
  aplicarTema(proximo);
  return proximo;
}
