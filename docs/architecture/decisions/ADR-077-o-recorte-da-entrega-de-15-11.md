# ADR-077 — O recorte da entrega de 15/11: o pedido nasce pelo link do cardápio

- **Status:** aceita — 09/11/2026, com as decisões de 26/10, 31/10, 01/11 e 02/11/2026
- **Data:** 26/10/2026
- **Rodada:** entrega de 15/11
- **Relacionada:** ADR-075 (o pedido montado dentro da conversa — vira melhoria futura), ADR-006
  (o rascunho pertence à conversa), ADR-009 (modelo de valores do pedido), ADR-021 (catálogo de
  serviços), ADR-023 (o pedido registra o pagamento), PRD §5 (P4) e §10

## Contexto

A entrega do curso é em **15/11/2026** (domingo e feriado; a última data útil é 13/11). Restam
três semanas. O caminho da ADR-075 — o 7a de ponta a ponta na conversa, com webhook assinado,
identidade de serviço, cotação, cardápio de quem compra e um número de WhatsApp de verdade —,
seguido do marco 3, **não cabe nelas com qualidade**. Os serviços que o marco 3 em diante
usa (`order`, `delivery`, `settlement`, `conversation`) eram esqueleto.

## Decisão

> **Até 15/11, o pedido nasce pelo link do cardápio que a conversa entrega.** O cliente chama a
> loja no WhatsApp, a resposta automática do WhatsApp Business manda o link, e o pedido é
> montado e confirmado no cardápio público. O servidor calcula o total.

1. **Um serviço só: o `order`.** Pagamento na entrega (forma, valor recebido, troco, Pix
   conferido) e entregador (o nome) são **campos do pedido** nesta entrega. `delivery` e
   `settlement` continuam esqueleto; o pagamento não é serviço (ADR-023).
2. **O preço vem do `catalog`, por chamada síncrona** no momento de criar o pedido. Sem evento
   entre serviços nesta entrega.
3. **Uma rota pública de leitura do cardápio**, só com o que está publicado, para o cardápio de
   quem compra.
4. **O aviso ao cliente** é o link `wa.me` com a mensagem pronta, que o atendente toca. Sem API
   de WhatsApp nesta entrega.
5. **O front é o React com o visual do protótipo `aki_delivery`** (as mesmas folhas de estilo,
   em `frontend/src/estilos/`).

## Consequências

- **A ADR-075 fica adiada, não revogada.** Depois de 15/11, o 7a entra como ela diz, e o
  pedido passa a nascer dentro da conversa; a rota pública e o `order` desta entrega são o que
  ele vai reaproveitar.
- **A ADR-006 é contrariada nesta entrega:** existe carrinho no cardápio público. Ele é estado da
  tela; o total que vale é o que o servidor calcula.
- **O risco do §11 do PRD continua mitigado:** o cardápio público é uma segunda entrada além do
  WhatsApp.

## Decisões tomadas depois, dentro deste recorte

Registradas aqui para a ADR não descrever menos do que o código faz. O detalhe de cada uma está
em [`docs/entrega-15-11/pedido.md`](../../entrega-15-11/pedido.md).

- **26/10/2026 — entrega e retirada, com taxa fixa por loja.** O cliente escolhe no checkout; na
  retirada a taxa é zero. A taxa mora no `order` (configuração de pedidos da loja), e não no
  `merchant`, para não mexer no agregado da loja nesta entrega. A área por bairro (ADR-020)
  continua sendo o modelo do produto.
- **27/10/2026 — loja que não existe devolve cardápio vazio (200)**, e não 404, na rota
  pública: o `catalog` só conhece produtos, e o `merchant` não tem rota pública que diga se uma
  loja existe. Gatilho para o 404: o `merchant` ganhar essa rota.
- **31/10/2026 — o entregador da equipe entra com a conta dele.** A loja o convida pela Equipe
  com a permissão `VER_ENTREGA`; ele vê só as entregas que estão com ele, marca a saída e a
  entrega, e registra dinheiro ou cartão — o Pix é conferido pela loja. Como a equipe do
  `merchant` não guarda nome, o `order` guarda o nome com que o entregador se apresenta. O
  entregador avulso, sem conta, continua possível só pelo nome. **A P2 não muda:** não há
  corrida aberta nem aceite.
