# ADR-061 — A loja escolhida mora na URL

- **Status:** aceita
- **Data:** 10/10/2026
- **Rodada:** W-E
- **Relacionada:** ADR-025 (fuso no cadastro), ADR-053 (o contrato declara),
  ADR-055 (corpo de erro), ADR-060 (a rota de criação),
  `docs/front/premissas-do-front.md`, e o gatilho escrito no `MenuDoPainel.tsx:34`

> **Nota sobre o título.** A primeira versão desta ADR se chamava *"a URL do painel passa
> a importar"* — e com isso eu citei o gatilho do `MenuDoPainel` usando a **minha**
> palavra, *"importar"*, no lugar da dele. O título mudou para que a paráfrase não volte a
> se passar por citação.

## Contexto

O painel navega **por estado** desde a W-B. A decisão estava certa quando havia uma seção
só, e ela veio com gatilho escrito, no próprio componente (`MenuDoPainel.tsx:34`):

> *"**Gatilho escrito:** a segunda seção com tela. Aí a URL passa a precisar dizer onde a
> pessoa está — para recarregar, para voltar e para mandar link a alguém —, e isso é rota,
> não estado."* (`MenuDoPainel.tsx:34-36`, colada inteira na W-E)

**O gatilho venceu na W-C**, e ninguém o leu. **A W-D o agravou**, pondo a terceira tela —
o produto aberto — pelo mesmo precedente, sem reler a linha que o expirava. A R-1 o
nomeou, e ele é um dos quatro casos de *"disparou e não produziu nada"*.

**Esta rodada não tem como adiá-lo**, e não por disciplina: depois do `201` da criação, o
painel precisa **abrir a loja que acabou de nascer**.

## Decisão

> **A loja escolhida é um segmento da URL, sob o `/painel` que já existe.**

```
/entrar                                              a porta (W-A)
/cadastrar                                           o cadastro de pessoa (W-A)
/painel                                              o seletor de loja
/painel/lojas/nova                                   o cadastro de loja  ← esta rodada
/painel/lojas/:estabelecimentoId                     a loja: vai à primeira seção que as permissões abrem
/painel/lojas/:estabelecimentoId/produtos            o cardápio
/painel/lojas/:estabelecimentoId/produtos/:produtoId o produto aberto
/painel/lojas/:estabelecimentoId/disponibilidade     a seção "Disponibilidade" (a mesma lista, com marcação)
/painel/lojas/:estabelecimentoId/disponibilidade/:produtoId  o produto aberto, com marcação
```

**As duas linhas de `disponibilidade` não estavam no desenho do pacote**, e entraram na W-E
pela própria decisão — *"cada tela do painel tem endereço"*: a seção "Disponibilidade"
existe desde a W-C, só para quem tem `ALTERAR_PRODUTO`, e o produto aberto a partir dela
marca opções. Sem endereço próprio, ou ela sumia, ou a tela do produto passaria a decidir
pela permissão se marca — o que a W-D não fazia.

### 1. O `/painel` fica, e a versão anterior desta ADR estava errada sobre ele

A primeira versão recusava o prefixo como *"segmento morto em toda URL"* — e ao mesmo
tempo afirmava que os caminhos propostos eram *"o que o repositório já usa"*. **As duas
coisas não podiam ser verdade**, e nenhuma era: `/criar-conta` não existe (é
`/cadastrar`), e `/painel` existe e é onde o painel mora.

**E o prefixo não é morto:** ele separa a área autenticada das duas páginas públicas. O
`ExigeSessao` envolve o que está sob ele, o `/` redireciona para ele e o `*` também —
três coisas que continuam funcionando sem tocar em nada.

### 2. Por que a loja na URL, e não só a tela

- **o `201` precisa de destino.** A resposta da criação é o `LojaDoUsuario` da loja nova.
  Sem endereço, "ir para a loja criada" é mudar estado — e um recarregamento o perde;
- **uma pessoa pode ter várias lojas** (ADR-060 §6). Com a loja em estado, a escolha volta
  ao padrão a cada recarga, e **a pessoa opera na loja errada** — que é o dano que o
  `ConsultarMinhasLojasService` já evita ordenando por nome, por comentário escrito;
- **o endereço é compartilhável e depurável.**

### 3. O `/painel` resolve por quantas lojas a pessoa tem

| lojas | o que acontece |
|---|---|
| **zero** | o `/painel` mostra o convite que já existe, **com um link para o cadastro** |
| **uma** | o `/painel` redireciona para ela |
| **mais de uma** | o `/painel` mostra o seletor |

**Zero lojas não leva ao cadastro por redirecionamento, e a razão importa:** *zero lojas*
não significa *"acabou de criar a conta"*. Significa também **convidada e ainda não
aceitou**, **suspensa**, **removida da última loja**. O texto que o `SeletorDeLoja` já
mostra — *"Você ainda não faz parte de nenhuma loja"* — é verdade nos quatro casos;
*"cadastre a sua loja"* só é no primeiro.

