import { useLayoutEffect } from 'react';
import css from '../estilos/login.css?inline';

/**
 * O `login.css` do protótipo só vale nas telas de entrada.
 *
 * No `aki_delivery` ele era carregado apenas pelo `index.html`. Ele redefine
 * `.topbar`, `.badge`, `.hero` e `*`, que o `style.css` usa nas outras telas —
 * carregado para o app inteiro, mudaria o painel. Por isso entra como texto e é
 * posto no `<head>` só enquanto a tela de entrada está montada, depois do
 * `style.css`, na mesma ordem do protótipo.
 */
export function useEstiloDoLogin(): void {
  useLayoutEffect(() => {
    const estilo = document.createElement('style');
    estilo.dataset.aki = 'login';
    estilo.textContent = css;
    document.head.appendChild(estilo);
    return () => estilo.remove();
  }, []);
}
