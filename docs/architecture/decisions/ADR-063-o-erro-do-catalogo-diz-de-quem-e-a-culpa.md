# ADR-063 — O erro do catálogo diz de quem é a culpa

- **Status:** aceita
- **Data:** 11/10/2026
- **Rodada:** E-3b
- **Relacionada:** ADR-052 (`@Version` e o 409), ADR-053 (o contrato declara), ADR-055
  (corpo de erro), ADR-056 (`ProdutoNaoEncontrado`), a tabela C do `catalogo.md` §9, e a
  linha do `CLAUDE.md` escrita na W-E

## Contexto

**O `catalog` tem uma exceção só para tudo, e ela virou 400 para tudo.** O censo da E-3b
mediu **30** pontos de `new RegraDoCatalogoViolada`, e a leitura mostrou que são **duas
espécies com um nome só**:

- **onze são regra** (medido na E-3b; o décimo segundo, a opção que não existe, virou 404 — §4) — C1, C2, C3, C4, C5, texto obrigatório, o `SEM_CONTROLE`
  que não acaba, o rascunho que não se inativa. **O comerciante pode causá-las**, e várias
  carregam o dado que o ajuda: o nome do grupo, o do produto, o valor;
- **dezoito são integridade** — *"produto sem id"*, *"grupo nulo"*,
  *"disponibilidade nula"*, *"opção sem id"*. **Só um defeito nosso as produz.**

**E as dezoito saem 400.** Isto é a figura que o `CLAUDE.md` recusa por escrito desde a
W-E — *"nunca tratador de `IllegalArgumentException`, que faria de todo defeito um erro do
cliente"* —, e o `catalog` a tem **desde a G-E**, sob outro nome de exceção. A regra foi
escrita olhando o `merchant`, e o vizinho não foi conferido.

**O javadoc da própria exceção previa o conserto**, e a condição venceu:

> *"Uma exceção só para toda a rodada, de propósito: **nesta altura o catálogo não tem
> rota**, e quem traduz regra violada em código HTTP é a camada que ainda não existe."*

O tradutor existe desde a G-E. **A frase é um gatilho em prosa, e por isso nunca apareceu
no razão** — que só enxerga a fórmula.

## Decisão

> **A exceção passa a carregar um motivo, e o tratador decide a frase e o código a partir
> dele. Regra do comerciante é 400; defeito nosso é 500.**

### 1. O motivo mora no domínio, o código HTTP mora na borda

`RegraDoCatalogoViolada` ganha um `enum Motivo` e os **dados** daquele motivo. **Ela não
sabe de HTTP** — nenhum `status()` no enum, nenhum import de `HttpStatus` no domínio. Quem
mapeia motivo em código é o `TratadorDeErros`, num `switch` exaustivo.

**Essa fronteira é a mesma do trigésimo segundo defeito**, do outro lado: lá a borda
estava cobrando regra de domínio; aqui o domínio não vai cobrar código HTTP.

### 2. `INTEGRIDADE` é 500, e não leva a mensagem técnica à tela

Os dezoito pontos de nulo recebem um motivo só, `INTEGRIDADE`, e o tratador
responde **500 com frase genérica** — *"não foi possível completar a operação"* ou o que a
§3 do pacote decidir —, **registrando a mensagem técnica no log**.

**A mensagem não vai ao corpo**, e isso não é zelo: *"produto sem id"* diz a quem lê como o
nosso modelo se chama por dentro.

### 3. As recusas de regra levam os dados, porque sem eles a frase não serve

A mensagem de C5 hoje é *"grupo obrigatório sem opções suficientes impede a publicação:
**Tamanho, Borda**"*. Um motivo que fosse só um código **perderia os nomes**, que são a
parte útil.

> **O motivo carrega os dados, e o tratador compõe.** *"O grupo «Tamanho» precisa de ao
> menos uma opção para o produto ir ao cardápio."* Sai o `C5`; fica o nome.

**A mensagem técnica continua existindo**, na exceção, para o log e para os testes — que,
segundo o javadoc, afirmam *"o tipo e um fragmento da mensagem"*.

### 4. A opção que não existe passa a ser 404

`"opção não encontrada neste produto"` (`Produto`:346) sai hoje como **400**, e a rota da
W-D — a marcação por opção — **o alcança**. O `catalog` já tem `ProdutoNaoEncontrado` →
404 (ADR-056); **a opção que não existe é a mesma figura, um nível abaixo**.

Nasce `OpcaoNaoEncontrada`, e a rota declara 404 **com provocação** (ADR-053).

**E a tela precisa de uma linha:** o `cliente.ts` do front tem mensagem por código, e um
404 sem entrada cai no genérico. **Uma entrada, e nada mais no front.**

### 5. O que esta ADR **não** faz

**Não separa a classe em duas.** `DefeitoDoCatalogo` com nome honesto seria melhor de ler
— e custaria trocar o tipo em dezoito pontos e corrigir todo teste que afirma o tipo.
**O comportamento fica certo com o motivo**, e o nome fica como dívida, com gatilho.

**Não toca nos outros dois tratadores do `catalog`** — `ProdutoNaoEncontrado` e
`LojaSemExpediente` continuam repassando `getMessage()`, e continuam no razão.

## Consequências

**O `catalog` deixa de dizer ao comerciante que ele errou quando quem errou fomos nós.**
Isso muda o código de resposta de dezoito caminhos — **todos hoje inalcançáveis
pela borda**, o que é precisamente o argumento: se algum deles aparecer num 500 em
produção, é defeito, e o log vai dizer qual.

**⚠ E é por isso que a §3 manda listar quais dos trinta são alcançáveis por uma rota de
hoje.** Se algum ponto de `INTEGRIDADE` for alcançável por um corpo malformado, **ele não é
integridade** — é forma, e o lugar dele é o Bean Validation do DTO. *Trigésimo segundo
defeito, do lado de cá.*

**O contrato de uma rota existente muda.** A marcação por opção passa a declarar 404, e o
`ContratoDeErrosIT` exige provocação — que é o teste que pegaria a omissão.

## Alternativas consideradas

**Separar em duas exceções, com nomes honestos.** **Recusada pelo custo** — dezoito trocas
de tipo e os testes que afirmam o tipo —, numa rodada que já traz uma camada de
persistência nova. *Está no gatilho.*

**Deixar tudo em 400, com um `INTEGRIDADE` genérico.** Tiraria o vazamento da mensagem e
manteria a mentira do código. **Recusada:** o `CLAUDE.md` já decidiu o contrário, e manter
a contradição escrita é pior do que nunca tê-la escrito.

**Pôr o status no `enum Motivo`.** Mais curto de ler. **Recusada** pela §1: o domínio
passaria a saber de HTTP, e a próxima camada que consumisse o catálogo — uma fila, um
gRPC — herdaria códigos de um protocolo que não é o dela.

## Gatilho escrito

**O dia em que alguém for ler o `catalog` e se confundir com uma `RegraDoCatalogoViolada`
que é 500.** Aí a classe se parte em duas, com nomes honestos, e os dezoito pontos trocam
de tipo.

**E a primeira rota de escrita do `catalog`** — a E-3c. Ela vai gerar corpos malformados de
verdade, e aí se mede quais dos trinta pontos **passaram a ser alcançáveis**. Um ponto de
`INTEGRIDADE` alcançável é um `@NotNull` faltando no DTO, não um motivo novo.
