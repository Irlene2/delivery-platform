# ADR-023 — O pedido registra o pagamento; o produto não cobra

**Status:** Aceita — 23/08/2026 · **revisada em 02/11/2026** para a proposta atual: o pagamento é
presencial e registrado no pedido
**Relacionada:** ADR-009 (valores), ADR-021 (catálogo de serviços), ADR-077 (o recorte da entrega
de 15/11)
**Detalha:** `docs/dominio/pedido.md` §7, `docs/entrega-15-11/pedido.md` §5
**Invariantes do `CLAUDE.md`:** 1 (nenhuma conclusão sem pagamento registrado), 6 (imagem não
confirma Pix), 8 (banco por serviço)

## Contexto

O comércio de bairro recebe na porta e no balcão: dinheiro com troco, cartão na maquininha da
própria loja e Pix na conta da loja. O dinheiro nunca passa pela plataforma, e a dor que o produto
resolve (PRD §1) não é cobrar — é **saber, no fim do dia, como cada pedido foi pago** e quanto cada
entregador tem a acertar.

Duas perguntas precisavam de resposta antes do primeiro pedido: quem guarda o registro do
pagamento, e se o produto cobra.

## Decisão

### O `order` é dono do registro do pagamento

O pagamento é um **registro dentro do agregado `Pedido`**, no `order-service`:

| No pedido | O que guarda |
|---|---|
| Pagamento **declarado** | O que o cliente disse que pagaria no checkout, e o troco que pediu |
| Pagamento **registrado** | Como foi pago de verdade: a forma, o valor recebido, o troco devolvido e, no Pix, se a loja conferiu no extrato |

Os dois ficam separados porque divergem no caso normal: o cliente pede dinheiro e acaba pagando no
cartão. A invariante 1 — nenhuma entrega ou retirada conclui sem pagamento registrado — só é
verificável porque quem controla a transição de estado controla também o registro.

### O produto não cobra

Não há cobrança online, provedor de pagamento, QR dinâmico nem webhook. **O aplicativo registra; não
movimenta dinheiro.** Por isso não existe serviço de pagamento no catálogo (ADR-021).

### O Pix é conferido por quem vê o extrato

O Pix cai na conta da loja, e comprovante na tela do cliente não confirma nada (invariante 6). Dois
caminhos, os dois terminando na conferência da loja:

1. **A loja confere antes:** registra o Pix já marcado como conferido no extrato.
2. **O entregador informa na porta** (02/11/2026): o pedido fica com o Pix **a conferir**, a entrega
   pode ser concluída, e a loja confirma depois em Pedidos. **O caixa do dia não fecha** enquanto
   houver Pix a conferir.

## Consequências

- Um lugar só para o dinheiro do pedido: o dashboard, o fechamento do dia e o acerto dos
  entregadores leem o mesmo registro.
- Nenhum dado de cartão ou conta bancária passa pelo sistema — sem PCI, sem segredo de provedor.
- O produto depende da conferência humana do Pix. É o que a loja já faz hoje, e o fechamento
  obriga que ela aconteça antes de fechar o caixa.

## Alternativas consideradas

- **Cobrança Pix pelo aplicativo, com provedor e webhook.** Era o desenho de 23/08. Fora da
  proposta atual: custa taxa por transação, cadastro com provedor e uma fronteira inteira de
  segurança, para resolver um problema que a loja não tem — ela já recebe Pix na própria conta.
- **Um serviço de registro de pagamentos separado do pedido.** Rejeitada: a invariante 1 deixaria de
  ser verificável dentro de uma transação.

## Gatilho escrito

Uma loja pedir cobrança antecipada (pagar ao pedir). Aí a decisão volta, do zero.
