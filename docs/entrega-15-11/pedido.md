# O pedido da entrega de 15/11 — rotas, campos e estados

**Depende da ADR-077** (proposta). Este documento é o desenho que a etapa 3 implementa, e o
que o código cumpre hoje. O índice das etapas está em [`README.md`](README.md). O
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
order                     ──► merchant  GET /api/v1/storefront/{id}/status (a loja está aberta? — §2.5)
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

Rascunho e inativo não aparecem; produto publicado em **seção desativada** também não (é o
"efeito de montagem" da `Categoria`).

**Loja inexistente: cardápio vazio (200), e não 404** — mudança de 27/10 em relação ao desenho
original. O `catalog` não conhece lojas, só produtos de uma loja, e o `merchant` não tem rota
pública que diga se uma loja existe. A tela mostra "esta loja ainda não tem cardápio". Gatilho
para o 404: o `merchant` ganhar essa rota. **Disparou em 01/11/2026** (§2.5), e a vitrine já
avisa que a loja não recebe pedidos; o `catalog` continua devolvendo o cardápio vazio, e a
troca dele pelo 404 fica para depois da demonstração.

### 2.2 `GET /api/v1/storefront/{merchantId}/orders/settings` → `order`

```json
{ "taxaDeEntrega": 5.00, "aceitaEntrega": true, "aceitaRetirada": true,
  "aberta": false, "proximaAbertura": "2026-11-01T21:00:00Z" }
```

Loja que nunca configurou: taxa **0,00**, as duas modalidades aceitas. `aberta` e
`proximaAbertura` vêm do `merchant` (§2.5): `aberta` nulo quando ele não respondeu — a vitrine
abre assim mesmo, e o pedido decide.

### 2.3 `POST /api/v1/storefront/{merchantId}/orders` → `order`

