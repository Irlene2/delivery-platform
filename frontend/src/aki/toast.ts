/**
 * O aviso no canto da tela, igual ao `toast()` do protótipo: um único elemento
 * `.toast` (estilo em `toast.css`), reaproveitado a cada mensagem.
 */
let temporizador: number | undefined;

export function toast(mensagem: string): void {
  let elemento = document.querySelector<HTMLDivElement>('.toast');
  if (elemento === null) {
    elemento = document.createElement('div');
    elemento.className = 'toast';
    elemento.setAttribute('role', 'status');
    document.body.appendChild(elemento);
  }
  const alvo = elemento;
  alvo.textContent = mensagem;
  alvo.classList.add('show');
  window.clearTimeout(temporizador);
  temporizador = window.setTimeout(() => alvo.classList.remove('show'), 2200);
}
