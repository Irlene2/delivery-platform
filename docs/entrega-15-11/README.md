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
| `merchant` (8082) | A loja, o horário, a equipe e o convite. Responde ao `order` o que cada pessoa pode fazer na loja (`VER_PEDIDO`, `ALTERAR_STATUS`, `VER_ENTREGA`, `CONFIGURAR_LOJA`, `VER_VENDAS`, `GERENCIAR_JORNADA`) e se a loja está aberta agora |
| `catalog` (8083) | Seções e produtos, publicação, disponibilidade, correção do cadastro e **foto**; serve o **cardápio público** e o preço que o `order` congela no item |
| `order` (8085) | **O único serviço novo.** O pedido e seus estados, a taxa da loja, o entregador e o pagamento registrado, as entregas de cada entregador e o **caixa do dia** fechado e gravado. Pergunta ao `merchant` se a loja está aberta |

`delivery`, `settlement` e `conversation` continuam esqueleto: nesta entrega, pagamento na
entrega e entregador são campos do pedido. **O `payment` saiu em 01/11/2026** — não há pagamento
pelo aplicativo (ADR-021, emenda).

## As etapas

| # | Etapa | Estado |
| --- | --- | --- |
| 1 | Base: a branch a partir do repositório do grupo, o front `aki_delivery` em React com o mesmo visual, horário e convite | ✅ |
| 2 | Desenho do pedido e do cardápio público — [`pedido.md`](pedido.md) e a ADR-077 | ✅ |
| 3 | Back-end: `order-service`, cardápio público no `catalog`, rotas no `gateway`, perfil `pedidos` no Compose | ✅ testes de unidade verdes; pilha de pé no Docker Desktop |
| 4 | Front ligado ao `order`: vitrine, checkout, acompanhamento, painel de pedidos, dashboard com dados reais, entregador com conta, fechamento do dia, configurações | ✅ passagem humana até o pedido entregue; o entregador e o fechamento ainda esperam a passagem humana |
| 5 | WhatsApp: a resposta automática do WhatsApp Business com o link do cardápio, e o aviso ao cliente pelo `wa.me` | ⏳ |
| 4b | O pacote até o marco 6 (01/11/2026): loja fechada não recebe pedido, corrigir produto, foto do produto, sessão de 12 horas, código do cadastro na tela (local), caixa do dia gravado | ✅ testes de unidade e de tela; espera os testes do Gradle e a passagem humana |
| 4c | Limpeza de 01/11/2026: o modo demonstração sai da tela de entrada, a "Pizzaria da Fixture" ganha o cardápio do protótipo (24 produtos, fotos e opções), o `payment-service` sai e a hospedagem de teste fica escrita | ✅ testes de unidade; espera a subida com a semeadura ligada |
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

## O que falta, e onde o time pode ajudar

**Prazo do time: 12/11/2026** (a entrega do curso é 15/11). Esforço: **P** até meio dia, **M**
um ou dois dias, **G** três dias ou mais — para uma pessoa que já subiu o projeto. **Peço que
cada um valide:** a tarefa faz sentido? A melhoria vale o esforço até a entrega? O esforço está
certo? Quem pega? Escrevam o nome na coluna e abram uma branch por tarefa (README, *"Como
colaborar"*).

### Para a entrega

| # | Tarefa | Por quê | Esforço | Quem |
| --- | --- | --- | --- | --- |
| 1 | Rodar os testes do Gradle numa máquina com folga de memória, um serviço por vez (`--max-workers=1`) | Ainda não houve uma rodada inteira verde com o Testcontainers | P | |
| 2 | Regenerar os contratos do `merchant`, `catalog` e `identity` (`-Dopenapi.atualizar=true`) e os tipos do front (`npm run tipos`) | As rotas novas de 01/11 não estão no contrato | P | |
| 3 | Contrato OpenAPI e testes de integração do `order` | Os tipos do pedido no front são escritos à mão (`src/api/pedidos.ts`) | M | |
| 4 | Passagem completa no navegador, com a fixture, anotando cada achado | Entregador, fechamento, loja fechada, foto e correção de produto só passaram em testes de tela | M | |
| 5 | Hospedagem de teste pelo túnel da Cloudflare, e liberar `.trycloudflare.com` no `preview.allowedHosts` do Vite | O link precisa abrir no celular de outra pessoa | P | |
| 6 | Configurar a resposta automática do WhatsApp Business com o link do cardápio, e escrever o passo a passo | Etapa 5: sem código, só configuração no celular da loja | P | |
| 7 | Roteiro da apresentação, ensaio e vídeo de reserva | O modo demonstração saiu; sem rede, o vídeo é a reserva | M | |
| 8 | Ler e aprovar a ADR-077 (e as emendas de 01/11) em grupo | É a decisão do recorte, ainda *proposta* | P | |

### Melhorias que podem entrar, se sobrar tempo — validem o esforço

| # | Melhoria | Esforço | Observação |
| --- | --- | --- | --- |
| 9 | Criar e editar grupos de opções pelo painel | M | Hoje só a semeadura cria grupos; a tela avisa que o servidor não sabe |
| 10 | Tirar da tela os botões de Google e Apple, ou deixar o aviso | P | Não há serviço por trás; mexe no visual do protótipo, então decidam juntos |
| 11 | "Esqueci minha senha" de verdade | M | Depende de enviar código — o mesmo problema do item 13 |
| 12 | Declarar `security: []` nas rotas públicas do `identity` | P | Dívida do lint dos contratos (`redocly.yaml`) |

### Depois da entrega

| # | Item | Esforço | Observação |
| --- | --- | --- | --- |
| 13 | Enviar o código de verificação por SMS ou WhatsApp | M | **Tem custo por mensagem**; hoje o código aparece na tela, só no teste |
| 14 | Aviso automático ao cliente a cada mudança de status | G | Precisa da API oficial do WhatsApp (marco 7) |
| 15 | Hospedagem de produção: servidor, domínio, HTTPS, cópia de segurança | G | A opção gratuita da Oracle serve para teste, sem garantia |
| 16 | Termos de uso e política de privacidade | M | Pauta em `docs/operacao/revisao-juridica.md` |
| 17 | Estoque por quantidade (marco 10) e atendimento com IA (marco 9) | G | Planejados na tabela de marcos |
