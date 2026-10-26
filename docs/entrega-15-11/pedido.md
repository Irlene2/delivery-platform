# O pedido da entrega de 15/11 — rotas, campos e estados

**Depende da ADR-077** (proposta). Este documento é o desenho que a etapa 3 implementa. O
OpenAPI de `contracts/openapi/` **não é escrito à mão** (ADR-039): ele sai do código, e este
texto é o que o código precisa cumprir.

Decisões tomadas em 26/10/2026:

- **Entrega e retirada.** O cliente escolhe no checkout. Na retirada, a taxa é **zero** (não
  nula) e não há endereço.
- **Taxa de entrega fixa por loja**, definida pelo administrador. Mora no `order`, e não no
  `merchant`, para esta entrega não mexer no agregado da loja.

## 1. Quem fala com quem

```
cliente (cardápio público) ──► gateway ──► catalog   GET  cardápio publicado
                                      └──► order     POST pedido · GET acompanhamento
painel (com token)        ──► gateway ──► order     lista, status, entregador, pagamento, taxa
order                     ──► catalog   GET cardápio publicado (preço e vendável, na hora do pedido)
order                     ──► merchant  GET /internal/merchants/{id}/me (permissão, como o catalog faz)
```

**O preço nunca vem do navegador.** O `order` lê o produto no `catalog` no momento de criar o
pedido e congela o nome e o preço no item (PRD H4.2, ADR-009). O total da tela do cliente é
exibição; o que vale é o da resposta.

## 2. Rotas públicas (sem token)

O gateway libera só estas, como já faz com `GET /api/v1/convites/*` (ADR-044).

### 2.1 `GET /api/v1/storefront/{merchantId}/menu` → `catalog`

O cardápio de quem compra (a porta da ADR-018): **só produto publicado** (`ATIVO`), com a seção.

```json
{
  "categorias": [{ "id": "…", "nome": "Pizzas", "ordem": 0 }],
  "produtos": [
    {
      "id": "…", "categoriaId": "…", "nome": "Pizza margherita",
      "descricao": "…", "precoBase": 49.90, "ordem": 0,
      "disponibilidade": "DISPONIVEL", "vendavel": true
    }
  ]
}
```

Loja inexistente: **404**. Rascunho e inativo não aparecem.

### 2.2 `GET /api/v1/storefront/{merchantId}/orders/settings` → `order`

```json
{ "taxaDeEntrega": 5.00, "aceitaEntrega": true, "aceitaRetirada": true }
```

Loja que nunca configurou: taxa **0,00**, as duas modalidades aceitas.

### 2.3 `POST /api/v1/storefront/{merchantId}/orders` → `order`

```json
{
  "cliente": { "nome": "Maria Souza", "telefone": "21999990000" },
  "modalidade": "ENTREGA",
  "endereco": { "logradouro": "Rua A, 10", "bairro": "Centro", "complemento": "ap 2" },
  "itens": [{ "produtoId": "…", "quantidade": 2, "observacao": "sem cebola" }],
  "pagamento": { "forma": "DINHEIRO", "trocoPara": 100.00 }
}
```

| Campo | Regra |
| --- | --- |
| `cliente.nome`, `cliente.telefone` | obrigatórios; o telefone é normalizado como no `identity` |
| `modalidade` | `ENTREGA` ou `RETIRADA` |
| `endereco` | obrigatório na entrega; **recusado** na retirada |
| `itens` | ao menos um; `quantidade` de 1 a 99 |
| `pagamento.forma` | `DINHEIRO`, `CARTAO` ou `PIX` — **na entrega ou no balcão** |
| `pagamento.trocoPara` | só em `DINHEIRO`; se vier, **maior ou igual ao total** |

**201**, com `Location` para o acompanhamento:

```json
{
  "id": "…", "numero": "000123", "codigoDeAcompanhamento": "c3f9…",
  "status": "RECEBIDO", "modalidade": "ENTREGA",
  "itens": [{ "produtoId": "…", "nome": "Pizza margherita", "precoUnitario": 49.90,
              "quantidade": 2, "observacao": "sem cebola", "subtotal": 99.80 }],
  "subtotal": 99.80, "taxaDeEntrega": 5.00, "total": 104.80,
  "pagamento": { "forma": "DINHEIRO", "trocoPara": 110.00 },
  "criadoEm": "2026-11-05T21:14:03Z"
}
```

**Recusas (400, com `detail` que nomeia o problema — passagem de 19/10, item 16):**
produto que não existe ou não está publicado ("O produto X não está no cardápio"), produto não
vendável ("X acabou"), endereço ausente na entrega, troco menor que o total, modalidade que a loja
não aceita. Loja inexistente: **404**. `catalog` fora do ar: **503**, e o pedido **não** é criado.

