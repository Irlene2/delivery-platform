# Entrega de 15/11/2026 — o recorte, etapa por etapa

**A decisão é a [ADR-077](../architecture/decisions/ADR-077-o-recorte-da-entrega-de-15-11.md)
(aceita em 09/11/2026).** Este arquivo é o índice: o fluxo que a demonstração mostra, o que cada serviço
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
   mensagem pronta. O **sino** do topo avisa de pedido novo, saída, entrega, cancelamento e Pix a
   conferir.
5. **O entregador, com a conta dele** (`.../entregas`), vê só as entregas que estão com ele,
   marca a saída e a entrega, e registra como recebeu: dinheiro, cartão ou Pix. O Pix fica **a
   conferir**, e a loja confirma no extrato, em Pedidos.
6. **No fim do dia, o administrador abre o fechamento** (`.../fechamento`): total por forma de
   pagamento, por horário, e o **acerto de cada entregador** — repasse = a taxa de cada entrega
   feita; acerto = dinheiro recebido − repasse. O caixa não fecha com pedido em andamento nem com
   Pix a conferir. O histórico mostra as **vendas de um período**, com planilha.

O dashboard da loja mostra as vendas e os pedidos recentes do dia, a partir dos mesmos pedidos.

## Os serviços envolvidos

| Serviço | O que faz nesta entrega |
| --- | --- |
| `gateway` (8080) | Única porta do front. Libera sem token só a vitrine: o cardápio, a taxa, fazer o pedido e acompanhá-lo |
| `identity` (8081) | Conta, login e token — do dono, da equipe e do entregador. O cliente da vitrine **não** tem conta |
| `merchant` (8082) | A loja, o horário, a equipe e o convite. Responde ao `order` o que cada pessoa pode fazer na loja (`VER_PEDIDO`, `ALTERAR_STATUS`, `VER_ENTREGA`, `CONFIGURAR_LOJA`, `VER_VENDAS`, `GERENCIAR_JORNADA`) e se a loja está aberta agora |
| `catalog` (8083) | Seções e produtos, publicação, disponibilidade, correção do cadastro e **foto**; serve o **cardápio público** e o preço que o `order` congela no item |
| `order` (8085) | **O único serviço novo.** O pedido e seus estados, a taxa da loja, o entregador e o pagamento registrado, as entregas de cada entregador e o **caixa do dia** fechado e gravado. Pergunta ao `merchant` se a loja está aberta |

`delivery`, `settlement` e `conversation` continuam esqueleto: nesta entrega, o entregador e o
pagamento registrado são campos do pedido. O pagamento é presencial e o produto não cobra (ADR-023).

## As etapas

| # | Etapa | Estado |
| --- | --- | --- |
| 1 | Base: a branch a partir do repositório do grupo, o front `aki_delivery` em React com o mesmo visual, horário e convite | ✅ |
| 2 | Desenho do pedido e do cardápio público — [`pedido.md`](pedido.md) e a ADR-077 | ✅ |
| 3 | Back-end: `order-service`, cardápio público no `catalog`, rotas no `gateway`, perfil `pedidos` no Compose | ✅ testes de unidade verdes; pilha de pé no Docker Desktop |
| 4 | Front ligado ao `order`: vitrine, checkout, acompanhamento, painel de pedidos, dashboard com dados reais, entregador com conta, fechamento do dia, configurações | ✅ passagem humana completa em 08/11/2026, do pedido ao caixa fechado |
| 5 | WhatsApp: a resposta automática do WhatsApp Business com o link do cardápio, e o aviso ao cliente pelo `wa.me` | ✅ configurado e conferido no celular em 08/11/2026 |
| 4b | O pacote até o marco 6 (01/11/2026): loja fechada não recebe pedido, corrigir produto, foto do produto, sessão de 12 horas, código do cadastro na tela (local), caixa do dia gravado | ✅ testes de unidade e de tela; passagem completa em 08/11/2026 |
| 4c | A loja de teste (01/11/2026): a "Pizzaria da Fixture" com o cardápio do protótipo — 24 produtos, fotos e opções —, a tela de entrada sem o modo demonstração e a hospedagem de teste escrita | ✅ testes de unidade; o cardápio conferido no navegador em 02/11 |
| 4e | A segunda leva de 02/11/2026: a foto de cada item no acompanhamento do pedido, o "Esqueci minha senha" pelo código de verificação e a entrada só pelo telefone, sem Google e Apple | ✅ testes de unidade e de tela; passagem completa em 08/11/2026 |
| 4d | Os ajustes da passagem de 02/11/2026: o sino de notificações, o Pix informado pelo entregador e conferido pela loja, a busca e os filtros do estoque, as vendas por período no fechamento, o menu da loja em "Minhas lojas", a conta nova levada ao "Cadastrar minha loja" e as fotos no painel | ✅ testes de unidade e de tela; passagem completa em 08/11/2026 |
| 6 | Passagem completa no navegador e vídeo da apresentação | ✅ passagem no navegador e no celular, pela Cloudflare, e o vídeo em 08/11/2026. A apresentação é em 16/11/2026 |
| 7 | Os ajustes dos testes no celular (13/11/2026): excluir produto, tirar do cardápio, selo e "pode acabar" gravados pelo painel, termos de uso e aviso de privacidade, textos que quebram linha e o menu lateral que fecha | ✅ testes do `catalog` verdes; testes de unidade e de tela do front |

