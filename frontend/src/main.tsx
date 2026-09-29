import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router';
import { App } from './App';
import { SessaoProvider } from './auth/SessaoProvider';
import './index.css';

/**
 * O TanStack Query entra agora, vazio de uso, e é uma escolha discutível que
 * fica registrada: a ADR-016 o nomeia no escopo 15.A, e a primeira tela que lê
 * dados é a rodada seguinte. Ele está aqui montado e sem nenhuma consulta —
 * "peça que nunca rodou" —, com um gatilho: se a W-B não o usar, ele sai.
 *
 * `retry: 0` porque as duas recusas deste sistema são definitivas: 401 não
 * melhora ao repetir, e 403 é fail-closed por desenho. Repetir só multiplicaria
 * a carga no `merchant`, que já responde uma consulta de autorização por
 * requisição enquanto não há cache (G-B4).
 */
const consultas = new QueryClient({
  defaultOptions: { queries: { retry: 0, refetchOnWindowFocus: false } },
});

const raiz = document.getElementById('raiz');
if (raiz === null) throw new Error('não há #raiz no index.html');

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={consultas}>
      <BrowserRouter>
        <SessaoProvider>
          <App />
        </SessaoProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
