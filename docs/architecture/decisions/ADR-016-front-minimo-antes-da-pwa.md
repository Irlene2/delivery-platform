# ADR-016 — Entregar front-end mínimo antes da PWA completa

**Status:** Aceita — 16/08/2026 · **emendada em 29/09/2026**: o 15.A começa no
marco 2, e não no 3 — pelo motivo que esta ADR já dava

## Contexto

A PWA completa — manifest, service worker, cache, offline, fila local, push — é
uma frente de trabalho densa e independente. Conduzi-la em paralelo com Saga,
Outbox e idempotência, com uma pessoa, tende a deixar as duas pela metade.

Por outro lado, backend sem interface é portfólio fraco, e tela cedo expõe API
mal desenhada rápido.

## Decisão

Duas entregas:

- **15.A — front mínimo (marco 3).** React, TypeScript, Vite, Tailwind, React
  Router, TanStack Query, cliente HTTP encapsulado, ESLint e Prettier. Escopo:
  autenticação, uma listagem e um formulário.
- **15.B — PWA completa (após o marco 6).** Manifest, service worker, cache,
  offline, push, mapa, React Hook Form, Zod, Playwright, axe-core.

## Consequências

**Positivas** — a API ganha consumidor real cedo; backend e front não competem
pela mesma janela de atenção; o service worker entra quando há o que cachear.

**Negativas** — por vários marcos não há app instalável para demonstrar; se o
projeto parar antes do marco 6, a parte PWA não existirá.

## Alternativas consideradas

- **PWA completa na Fase 2.** Rejeitada: duas frentes densas em paralelo.
- **Nenhum front até o fim do backend.** Rejeitada: API sem consumidor real
  acumula erro de contrato.

## Emenda de 29/09/2026 — antecipado um marco, pelo argumento desta ADR

Esta ADR põe o 15.A no marco 3. A W-A o começa no **marco 2**, e o argumento não
é de cronograma: é a frase que está no "Contexto" acima — *"tela cedo expõe API
mal desenhada rápido"*.

**A G-B3 provou o custo de não ter consumidor.** A primeira rota do catálogo
funcionava, e o contrato congelado que ela publicou descrevia um parâmetro de
consulta único chamado `paginacao`, do tipo objeto e obrigatório: **nenhum
cliente que seguisse o contrato conseguiria chamar a rota.** Faltava
`@ParameterObject` no `Pageable`. Nenhum teste podia perceber, porque o teste
chama a rota, não o contrato — e o `ContratoOpenApiIT` prova que o arquivo não
mudou sem querer, não que ele descreve uma chamada possível.

Só um consumidor acha esse defeito. A ADR-039 deu ao contrato o papel de
especificação; **um consumidor é o que torna essa promessa verificável.**

### O que NÃO muda

O escopo do 15.A continua o desta ADR: autenticação, uma listagem e um
formulário — e a pilha continua a que o `frontend/README.md` já descrevia desde o
commit inicial. A W-A entrega a autenticação; a listagem espera rota.

O 15.B continua depois do marco 6. Nada de service worker, manifest, mapa ou
fila local antes disso.

### O que a antecipação custou, e está registrado

O marco 2 é o catálogo. O front consome o `identity` — que é do marco 1 e está
fechado — e não consome nada do catálogo, porque a rota do cardápio precisa de um
`estabelecimentoId` que o sistema ainda não sabe responder. **A W-A não avança o
marco 2 em nada**; ela paga a dívida de contrato do marco 1 e prepara a do 3.