### 2.4 `GET /api/v1/storefront/orders/{codigoDeAcompanhamento}` → `order`

O que o cliente acompanha: número, status, itens, valores, modalidade. **Sem telefone nem
endereço** na resposta (ADR-013). O código é um UUID aleatório; sem ele, não há como listar
pedidos de ninguém. Inexistente: **404**.

## 3. Rotas do painel (com token)

A permissão é resolvida no `merchant` a cada requisição (ADR-011), pelo mesmo caminho do
`catalog`. Recurso de outra loja é **404** (ADR-056).

| Rota | Permissão | O que faz |
| --- | --- | --- |
| `GET /api/v1/merchants/{id}/orders?status=&dia=` | `VER_PEDIDO` | lista, do mais novo para o mais velho; `dia` no fuso da loja, padrão hoje |
| `GET /api/v1/merchants/{id}/orders/{pedidoId}` | `VER_PEDIDO` | o pedido inteiro, com telefone e endereço |
| `PUT /api/v1/merchants/{id}/orders/{pedidoId}/status` | `ALTERAR_STATUS` | `{ "status": "EM_PREPARO", "motivo": null }` — transição da §4 |
| `PUT /api/v1/merchants/{id}/orders/{pedidoId}/courier` | `ALTERAR_STATUS` | `{ "nome": "Carlos" }` — o entregador, só em `ENTREGA` |
| `PUT /api/v1/merchants/{id}/orders/{pedidoId}/settlement` | `ALTERAR_STATUS` | o pagamento registrado — §5 |
| `GET` e `PUT /api/v1/merchants/{id}/orders/settings` | `CONFIGURAR_LOJA` | `{ "taxaDeEntrega": 5.00, "aceitaEntrega": true, "aceitaRetirada": true }` |

O gateway já roteia `/api/v1/merchants/{merchantId}/orders/**` para o `order`.

## 4. Estados

O recorte do `docs/dominio/pedido.md` §2. **Fora desta entrega:** `PAGO` (marco 8),
`AGUARDANDO_ESTOQUE` (marco 10), `NAO_ENTREGUE` e os subestados de `EM_PREPARO`.

```
RECEBIDO ─► CONFIRMADO ─► EM_PREPARO ─► PRONTO ─┬─► SAIU_PARA_ENTREGA ─► ENTREGUE   (ENTREGA)
   │             │             │                └─► AGUARDANDO_CLIENTE ─► RETIRADO  (RETIRADA)
   └─────────────┴─────────────┴──► CANCELADO (motivo obrigatório)
```

| De | Para | Condição |
| --- | --- | --- |
| `RECEBIDO` | `CONFIRMADO` | a loja aceita |
| `CONFIRMADO` | `EM_PREPARO` | — |
| `EM_PREPARO` | `PRONTO` | — |
| `PRONTO` | `SAIU_PARA_ENTREGA` | só `ENTREGA`; **entregador informado** |
| `PRONTO` | `AGUARDANDO_CLIENTE` | só `RETIRADA` |
| `SAIU_PARA_ENTREGA` | `ENTREGUE` | **pagamento registrado** (R2 do planejamento) |
| `AGUARDANDO_CLIENTE` | `RETIRADO` | **pagamento registrado** |
| `RECEBIDO`, `CONFIRMADO`, `EM_PREPARO` | `CANCELADO` | `motivo` não vazio |

Qualquer outra transição: **409**, com `detail` dizendo de onde para onde não pode.
`ENTREGUE`, `RETIRADO` e `CANCELADO` são finais.

## 5. O pagamento registrado

`PUT …/orders/{pedidoId}/settlement`, a partir de `PRONTO`:

```json
{ "formaEfetiva": "DINHEIRO", "valorRecebido": 110.00, "pixConferido": null }
```

| Forma | Regra |
| --- | --- |
| `DINHEIRO` | `valorRecebido` ≥ total; o servidor calcula `trocoDevolvido` |
| `CARTAO` | só a forma |
| `PIX` | `pixConferido: true` — o operador conferiu no extrato. **Comprovante na tela não vale** (ADR-004 do planejamento) |

A forma efetiva pode ser diferente da declarada no pedido (o cliente disse dinheiro e pagou no
cartão); as duas ficam guardadas.

## 6. O que fica de fora, de propósito

Cupom e desconto, pedido mínimo, área por bairro, opções do produto no pedido (o item leva o
produto, sem os grupos), avisos automáticos pelo WhatsApp (o painel monta o link `wa.me`),
eventos para outros serviços, e o extrato do expediente (marco 6).
