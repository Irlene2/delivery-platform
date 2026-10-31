# Entrega de 15/11/2026 — o recorte, etapa por etapa

**A decisão é a [ADR-077](../architecture/decisions/ADR-077-o-recorte-da-entrega-de-15-11.md)
(proposta).** Este arquivo é o índice: o fluxo que a demonstração mostra, o que cada serviço
faz nele, o estado de cada etapa e como testar. As rotas e regras do pedido estão em
[`pedido.md`](pedido.md); os testes pela API, em [`testes-pedido.http`](testes-pedido.http).

## O fluxo, do ponto de vista de quem usa

1. **O dono cadastra a loja do zero.** Entra, cai em "Minhas lojas", cadastra a loja, cria as
   seções e os produtos e publica. Em Configurações define a taxa de entrega fixa, se aceita
   entrega e retirada, e copia o **link do cardápio**.
2. **O link vai para a resposta automática do WhatsApp Business** da loja (etapa 5). Quem chama
   a loja no WhatsApp recebe o link.
3. **O cliente monta o pedido no cardápio público** (`/loja/:id`), sem conta: carrinho, entrega
   ou retirada, endereço, forma de pagamento e troco. O servidor calcula o total com o preço do
   `catalog` — o total da tela é só exibição. O cliente recebe um código e acompanha em
   `/pedido/:codigo`.
4. **A loja opera o pedido no painel** (`.../pedidos`): aceita, prepara, deixa pronto, entrega a
   um entregador da equipe (ou avulso, pelo nome) e avisa o cliente pelo link `wa.me` com a
   mensagem pronta.
5. **O entregador, com a conta dele** (`.../entregas`), vê só as entregas que estão com ele,
   marca a saída e a entrega, e registra quanto recebeu em dinheiro ou cartão. O Pix é
   conferido pela loja.
6. **No fim do dia, o administrador abre o fechamento** (`.../fechamento`): total por forma de
   pagamento, por horário, e o **acerto de cada entregador** — repasse = a taxa de cada entrega
   feita; acerto = dinheiro recebido − repasse.

O dashboard da loja mostra as vendas e os pedidos recentes do dia, a partir dos mesmos pedidos.

## Os serviços envolvidos

| Serviço | O que faz nesta entrega |
| --- | --- |
| `gateway` (8080) | Única porta do front. Libera sem token só a vitrine: o cardápio, a taxa, fazer o pedido e acompanhá-lo |
| `identity` (8081) | Conta, login e token — do dono, da equipe e do entregador. O cliente da vitrine **não** tem conta |
| `merchant` (8082) | A loja, o horário, a equipe e o convite. Responde ao `order` o que cada pessoa pode fazer na loja (`VER_PEDIDO`, `ALTERAR_STATUS`, `VER_ENTREGA`, `CONFIGURAR_LOJA`) |
| `catalog` (8083) | Seções e produtos, publicação e disponibilidade; serve o **cardápio público** e o preço que o `order` congela no item |
| `order` (8085) | **O único serviço novo.** O pedido e seus estados, a taxa da loja, o entregador e o pagamento registrado, as entregas de cada entregador |

`payment`, `delivery`, `settlement` e `conversation` continuam esqueleto: nesta entrega,
pagamento na entrega e entregador são campos do pedido.

## As etapas

| # | Etapa | Estado |
| --- | --- | --- |
| 1 | Base: a branch a partir do repositório do grupo, o front `aki_delivery` em React com o mesmo visual, horário e convite | ✅ |
| 2 | Desenho do pedido e do cardápio público — [`pedido.md`](pedido.md) e a ADR-077 | ✅ |
| 3 | Back-end: `order-service`, cardápio público no `catalog`, rotas no `gateway`, perfil `pedidos` no Compose | ✅ testes de unidade verdes; pilha de pé no Docker Desktop |
| 4 | Front ligado ao `order`: vitrine, checkout, acompanhamento, painel de pedidos, dashboard com dados reais, entregador com conta, fechamento do dia, configurações | ✅ passagem humana até o pedido entregue; o entregador e o fechamento ainda esperam a passagem humana |
| 5 | WhatsApp: a resposta automática do WhatsApp Business com o link do cardápio, e o aviso ao cliente pelo `wa.me` | ⏳ |
| 6 | Testes que faltam (contrato e integração do `order`), passagem completa no navegador, ensaio da demonstração e vídeo de reserva | ⏳ |

Pendente fora das etapas: a aprovação da ADR-077 pelo grupo. O que foi assumido como dívida
está em [`docs/dividas.md`](../dividas.md), seção *"As da entrega de 15/11"*.

## Como subir e testar

1. **Subir:** [`docs/como-subir-local.md`](../como-subir-local.md) — perfil `marco2` mais
   `pedidos`. No Windows com Docker Desktop, a seção 0.1.
2. **Back-end:** em `backend`, `.\gradlew.bat :services:order-service:test` e
   `.\gradlew.bat :services:catalog-service:test`.
3. **Front:** em `frontend`, `npm run typecheck`, `npm run lint`, `npm test` e `npm run dev`
   (porta 5173).
4. **Pela API:** [`testes-pedido.http`](testes-pedido.http), na ordem.
5. **Pelas telas, o roteiro da demonstração:**
   - entrar como dono, cadastrar a loja, criar e publicar dois produtos, definir a taxa;
   - em Equipe, convidar o entregador com a permissão "Entregador"; ele aceita e entra com a
     conta dele, e se apresenta com o nome;
   - numa aba anônima, abrir o link do cardápio e fazer um pedido de entrega em dinheiro;
   - no painel, aceitar, preparar, deixar pronto e entregar ao entregador;
   - na conta do entregador, sair, registrar o dinheiro e entregar;
   - conferir o acompanhamento do cliente, o dashboard e o fechamento do dia.

**Sem o back-end**, a tela de entrada tem "Ver as telas em modo demonstração", com dados de
exemplo — serve de reserva na apresentação, não de teste.
