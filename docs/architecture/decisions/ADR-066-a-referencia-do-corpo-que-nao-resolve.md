# ADR-066 — A referência do corpo que não resolve é pedido inválido, não recurso ausente

- **Status:** aceita
- **Data:** 11/10/2026
- **Rodada:** E-3c
- **Relacionada:** ADR-056 (`ProdutoNaoEncontrado` é 404), ADR-063 (o erro diz de quem é a
  culpa), ADR-064 (a categoria ganha persistência), ADR-065 (o produto nasce por rota), a
  M7 (de outra loja é idêntico a "não existe"), e o javadoc do `Produto`

## Contexto

**O javadoc do `Produto` escreveu esta regra e disse de quem ela é**, e a frase está em
`main` desde a G-B:

> *"Se a categoria existe, e se ela é da mesma loja. **Categoria é outra raiz; agregado não
> valida agregado.** A regra é do caso de uso, e ela nasce com a rota que cria produto."*

**A rota é esta.** E até a E-3b a regra era impossível de escrever: não havia como perguntar
se uma categoria existe, porque não havia coleção. A ADR-064 deu persistência; **esta ADR
paga a dívida que o javadoc deixou escrita.**

**O que falta decidir não é a regra — é o código de resposta.** O repositório tem dois
precedentes de 404, e os dois são de identificador que vem **no caminho**:
`ProdutoNaoEncontrado` (ADR-056) e `OpcaoNaoEncontrada` (ADR-063 §4, na rota
`.../produtos/{produtoId}/opcoes/{opcaoId}/...`). **A `categoriaId` vem no corpo**, e é a
primeira vez que isso acontece no `catalog`.

## Decisão

> **404 é de identificador que está na URL. Referência que vem no corpo e não resolve é
> pedido inválido: 400, com motivo e com a frase que nomeia o campo.**

### 1. A regra, inteira, e ela é uma só

O caso de uso que cria produto **pergunta ao `CategoriaRepositorio`** se existe categoria com
aquele id **naquele estabelecimento**. Não existe, ou existe em outra loja: **a mesma
recusa, com a frase idêntica.**

**Isso é a M7, do outro lado da operação.** A M7 diz que um recurso de outra loja responde
igual a um que não existe, para não contar quem é cliente de quem. Aqui o alvo é o mesmo:
**uma resposta que distinguisse "essa seção é de outra loja" de "essa seção não existe"
contaria, a quem tentasse ids no escuro, quais existem no sistema.**

### 2. Por que 400, e não 404

**O recurso que a URL endereça existe.** `POST /merchants/{id}/catalog/produtos` fala com o
cardápio de uma loja que existe, sob um token que prova pertencimento. **O 404 diria que o
cardápio não está lá**, e está.

**O que está errado é o pedido** — um campo dele aponta para coisa que não há. Quem recebe
404 não sabe o que corrigir; quem recebe a frase da ADR-063 com o nome do campo sabe:
*"escolha uma seção do cardápio desta loja."*

**E a máquina já existe.** A ADR-063 fez a exceção carregar motivo e dados, e o tratador
compor a frase a partir deles. **Esta regra não pede mecanismo novo: pede um motivo novo**,
e o `switch` exaustivo sem `default` garante que ele não caia no genérico — foi exatamente
para isto que ele foi escrito sem `default`.

### 3. O motivo novo é o primeiro lançado fora do agregado, e isso vai escrito

Os pontos que a E-3b censeou são todos de dentro de raízes — `Produto`, `Categoria`,
`GrupoDeOpcoes`, `Opcao`, `Disponibilidade`, `Textos`. **Este nasce no caso de uso**, e é
coerente com a frase do javadoc: *agregado não valida agregado*.

**Não é exceção de aplicação nova.** Reusa a `RegraDoCatalogoViolada` com um motivo, e a
razão é que o leitor da resposta não deve ver diferença: para quem cadastra, *"preço tem de
ser positivo"* e *"essa seção não existe aqui"* são a mesma espécie de recado — **regra do
catálogo violada, com a frase que diz o que fazer.**

