# ADR-077 — O recorte da entrega de 15/11: o pedido nasce pelo link do cardápio

- **Status:** proposta
- **Data:** 26/10/2026
- **Rodada:** entrega de 15/11
- **Relacionada:** ADR-075 (o pedido nasce no canal — adiada por esta, não revogada), ADR-006
  (o rascunho pertence à conversa), ADR-009 (modelo de valores do pedido), ADR-021 (catálogo de
  serviços), ADR-023 (o pedido é dono do registro), PRD §5 (P4) e §10

## Contexto

A entrega do curso é em **15/11/2026** (domingo e feriado; a última data útil é 13/11). Restam
três semanas. O caminho da ADR-075 — o 7a de ponta a ponta na conversa, com webhook assinado,
identidade de serviço, cotação, cardápio de quem compra e um número de WhatsApp de verdade —,
seguido do marco 3, **não cabe nelas com qualidade**. Os cinco serviços que o marco 3 em diante
usa (`order`, `payment`, `delivery`, `settlement`, `conversation`) são esqueleto.

## Decisão

> **Até 15/11, o pedido nasce pelo link do cardápio que a conversa entrega.** O cliente chama a
> loja no WhatsApp, a resposta automática do WhatsApp Business manda o link, e o pedido é
> montado e confirmado no cardápio público. O servidor calcula o total.

1. **Um serviço só: o `order`.** Pagamento na entrega (forma, valor recebido, troco, Pix
   conferido) e entregador (o nome) são **campos do pedido** nesta entrega. `payment`,
   `delivery` e `settlement` continuam esqueleto.
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

## Alternativas consideradas

- **A ADR-075 completa antes de 15/11.** Recusada pelo prazo.
- **Menu numerado pelo Twilio Sandbox** (proposta B). Fica como evolução se sobrar tempo depois
  de a proposta A estar de pé.

## Gatilho escrito

**A entrega de 15/11.** Passada ela, esta ADR é revista: o 7a volta à frente, e cada simplificação
acima ganha a sua decisão própria ou sai.
