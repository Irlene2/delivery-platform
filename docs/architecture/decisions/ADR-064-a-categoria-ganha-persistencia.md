# ADR-064 — A categoria ganha persistência, e só criar e listar

- **Status:** aceita
- **Data:** 11/10/2026
- **Rodada:** E-3b
- **Relacionada:** C13 (`catalogo.md` §9), a §1 e a §7 do `catalogo.md`, ADR-017 (catálogo
  documental), ADR-027 (valor novo em enum é compatível), ADR-052 (`@Version`), ADR-053
  (o contrato declara), ADR-059 (a semeadura), ADR-063 (o erro diz de quem é a culpa), a
  invariante 7 do `CLAUDE.md` (quem publica precisa de outbox)

## Contexto

**A `Categoria` existe em Java e em lugar nenhum mais.** Medido na E-3a e confirmado na
E-3b: há classe de domínio com regras, há linha no `catalogo.md` §1 dizendo que é **raiz**,
há evento declarado — `CategoriasReordenadasV1`, com o `conversation` como consumidor. Em
`application/port/out/` há **só** a porta de produto; em
`infrastructure/persistence/` há **só** a peça de produto. **Nenhuma categoria pode ser
gravada ou lida.**

O javadoc dela explica por que nasceu cedo:

> *"um identificador que aponta para um tipo que não existe é a mesma promessa com sintaxe
> de código que este repositório recusou no enum sem produtor."*

**O argumento é bom, e a peça parou no meio.** O `ProdutoResponse` carrega `categoriaId`, e
o contrato devolve um identificador irresolvível. **E não havia gatilho escrito** para a
persistência — o `grep` por gatilho e dívida nas ADRs voltou vazio. O que havia era prosa,
no mesmo javadoc: *"ela nasce nesta rodada — e não na seguinte"*.

**A fixture confessa o estado, por escrito** (`Fixture.java`:20):

> *"A categoria. **Solta de propósito**: `Categoria` não tem repositório nem documento, e
> nada no domínio ou na leitura exige que ela exista gravada."*

E a regra que torna isto urgente está no javadoc do `Produto`: *"se a categoria existe, e
se ela é da mesma loja… **a regra é do caso de uso, e ela nasce com a rota que cria
produto**"*. Essa rota é a E-3c.

## Decisão

> **A categoria ganha porta, documento, coleção e índice — e duas rotas: criar e listar.**

```
POST /api/v1/merchants/{estabelecimentoId}/catalog/categorias    201    CRIAR_PRODUTO
GET  /api/v1/merchants/{estabelecimentoId}/catalog/categorias    200    VER_PRODUTO
```

O gateway já roteia as duas: `Path=/api/v1/merchants/{merchantId}/catalog/**`, medido em
`backend/infra/gateway/src/main/resources/application.yml`:77.

### 1. Só criar e listar, e o corte é medido

`renomear`, `reordenar`, `ativar` e `desativar` ficam no agregado.

**`reordenar` publica `CategoriasReordenadasV1`**, e **o `catalog` não tem outbox** — medido
na E-3a. A invariante 7 do `CLAUDE.md` diz que quem publica precisa de um, e dar outbox ao
`catalog` é a rodada que a G-C3 inteira fez no `merchant`. **Enfiá-la aqui seria esconder
uma camada dentro de outra** — o trigésimo quarto defeito.

Os outros três não publicam nada, e ficam por outro motivo: **sem tela que os peça, seriam
rotas sem chamador.** Entram com a tela, e o gatilho está escrito.

### 2. 201 sem `Location`, e desta vez eu conferi antes

Não há `GET` de categoria por id, e esta rodada não cria um: a tela precisa da lista.
**Prometer `Location` para rota inexistente foi o vigésimo sexto defeito**, na E-1.

### 3. As permissões, e as duas são escolha

| rota | permissão |
|---|---|
| criar | `CRIAR_PRODUTO` |
| listar | `VER_PRODUTO` |