⚠ **O nome do motivo segue a forma dos que já estão no enum.** A §2.2 do pacote manda colar
o enum inteiro antes de acrescentar; se a forma for outra, **siga a forma e diga qual**.
*Prescrever o nome sem ler o enum é o nono defeito.*

### 4. Ninguém conserta a referência órfã que já existe

**Os produtos semeados e os que o `ProdutoResponse` devolve hoje carregam `categoriaId`** e
nunca foram verificados contra coleção nenhuma — não havia coleção. Esta ADR **não** varre o
banco nem valida na leitura.

**A regra vale na escrita, daqui para a frente**, e é o lugar certo: validar na leitura faria
o cardápio de uma loja ficar ilegível por causa de um dado velho, e **transformaria um defeito
de ontem em indisponibilidade de hoje**.

⚠ **E a semeadura passa a precisar de categoria gravada** — a E-3b já a fez criar uma. Se o
semeador criar produto por caminho que não passe pelo caso de uso, **ele não é alcançado por
esta regra**: diga por qual caminho ele cria, e não o reescreva nesta rodada.

## Consequências

**O `categoriaId` do contrato passa a ter referente garantido na escrita.** O que a ADR-064
deixou como *"nada ainda força a correspondência"* passa a ser forçado — pela rota, não pelo
documento.

**A `CategoriaRepositorio` ganha o terceiro método, e o corte da ADR-064 expira pelo próprio
motivo que o escreveu.** Lá estava: *"porta com método que ninguém chama é superfície"*.
**Agora há chamador.**

**A tela vai precisar listar seções antes de criar produto**, e a rota de listar existe desde
a E-3b. **É a primeira vez neste projeto que uma tela precisa de duas rotas do mesmo serviço
para montar um formulário** — a W-F vai medir isso.

## Alternativas consideradas

**404 com `CategoriaNaoEncontrada`.** Seguiria a figura *"a mesma coisa um nível abaixo"* que
a ADR-063 §4 usou para a opção. **Recusada:** naquele caso a opção vinha **no caminho**, e é
isso que faz um 404. Mistura *"o que você pediu não está aqui"* com *"o que você mandou está
errado"*, e deixa a tela sem saber que campo destacar.

**422 `Unprocessable Entity`.** Mais preciso em teoria. **Recusada:** o repositório não usa
422 em lugar nenhum, e introduzir um terceiro código para erro de pedido faria o front ter de
tratar 400 e 422 com o mesmo mapa. *Está no gatilho.*

**Bean Validation no DTO.** **Recusada, e é o trigésimo segundo defeito nomeado:** a borda
valida **forma** — que o campo veio, que não é branco, que é um UUID. **Existência é regra, e
regra que atravessa duas raízes é do caso de uso.** A borda não tem como perguntar ao banco, e
se tivesse, não deveria.

**Validar também na leitura, e esconder produto com categoria órfã.** **Recusada pela §4:**
faria dado velho virar indisponibilidade nova.

## Gatilho escrito

**A primeira outra referência de corpo que atravesse raízes** — o `entregadorId` de um pedido,
o `estabelecimentoId` de uma conversa. Aí esta ADR deixa de ser sobre categoria e passa a ser
a regra da casa, e o nome dela muda.

**O dia em que o front precisar distinguir um campo inválido de outro na mesma resposta.** Hoje
a frase diz qual campo em português; se a tela precisar do nome do campo em dado, **o corpo de
erro ganha um `campo`** — e aí a ADR-055 é que muda.

**E o dia em que alguém apagar uma categoria que tem produtos.** Não há rota de apagar
categoria, e quando houver, **esta regra fica de pé do lado errado do tempo**: o produto
passaria a apontar para coisa que existiu. *Está no `dividas.md`.*
