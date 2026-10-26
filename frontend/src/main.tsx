import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import { App } from './App';
import { SessaoProvider } from './auth/SessaoProvider';
import { aplicarTema, temaAtual } from './aki/tema';
import './index.css';
// O visual do protótipo `aki_delivery`, sem mudar uma linha: as mesmas folhas.
// (O login.css entra só nas telas de entrada — ver aki/estiloDoLogin.ts.)
import './estilos/style.css';
import './estilos/toast.css';
import './estilos/produtos.css';

/**
 * O TanStack Query saiu na W-B, pelo gatilho que a W-A escreveu aqui: *"se a
 * W-B não o usar, ele sai"*. Ela não usou — duas leituras sem escrita, e a
 * corrida ao trocar de loja resolvida no `useRecurso`. O gatilho de volta está
 * no `frontend/README.md`.
 */
aplicarTema(temaAtual());

const raiz = document.getElementById('raiz');
if (raiz === null) throw new Error('não há #raiz no index.html');

createRoot(raiz).render(
  <StrictMode>
    <BrowserRouter>
      <SessaoProvider>
        <App />
      </SessaoProvider>
    </BrowserRouter>
  </StrictMode>,
);
