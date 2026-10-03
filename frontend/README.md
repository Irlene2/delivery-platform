# Front-end

Scaffoldado na rodada **W-A**, em 29/09/2026 — um marco antes do que a ADR-016
previa, pelo motivo que ela mesma dá (_"tela cedo expõe API mal desenhada
rápido"_). Ver a emenda de 29/09/2026 à ADR-016.

**Antes de escrever tela**, leia `docs/front/premissas-do-front.md`: ele lista o
que este produto **decidiu não ter**, e por qual premissa.

## Como rodar

    npm install
    npm run tipos      # gera src/api/generated/ de contracts/openapi/*.json
    npm run dev        # porta 5173, fixa — é a origem que o gateway aceita

O back precisa estar no ar: `identity-service` em 8081 e o `gateway` em 8080.

## 15.A — front mínimo (marco 3)

React, TypeScript, Vite, Tailwind CSS, React Router, TanStack Query, cliente
HTTP encapsulado, ESLint e Prettier.

> **30/09/2026, W-B — o TanStack Query saiu.** Ele estava nesta lista e no
> `package.json` desde a W-A, montado no `main.tsx` e sem uma consulta: a W-A
> deixou escrito que a rodada da primeira tela decidiria entre justificá-lo e
> removê-lo. A primeira tela tem **duas leituras e nenhuma escrita**, e isso não
> paga o provedor, a configuração e um segundo vocabulário de estado. No lugar
> ficou `src/api/useRecurso.ts`, um hook pequeno com um teste que prova a
> corrida ao trocar de loja — medido: com a guarda desligada, ele fica
> vermelho.
>
> **Gatilho para trazê-lo de volta:** a primeira escrita que precise invalidar
> leitura de **outra** tela. Aí o problema deixa de ser buscar e passa a ser
> coerência entre telas, que é o que ele resolve e o `useRecurso` não resolve.

Escopo: autenticação, uma listagem e um formulário. Responsivo desde o primeiro
componente. O objetivo é dar à API um consumidor real cedo, não construir a
interface final.

## 15.B — PWA completa (após o marco 6)

Manifest, service worker, cache do shell, tela offline, fila local, atualização
segura do SW e push. Somam-se React Hook Form, Zod, MapLibre/Leaflet, Playwright
e axe-core.

Scaffoldar agora seria criar dependências para manter sem nada que as exercite.