**Loja fechada: 409**, com a frase dizendo quando abre (*"a loja está fechada agora e abre hoje às
18:00; volte nesse horário para fazer o pedido"*). Sem resposta do `merchant`: **503**, e o
pedido não nasce — falha fechada.

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
não aceita. Loja inexistente cai em "produto fora do cardápio" (400), pelo mesmo motivo do §2.1.
`catalog` fora do ar: **503**, e o pedido **não** é criado.

### 2.4 `GET /api/v1/storefront/orders/{codigoDeAcompanhamento}` → `order`

O que o cliente acompanha: número, status, itens, valores, modalidade. **Sem telefone nem
endereço** na resposta (ADR-013). O código é um UUID aleatório; sem ele, não há como listar
pedidos de ninguém. Inexistente: **404**.

### 2.5 `GET /api/v1/storefront/{merchantId}/status` → `merchant` (não passa pelo gateway)

Decisão de 01/11/2026: **loja fora do horário não recebe pedido.** Quem sabe o horário é o
agregado da loja, no `merchant` (horário, pausa e fuso — `Estabelecimento.estaAberta`). Esta rota
pública devolve a resposta dele:

```json
{ "nome": "Pizzaria da Marli", "aberta": false, "proximaAbertura": "2026-11-01T21:00:00Z" }
```

`proximaAbertura` nula com a loja aberta, **ou quando ela ainda não definiu o horário** — e loja
sem horário fica fechada. É o passo 3 dos primeiros passos do painel. Identificador que não é de
loja: **404** (a vitrine é o link que a loja divulga; ver `LojaNaoEncontrada`).

Quem chama é o `order`, por dentro: na vitrine (§2.2) e ao receber o pedido (§2.3). O gateway não
a roteia — o cliente recebe a situação junto com a taxa, numa chamada só.

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
| `PUT /api/v1/merchants/{id}/orders/{pedidoId}/pix-check` | `ALTERAR_STATUS` | a loja confirma no extrato o Pix que o entregador informou; vale em qualquer estado, inclusive entregue. **400** se não há Pix a conferir (02/11/2026) |
| `GET` e `PUT /api/v1/merchants/{id}/orders/settings` | `CONFIGURAR_LOJA` | `{ "taxaDeEntrega": 5.00, "aceitaEntrega": true, "aceitaRetirada": true }` |

O gateway já roteia `/api/v1/merchants/{merchantId}/orders/**` para o `order`.

### 3.1 O entregador da equipe (decisão de 31/10/2026)

O entregador é funcionário da loja e **entra com a conta dele**: a loja o convida pela Equipe com a
permissão `VER_ENTREGA`. Na primeira vez ele informa o nome com que a loja vai reconhecê-lo — a
equipe do `merchant` não guarda nome, e esta entrega não mexe nela.

| Rota | Permissão | O que faz |
| --- | --- | --- |
| `GET /api/v1/merchants/{id}/orders/couriers` | `VER_PEDIDO` | os entregadores que já se apresentaram |
| `GET` e `PUT /api/v1/merchants/{id}/orders/couriers/me` | `VER_ENTREGA` | eu, como entregador (`204` se ainda não me apresentei); `{ "nome": "Carlos Moto" }` |
| `GET /api/v1/merchants/{id}/orders/deliveries?dia=` | `VER_ENTREGA` | **as minhas** entregas do dia — o `usuarioId` sai do token, nunca da URL |
| `PUT /api/v1/merchants/{id}/orders/deliveries/{pedidoId}/status` | `VER_ENTREGA` | só `SAIU_PARA_ENTREGA` e `ENTREGUE` |
| `PUT /api/v1/merchants/{id}/orders/deliveries/{pedidoId}/settlement` | `VER_ENTREGA` | `DINHEIRO`, `CARTAO` ou `PIX`. O Pix fica **a conferir** pela loja (02/11/2026) |

`PUT …/{pedidoId}/courier` aceita `{ "usuarioId": "…" }` (alguém da equipe; o nome vem do cadastro)
ou `{ "nome": "…" }` (o entregador avulso, sem conta). Entrega que não está com quem pede é **404**.

### 3.2 O fechamento do dia e o repasse

**Repasse ao entregador = a taxa de entrega de cada pedido que ele entregou** (decisão de
31/10/2026). O dinheiro que ele recebeu na porta é da loja, e o acerto é a diferença:
`acerto = dinheiro recebido − repasse` (positivo: ele devolve à loja; negativo: a loja paga).

**Desde 01/11/2026 o caixa do dia é fechado e gravado no `order`** (`FechamentoDoDia`, migration
V3). O servidor apura tudo a partir dos pedidos do dia — o navegador manda só o dia e, se quiser,
o dinheiro contado e uma observação:

| Rota | Permissão | O que faz |
| --- | --- | --- |
| `POST /api/v1/merchants/{id}/orders/closings` | `GERENCIAR_JORNADA` | `{ "dia": "2026-11-05", "dinheiroContado": 320.00, "observacao": "…" }` → **201** com o fechamento |
| `GET /api/v1/merchants/{id}/orders/closings` | `VER_VENDAS` | o histórico, do mais recente ao mais antigo. Com `?de=AAAA-MM-DD&ate=AAAA-MM-DD`, os dias fechados do período, de até um ano — **400** se invertido ou maior (02/11/2026) |
| `GET /api/v1/merchants/{id}/orders/closings/{dia}` | `VER_VENDAS` | o fechamento de um dia; **404** se ainda não fechou |

- **Não fecha com pedido em andamento** (409, *"há 2 pedidos do dia em andamento; conclua ou
  cancele antes de fechar o caixa"*), **nem com Pix informado pelo entregador ainda a conferir**
  (409, desde 02/11/2026), nem duas vezes o mesmo dia (409), nem dia futuro (400).
- **A conferência é opcional:** `diferencaDeCaixa = dinheiro contado − vendas em dinheiro`, só
  registrada, sem bloquear.
- **Depois de fechado, o dia não muda:** os pedidos daquele dia não avançam, não trocam de
  entregador e não registram pagamento (409), e a vitrine não aceita pedido novo naquele dia.
- A tela exporta o fechamento em CSV (separador `;`, vírgula decimal).

## 4. Estados

O recorte do `docs/dominio/pedido.md` §2. **Fora desta entrega:** `PAGO` (só com pagamento online, fora da proposta — ADR-023),
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

**O Pix informado pelo entregador** (02/11/2026): pelo app de entregas, o entregador registra `PIX`
sem conferência; o pedido fica com `pixConferido: false`, pode ser entregue, e aparece em Pedidos
com o botão *"Conferi o Pix no extrato"* (`PUT …/pix-check`). O sino avisa, e o caixa do dia não
fecha enquanto houver Pix a conferir.

## 6. Como testar

- **Testes de unidade** (sem Docker): `.\gradlew.bat :services:order-service:test` no
  PowerShell, em `backend`. O domínio (`PedidoTest`) e os casos de uso com dublês
  (`FazerPedidoServiceTest`, `PedidosDaLojaServiceTest`, `EntregadoresServiceTest`).
- **Ponta a ponta pela API:** `docs/entrega-15-11/testes-pedido.http` — do cardápio público ao
  pedido entregue, as recusas, e o entregador na conta dele —, com a pilha de pé por
  `./scripts/subir-local.sh --com-pedidos` ou, no Docker Desktop, pela seção 0.1 de
  `docs/como-subir-local.md`.
- **Pelas telas:** o roteiro de `docs/entrega-15-11/README.md`.
- **Ainda não há** teste de integração do `order` com banco de verdade (Testcontainers), nem
  contrato em `contracts/openapi/order-service.json`. Gatilho: depois da demonstração, antes de
  propor o merge ao grupo.

## 7. O que fica de fora, de propósito

Cupom e desconto, pedido mínimo, área por bairro, opções do produto no pedido (o item leva o
produto, sem os grupos), avisos automáticos pelo WhatsApp (o painel monta o link `wa.me`),
eventos para outros serviços, e o extrato do expediente (marco 6).
