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
Sem o back, a tela de entrada tem **"Ver as telas em modo demonstração"**: escolha o
perfil e navegue com os dados de exemplo, sem nenhuma chamada à API.

## O visual: o protótipo `aki_delivery`, em React

As telas são as do protótipo HTML `frontend/aki_delivery`, reescritas em React com
**a mesma marcação e as mesmas classes**. As folhas de estilo são cópias fiéis, em
`src/estilos/` (por isso estão no `.prettierignore`):

| Folha                                    | Onde vale                                                                                                             |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `style.css`, `toast.css`, `produtos.css` | o app inteiro (importadas no `main.tsx`)                                                                              |
| `login.css`                              | só nas telas de entrada — ela redefine `.topbar`, `.badge` e `*`; entra e sai com a tela (`src/aki/estiloDoLogin.ts`) |

O `index.css` importa o Tailwind **sem o preflight**: o reset dele mudaria margens,
títulos e botões das folhas do protótipo. As utilidades continuam disponíveis.

| Rota                                                           | Tela do protótipo                                                                 | Dados                       |
| -------------------------------------------------------------- | --------------------------------------------------------------------------------- | --------------------------- |
| `/entrar`, `/cadastrar`, `/convite/:token`                     | `index.html`                                                                      | `identity`, `merchant`      |
| `/painel`                                                      | escolha da loja, ou "Cadastrar minha loja"                                        | `GET /me/estabelecimentos`  |
| `/painel/lojas/nova`                                           | formulário no visual do protótipo                                                 | `POST /me/estabelecimentos` |
| `/painel/lojas/:id`                                            | `administrador.html` ou `colaborador.html`, pelo **papel** que o servidor informa | pedidos: demonstração       |
| `.../produtos`, `.../produtos/novo`, `.../produtos/:produtoId` | `produtos.html` (lista e editor, com as opções do produto)                        | `catalog`                   |
| `.../disponibilidade`                                          | Estoque                                                                           | `catalog`                   |
| `.../equipe`                                                   | convite para a equipe                                                             | `merchant`                  |
| `.../cardapio`                                                 | `cardapio.html` com os produtos publicados                                        | `catalog`                   |
| `/cliente`, `/cliente/cardapio`, `/cliente/pedidos`            | `cliente.html`, `cardapio.html`, `pedidos.html`                                   | demonstração                |
| `/entregador`                                                  | `entregador.html`                                                                 | demonstração                |

**O que ainda é demonstração** (sem rota no back): pedidos, carrinho, checkout,
ticket, vendas, WhatsApp, entregador e a área do cliente. Ficam em `sessionStorage`
(`src/aki/pedidos.ts`), e os valores são só exibição.

**O que o servidor ainda não sabe**, e a tela diz: alterar nome, tipo, descrição ou
preço de um produto existente; criar grupos de opções; guardar foto e selo. Os
ingredientes vão no fim da descrição (`Ingredientes: a, b, c`) e voltam separados.

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