A ADR-077 foi aceita em 09/11/2026. O contrato OpenAPI e os testes de integração do `order`
ficaram como o próximo passo técnico (veja *"Depois da entrega"*).

## Como subir e testar

1. **Subir:** [`docs/como-testar.md`](../como-testar.md) — na sua máquina (parte B) ou aberto
   para o celular pela Cloudflare (parte C), com o roteiro completo do teste. A referência
   longa, com os tempos medidos e cada problema já visto, é o
   [`docs/como-subir-local.md`](../como-subir-local.md).
2. **Back-end:** em `backend`, `.\gradlew.bat :services:order-service:test` e
   `.\gradlew.bat :services:catalog-service:test`.
3. **Front:** em `frontend`, `npm run typecheck`, `npm run lint`, `npm test` e `npm run dev`
   (porta 5173).
4. **Pela API:** [`testes-pedido.http`](testes-pedido.http), na ordem.
5. **Pelas telas, o roteiro da demonstração.** Para não cadastrar tudo à mão, use a
   **"Pizzaria da Fixture"**, que já vem com o cardápio do protótipo — como ligar e o horário a
   ajustar estão em [`docs/como-subir-local.md`](../como-subir-local.md), *"Testar pedidos com a
   Pizzaria da Fixture"*. Para mostrar a loja nascendo do zero, cadastre outra conta.
   - entrar como dono, cadastrar a loja, criar e publicar dois produtos, definir a taxa;
   - em Equipe, convidar o entregador com a permissão "Entregador"; ele aceita e entra com a
     conta dele, e se apresenta com o nome;
   - numa aba anônima, abrir o link do cardápio e fazer um pedido de entrega em dinheiro;
   - no painel, aceitar, preparar, deixar pronto e entregar ao entregador;
   - na conta do entregador, sair, registrar o dinheiro e entregar;
   - conferir o acompanhamento do cliente, o dashboard e o fechamento do dia.

**Sem o back-end não há mais reserva na tela:** o modo demonstração saiu da entrada em
01/11/2026. A reserva da apresentação passa a ser **o vídeo** (etapa 6).

## Depois da entrega

A entrega está concluída. O que vem a seguir, do mais técnico ao de produto. Esforço: **P** até
meio dia, **M** um ou dois dias, **G** três dias ou mais.

| Próximo passo | Esforço | Observação |
| --- | --- | --- |
| Contrato OpenAPI e testes de integração do `order` | M | Hoje ele tem testes de unidade e o de arquitetura, e os tipos do pedido no front são escritos à mão (`src/api/pedidos.ts`) |
| Uma rodada dos testes do Gradle de todos os serviços | P | Os do `catalog` passaram em 13/11/2026 e os do `identity` em 02/11; numa máquina com pouca memória, um serviço por vez (`--max-workers=1`) |
| Criar e editar grupos de opções pelo painel | M | Hoje só a loja de teste cria grupos |
| Declarar `security: []` nas rotas públicas do `identity` | P | Aviso do lint dos contratos (`redocly.yaml`) |
| Enviar o código de verificação | M a G | SMS e mensagem enviada pela empresa no WhatsApp **custam por mensagem**. O caminho gratuito é o cliente **mandar** o código pelo WhatsApp, com a API oficial. Hoje o código aparece na tela, só no teste |
| Aviso automático ao cliente a cada mudança de status | G | Precisa da API oficial do WhatsApp (conta Meta Business verificada, número próprio, modelos de mensagem aprovados e endereço público com HTTPS). Hoje a loja clica e o WhatsApp abre com a mensagem pronta |
| Hospedagem de produção: servidor, domínio, HTTPS, cópia de segurança | G | A opção gratuita da Oracle serve para teste, sem garantia |
| Revisão jurídica dos termos de uso e do aviso de privacidade | M | Os textos estão no ar desde 13/11/2026 (`/termos` e `/privacidade`); a pauta está em `docs/operacao/revisao-juridica.md` |
| Estoque por quantidade (marco 10) e atendimento com IA (marco 9) | G | Planejados na tabela de marcos do README |