> **A tela existente sabia algo que o desenho da primeira versão desta ADR não sabia.** Ela
> fica, e ganha um link.

### 4. O painel continua sem derivar regra de domínio

O cadastro **não reimplementa invariante nenhuma**. Ele marca campo obrigatório — que é
forma, e o `@NotBlank` do DTO diz o mesmo — e **mostra a recusa que a borda devolver**,
pelo `detail` do `400`, como a ADR-055 manda.

**O botão de enviar não é desabilitado pela M12.** Decidir no front que *"modalidade sem
forma de pagar é inválido"* seria o front derivando regra de domínio — o que a premissa do
front recusa, e o que a W-D já consertou uma vez no `vendavel`.

**A exceção é o fuso, e ela não é exceção:** o `merchant` declara as zonas aceitas no
schema do campo (`@Schema(allowableValues)`, soldado a `FusoHorario.identificadoresAceitos()`
pelo `AllowableValuesDoFusoTest`), e o tipo gerado vira a união das 16. **O front lê esse
tipo:** a lista do seletor sai das chaves de um `Record<Fuso, string>` de rótulos, e o
`Record` exige do typecheck **exatamente** as chaves da união — uma zona a mais ou a menos
no contrato derruba o build. É o precedente do `ESTADOS`/`ROTULO` da `disponibilidade.ts`.

**Por que não o array exportado pelo gerador:** o `openapi-typescript` tem
`--enum-values`, que exportaria os valores prontos, em tempo de execução. Medido na W-E: ele
gera, para os valores do mapa `metodosPorModalidade`, um array com tipo errado, e o arquivo
gerado deixa de compilar.

**As modalidades não estão no contrato.** O mapa é declarado com chave `string`, e só as
formas de pagar vêm como enum. O formulário nomeia `ENTREGA` e `RETIRADA` numa constante
— a única lista dele que não tem o contrato atrás. Está no `dividas.md`.

## Consequências

**O gatilho do `MenuDoPainel` está pago**, três rodadas depois de vencer. A linha sai de
*"disparou e não produziu nada"* no `dividas.md` com o histórico inteiro.

**As três telas existentes ganham endereço, e nenhuma muda o que mostra** — com **duas
exceções escritas**, as duas no `SeletorDeLoja`: o link para `/painel/lojas/nova`, que é
decisão desta rodada (§3); e, com mais de uma loja e nenhuma escolhida, uma opção vazia
*"Escolha uma loja"* — sem ela a primeira apareceria marcada sem estar escolhida, e
escolhê-la não dispararia mudança nenhuma. A segunda não estava no desenho; é consequência
do `/painel` mostrar o seletor em vez de escolher sozinho.

**O `/painel` com uma loja passa a redirecionar.** Hoje ele escolhe sozinho em estado, e
há teste afirmando isso. O comportamento visível é o mesmo; o teste muda de forma.

**Recarregar deixa de perder a loja escolhida.** Não havia teste cobrindo isso, porque não
havia URL a cobrir.

**O `/painel/lojas/:id` aceita id de loja sem vínculo.** A tela pede os dados, o
`merchant` responde 403 (M7, idêntico a "não existe"), e a tela mostra recusa genérica — a
ADR-055 já decide isso. **O front não verifica vínculo**, porque verificar seria derivar a
invariante 9 no navegador.

## Alternativas consideradas

**Continuar por estado.** **Recusada**, e esta é a terceira oportunidade: o gatilho venceu
na W-C, foi agravado na W-D e nomeado na R-1. Adiar outra vez faria de "gatilho escrito"
decoração.

**Tirar o `/painel` e pôr `/lojas/:id` na raiz.** Era o desenho da primeira versão desta
ADR. **Recusada** pela §1: o prefixo tem função, e tirá-lo mexeria no `/`, no `*` e no
`ExigeSessao` sem ganhar nada.

**Zero lojas redirecionando para o cadastro.** **Recusada** pela §3: a lista vazia tem
quatro causas e o redirecionamento serve a uma.

**A loja escolhida num armazenamento do navegador.** Resolve o recarregamento e **não**
resolve o destino do `201` nem o endereço compartilhável, e põe estado de navegação onde
nenhum teste olha. **Recusada.**

**O cadastro como modal dentro do painel.** Seria a quarta tela sem endereço, e o `201`
sem destino. **Recusada** pela §2.

## Gatilho escrito

**A primeira tela do painel que não pertença a uma loja** — relatório que cruze duas
lojas, conta da pessoa, preferências. Aí `/painel/lojas/:id` deixa de ser o prefixo de
tudo que está sob o painel, e a árvore ganha um nível.

**O dia em que o seletor tiver mais de uma dúzia de lojas.** Hoje a lista vem inteira numa
chamada, ordenada por nome. Quando não bastar, é busca no servidor — e aí o
`GET /api/v1/me/estabelecimentos` ganha paginação, que ele não tem.
