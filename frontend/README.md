# Front-end

Scaffoldado na rodada **W-A**, em 29/09/2026 — um marco antes do que a ADR-016
previa, pelo motivo que ela mesma dá (_"tela cedo expõe API mal desenhada
rápido"_). Ver a emenda de 29/09/2026 à ADR-016.

**Antes de escrever tela**, leia `docs/front/premissas-do-front.md`: ele lista o
que este produto **decidiu não ter**, e por qual premissa.

## Como rodar

    npm install
    npm run tipos      # gera src/api/generated/ de contracts/openapi/*.json, já formatado
    npm run dev                             # porta 5173, fixa — é a origem que o gateway aceita

Antes de subir para o Git: `npm run typecheck`, `npm run lint` e `npm test`.

O back precisa estar no ar: o `gateway` em 8080, com `identity`, `merchant` e `catalog` — e o
`order` para os pedidos (perfil `pedidos`, em `docs/como-subir-local.md`).
**Para testar, use a "Pizzaria da Fixture"**, gravada no banco com o cardápio de exemplo do
protótipo (`docs/como-subir-local.md`, *"Testar pedidos com a Pizzaria da Fixture"*). O link
"Ver as telas em modo demonstração" saiu da tela de entrada em 01/11/2026.

## O visual: o protótipo `aki_delivery`, em React

As telas são as do protótipo HTML `aki_delivery` (que **não** está neste repositório),
reescritas em React com **a mesma marcação e as mesmas classes**. As folhas de estilo são cópias fiéis, em
`src/estilos/` (por isso estão no `.prettierignore`):

| Folha                                    | Onde vale                                                                                                             |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `style.css`, `toast.css`, `produtos.css` | o app inteiro (importadas no `main.tsx`)                                                                              |
| `login.css`                              | só nas telas de entrada — ela redefine `.topbar`, `.badge` e `*`; entra e sai com a tela (`src/aki/estiloDoLogin.ts`) |

O `index.css` importa o Tailwind **sem o preflight**: o reset dele mudaria margens,
títulos e botões das folhas do protótipo. As utilidades continuam disponíveis. `pedidos.css`
é a única folha nova: as telas de pedido, fechamento e entregas, no mesmo vocabulário.

**As fontes moram no `index.html`** (Inter, Material Icons e Material Symbols), e não em
`@import` dentro do CSS: as telas de entrada usam a classe `material-icons` e as internas
`material-symbols-outlined`. Sem a fonte certa, o ícone aparece como texto (`light_mode`,
`visibility`, `lock`).

As rotas, por quem as usa:

| Rota                                                           | Quem usa                                                                      | Tela do protótipo                                                                                                                                                      | Dados                                                 |
| -------------------------------------------------------------- | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- |
| `/entrar`, `/cadastrar`, `/convite/:token`                     | todos                                                                         | `index.html`                                                                                                                                                           | `identity`, `merchant`                                |
| `/loja/:id`                                                    | **cliente, sem conta** — é o link que a resposta automática do WhatsApp manda | `cardapio.html` com carrinho e checkout                                                                                                                                | `catalog` (cardápio público), `order` (taxa e pedido) |
| `/loja/:id/pedidos`                                            | cliente                                                                       | `pedidos.html`, os pedidos feitos neste navegador                                                                                                                      | `order`                                               |
| `/pedido/:codigo`                                              | cliente                                                                       | acompanhamento do pedido pelo código                                                                                                                                   | `order`                                               |
| `/painel`                                                      | quem tem conta                                                                | "Minhas lojas": escolher a loja ou **cadastrar a primeira**                                                                                                            | `GET /me/estabelecimentos`                            |
| `/painel/lojas/nova`                                           | dono novo                                                                     | cadastro da loja, do zero                                                                                                                                              | `POST /me/estabelecimentos`                           |
| `/painel/lojas/:id`                                            | administrador ou colaborador, pelo **papel** que o servidor informa           | `administrador.html` / `colaborador.html` — vendas e pedidos recentes do dia, e os primeiros passos para loja nova                                                     | `order`                                               |
| `.../pedidos`                                                  | quem tem `VER_PEDIDO`                                                         | a fila do dia: aceitar, preparar, despachar com entregador, registrar o pagamento, conferir o Pix informado pelo entregador, avisar pelo WhatsApp                                                                | `order`                                               |
| `.../entregas`                                                 | **entregador** (`VER_ENTREGA`)                                                | as entregas que a loja deu a ele: saiu, entregue, como recebeu (dinheiro, cartão ou Pix a conferir)                                                                                                       | `order`                                               |
| `.../fechamento`                                               | administrador                                                                 | fechamento do dia: soma ao vivo até fechar; depois, o caixa **gravado** — formas de pagamento, conferência do dinheiro, **acerto de cada entregador**, as vendas de um período e CSV | `order`                                               |
| `.../configuracoes`                                            | administrador                                                                 | taxa de entrega fixa, se aceita entrega e se aceita retirada; o link do cardápio para pôr na resposta automática                                                       | `order`                                               |
| `.../produtos`, `.../produtos/novo`, `.../produtos/:produtoId` | administrador                                                                 | `produtos.html` (lista e editor, com as opções do produto): corrige nome, preço, descrição e seção, e põe a **foto** (reduzida no navegador)                           | `catalog`                                             |
| `.../disponibilidade`                                          | quem cuida do estoque                                                         | Estoque, com busca pelo nome e filtro por categoria e situação                                                                                                                                                                | `catalog`                                             |
| `.../equipe`                                                   | administrador                                                                 | convite para a equipe, entregador incluído                                                                                                                             | `merchant`                                            |
| `.../horario`                                                  | administrador                                                                 | horário de funcionamento                                                                                                                                               | `merchant`                                            |
| `.../cardapio`                                                 | painel                                                                        | `cardapio.html` com os produtos publicados                                                                                                                             | `catalog`                                             |
| `/cliente`, `/cliente/cardapio`, `/cliente/pedidos`            | modo demonstração                                                             | `cliente.html`, `cardapio.html`, `pedidos.html`                                                                                                                        | demonstração                                          |

`/entregador` não existe mais como tela: redireciona para `/painel`, e o entregador entra na
loja dele e cai em `.../entregas`.

**O que ainda é demonstração:** só a área `/cliente`, que mostra as telas com dados de exemplo
(`src/pedido/demonstracao.ts`) sem chamar a API. O modo demonstração do painel (`src/aki/demo.ts`)
continua no código, sem porta de entrada na tela desde 01/11/2026. O carrinho do cliente é estado da tela (`src/aki/pedidos.ts`); o total que vale
é o que o `order` devolve.

**O sino do topo** (administrador) relê os pedidos do dia a cada 30 segundos e avisa do que não foi
feito no painel: pedido novo, saída e entrega, retirada, cancelamento, pagamento e Pix a conferir
(`src/painel/notificacoes.ts`). O contador é só do que ainda não foi visto nesta aba.

**A entrada é só pelo telefone.** Os botões de Google e Apple do protótipo saíram em 02/11/2026:
a conta é o telefone. **"Esqueci minha senha"** (`/recuperar-senha`) pede o código ao `identity` e
troca a senha; com `DELIVERY_CODIGO_NA_TELA=true`, o código aparece na tela, como no cadastro.

**O acompanhamento do pedido mostra a foto de cada item**, lida do cardápio público da loja
(`src/cliente/useFotosDaLoja.ts`); produto que saiu do cardápio fica na imagem padrão.

**O que o servidor ainda não sabe**, e a tela diz: mudar a posição ou o "pode acabar" de um
produto que já existe; criar grupos de opções; guardar o selo. Os ingredientes vão no fim da
descrição (`Ingredientes: a, b, c`) e voltam separados.

**O código do cadastro na tela:** com `DELIVERY_CODIGO_NA_TELA=true` no `.env` (só local e no
ambiente de teste), o `identity` devolve o código e a tela de cadastro o mostra e preenche.

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