**Nenhum documento nomeia permissão de categoria** — medido. A `PermissaoDoCatalogo` tem
quatro valores, todos com `PRODUTO` no nome. **Criar seção é montar o cardápio**, e é o
mesmo ato comercial de criar item; **quem vê o cardápio vê as seções dele**. Um
`GERENCIAR_CARDAPIO` próprio está recusado abaixo.

### 4. `@Version`, pelo precedente

O `ProdutoDocumento` tem `@Version Long versao` (ADR-052). **A categoria leva também** —
não por cerimônia: a rodada de reordenar vai precisar, e acrescentar depois é migração.

### 5. O índice não está na §7, e passa a estar

A §7 lista dois índices, **e os dois são de produto**:
`(estabelecimentoId, estadoDePublicacao)` e `(estabelecimentoId, categoriaId, ordem)`.

A categoria precisa do dela — `(estabelecimentoId, ordem)`, que é como a lista é servida.
A C13 manda *"toda estrutura e índice via Mongock"*: ele nasce num `changeUnit`, no
padrão do `V001CriaIndicesDoProduto`, **e a §7 ganha a linha**. Índice que existe no código
e não no documento é a assimetria que a C13 foi escrita para impedir.

### 6. O corpo, e o que a aplicação decidiu sem o pacote dizer

**A `ordem` vem no corpo e não tem regra.** O agregado aceita qualquer inteiro — repetido,
negativo — porque o `catalogo.md` §1 não escreve regra sobre ela, e a medição da E-3b não
achou nenhuma no código. **O empate é resolvido pela listagem**: `ordem`, depois `nome`,
depois `id`. Sem desempate, duas seções com a mesma ordem trocariam de lugar entre
recargas. O índice cobre só a primeira chave; o resto se ordena em memória, sobre as
poucas seções de uma loja.

**Nome em branco é forma, e não regra.** `@NotBlank` no DTO, e o
`Textos.exigirPreenchido` do agregado fica como segunda rede. É a fronteira da ADR-063:
nulo e vazio vindos de fora são forma. **O 400 sai sem `detail`**, pelo tratamento padrão
do Boot — nenhum serviço deste repositório trata `MethodArgumentNotValidException`, e a
tela cai na mensagem dela para o 400 (ADR-055).

**A listagem não é paginada**, contra a convenção do `CLAUDE.md` (*"Paginação obrigatória
em toda listagem"*) — e é decisão desta ADR, com o precedente da lista de lojas do
`merchant` e com gatilho. A consulta filtra pela loja; nunca é um `findAll()`.

## Consequências

**O `categoriaId` do contrato passa a ter referente.** Nada ainda **força** a
correspondência — isso é a E-3c.

**O semeador passa a criar a categoria**, e o javadoc *"solta de propósito"* sai junto com
a razão que ele dava. **Não é melhorar o que não é assunto:** a fixture aponta para a raiz
que esta rodada torna gravável.

**O marco 2 não ganha coluna**, e nada neste pacote diz que ganha. Faltam a rota de produto
(E-3c) e a tela (W-F).

## Alternativas consideradas

**Um `GERENCIAR_CARDAPIO` novo.** Nomearia o ato com precisão. **Recusada por ora:** custa
dois enums, o conjunto do fundador, o contrato e os tipos do front — e **ninguém pediu a
separação**. *Está no gatilho.*

**A categoria como campo do produto.** **Recusada:** o documento já decidiu o contrário com
razão escrita — *"reordenar o cardápio move dezenas de produtos entre categorias sem tocar
em nenhum deles"*.

**Fazer tudo: persistência, as cinco operações e o outbox.** **Recusada** pelo tamanho.

**Adiar, e aceitar `categoriaId` solto na E-3c.** **Recusada:** entregaria a primeira rota
de criação do `catalog` violando a regra que o próprio domínio escreveu para ela.

## Gatilho escrito

**A primeira tela que renomeie, reordene, ative ou desative uma seção.** Aí as três sem
evento viram rotas — e **`reordenar` arrasta o outbox do `catalog` junto**.

**O dia em que alguém quiser separar quem cria produto de quem mexe nas seções.**

**E o dia em que uma loja tiver mais seções do que cabem numa resposta.** A listagem nasce
sem paginação, como a de lojas do `merchant`.