- **31/10/2026 — o repasse é a taxa de entrega de cada pedido entregue**, e o acerto do dia é
  `dinheiro recebido − repasse`. O **fechamento do dia é um relatório** calculado na tela a
  partir dos pedidos, com impressão; fechar o caixa com registro e histórico continua sendo o
  marco 6, no `settlement`. Confirmado como regra do produto em 31/10/2026 (emenda ao PRD §10): o entregador
  recebe por entrega, a taxa cobrada do cliente, no lugar das modalidades da ADR-022.

- **01/11/2026 — loja fora do horário não recebe pedido.** O `merchant` ganha uma rota pública,
  `GET /api/v1/storefront/{id}/status`, que devolve o que o agregado da loja já sabia calcular
  (`estaAberta`, com horário, pausa e fuso) e a próxima abertura. O `order` a consulta ao montar
  a vitrine e ao receber o pedido: fechada é **409** com a frase de quando abre; sem resposta do
  `merchant` é **503**, falha fechada. Loja sem horário fica fechada. Pública porque a situação
  da loja é o que ela mesma divulga; sem credencial entre serviços, como a ADR-045 manda. Com
  ela, o identificador que não é de loja passa a ter resposta própria (**404**) — o gatilho que
  o `pedido.md` §2.1 deixou para o cardápio vazio.

- **01/11/2026 — o pacote até o marco 6.** Corrigir o cadastro de produto (`PUT` do produto no
  `catalog`, com C1 e C2 valendo para o publicado); **a foto do produto** numa coleção própria do
  MongoDB do `catalog`, servida por rota pública, e não no MinIO do `catalogo.md` §9 — sem
  serviço novo até a entrega; **o caixa do dia apurado e gravado no `order`**, e não no
  `settlement` que o PRD desenha — o `order` já tem os pedidos, e montar outro serviço do zero não
  cabe até 15/11. As emendas da ADR-037 (sessão de 12 horas) e da ADR-042 (o código na tela, só
  com a bandeira de desenvolvimento) são da mesma rodada.

- **01/11/2026 — o pagamento é presencial.** O cliente paga na entrega ou na retirada, e a loja
  registra no pedido; o produto não cobra (ADR-023, ADR-021).
- **02/11/2026 — o entregador informa Pix.** Na porta, ele registra dinheiro, cartão ou Pix; o Pix
  fica **a conferir** e a loja confirma em Pedidos, pela rota `PUT .../orders/{id}/pix-check`. O
  caixa do dia não fecha com Pix a conferir.
- **02/11/2026 — as vendas de um período.** O histórico do fechamento aceita `de` e `ate` (até um
  ano) e soma os dias fechados; o CSV sai com um dia por linha.
- **02/11/2026 — o sino do administrador.** Os avisos saem dos pedidos do dia, relidos a cada 30
  segundos: pedido novo, saída e entrega, retirada, cancelamento, pagamento registrado e Pix a
  conferir. O que a própria loja faz no painel não avisa. Sem serviço de notificação.
- **01/11/2026 — a loja de teste é a da fixture.** O link "Ver as telas em modo demonstração"
  saiu da tela de entrada; o cardápio de exemplo do protótipo (seis seções, 24 produtos, fotos e
  as opções do hambúrguer) passou a ser gravado no banco da "Pizzaria da Fixture" pela
  semeadura do `catalog` (ADR-059, emenda).

## Alternativas consideradas

- **A ADR-075 completa antes de 15/11.** Recusada pelo prazo.
- **Menu numerado pelo Twilio Sandbox** (proposta B). Fica como evolução se sobrar tempo depois
  de a proposta A estar de pé.

## Gatilho escrito

**A entrega de 15/11.** Passada ela, esta ADR é revista: o 7a volta à frente, e cada simplificação
acima ganha a sua decisão própria ou sai.
