# ADR-073 — O horário ganha rota, sob uma permissão que não existia

- **Status:** aceita
- **Data:** 17/10/2026
- **Rodada:** E-4
- **Relacionada:** ADR-011 (autorização comercial), ADR-027 (compatibilidade de eventos), ADR-046
  (quem observa a abertura do expediente), ADR-056 (recurso de outra loja), ADR-059 (a lista do
  que a API não escreve), ADR-060 (a primeira rota de escrita), ADR-072, a **H1.2** do `PRD.md`,
  e a coluna do marco 1 no `PRD.md` §10

## Contexto

**O censo da R-5 mediu o que a fixture grava e a API não escreve.** O horário de funcionamento é
o primeiro da lista, e **é o primeiro de uma cadeia**:

> o `ESGOTADO_HOJE` exige **expediente corrente**, e a marcação responde **409**
> (`LojaSemExpediente`) numa loja sem horário. **Então o horário vem antes.**

**Isso liga os dois marcos.** *Marcar o que acabou* é a coluna do marco 2, cumprida em 15/10 numa
loja **semeada** — que tinha horário porque o `ApplicationRunner` o construiu. **Uma loja criada
por rota nasce `semHorario()`, e a marcação recusa.**

**O domínio já sabe modelar horário**, e a medição da E-4 mostrou exatamente como:

- `Disponibilidade(Map<DayOfWeek, List<Faixa>> horarioDeFuncionamento, Pausa pausa)`, *record*;
- `Faixa(LocalTime inicio, LocalTime fim)` — início inclusivo, fim exclusivo, segundos truncados;
- **`semHorario()` é `new Disponibilidade(Map.of(), Pausa.nenhuma())`**, e o javadoc diz que
  *"horário vazio é válido e significa 'nunca abre por horário'"*.

**O que falta não é o estado nem o modelo: é o ato, a rota, e quem pode fazê-lo.**

## Decisão

> **`PUT .../settings/horario` substitui o horário semanal inteiro, sob uma permissão nova e
> ampla — e não publica evento nenhum, porque a varredura já cobre o caso.**

### 1. `PUT`, e substitui tudo — o vazio inclusive

O horário semanal é **um valor só**, não uma coleção que se edita item a item: a rota recebe o
mapa inteiro e **substitui** o que havia. Duas chamadas iguais deixam a loja igual.

**E o corpo sem nenhuma faixa volta ao `semHorario()`** — não é caso especial nem rota nova: **é
o estado nomeado que o modelo já tem**, e a medição confirmou que o mapa vazio *é* ele. *Sem
isso, quem definisse um horário por engano entraria num estado que não conseguiria desfazer.*

**Por que não `PATCH` nem operações por faixa:** *"acrescente uma faixa na terça"* obriga o
cliente a saber o que já está lá, e cria estados intermediários que ninguém escolheu.

**A rota cabe no gateway sem mudança:** `/api/v1/merchants/{merchantId}/settings/**` já roteia
para o `merchant` (`id: merchant-settings`). ⚠ *Nenhuma entrada nova no `application.yml`, e
nenhum caso novo no `RoteamentoIT`.*

### 2. `CONFIGURAR_LOJA` — uma permissão, e ampla de propósito

**Não existia permissão para configurar a loja.** O `Permissao` tem dez valores — produto,
pedido, status, vendas, entrega, equipe, jornada — e nenhum diz isto. O `estabelecimento.md`
afirma que o `merchant` é *"onde mora a configuração"* e **não diz quem pode alterá-la**.

> **`CONFIGURAR_LOJA` governa a configuração da loja inteira** — horário agora, e as áreas de
> entrega, o desconto de retirada e o pedido mínimo quando chegarem.

**Por que ampla, e não uma por campo:** uma permissão por campo faz o enum crescer com cada
linha da H1.2, e o comerciante que delega configuração delega configuração — *não há caso de uso
de "pode mexer no horário mas não nas taxas" que alguém tenha pedido.* ⚠ *Quando alguém pedir,
a permissão se parte, e isso é emenda.*

**Por que permissão, e não papel.** Autorizar por *"só `ADMINISTRADOR`"* não mexeria no enum —
e criaria **um segundo critério de autorização** ao lado das permissões, no mecanismo mais
sensível do sistema. **A ADR-011 decide por permissão, e um mecanismo só é o que a ADR-071
defende.**

⚠ **E o custo é real, mecânico, e maior do que a versão anterior desta ADR dizia.** O
`Permissao` está **publicado** em quatro lugares — medido na E-4:

- no **contrato OpenAPI**, em **seis** enums inline;
- nos **tipos gerados do front**, e em **dois mapas exaustivos** — o `secoes.ts` e o
  `ConvidarParaEquipe.tsx`, que monta a lista de opções do convite com `Object.keys`;
- **dentro do `VinculoAlteradoV1`**, como `List<Permissao>` no payload publicado;
- e **na resposta da rota interna de autorização** — o `catalog` recebe a lista de
  permissões pelo `AutorizacaoComercialHttp`, a cada requisição que autoriza.

**O terceiro é o grave, e a ADR-027 o governa.** A §2 dela já tem a exceção — *"valor novo em
enum exige versão nova, salvo se todos os consumidores tratarem valor desconhecido como
'ignorar'"* —, e **aqui ela vale, medida**: o único consumidor do evento, o
`OuvinteDeVinculoAlterado` do `catalog`, não lê o campo; e o `AutorizacaoComercialHttp` mapeia
só o que o `PermissaoDoCatalogo` conhece e descarta o resto com um `log.info`, como já fazia
com as seis permissões que não são de produto. A emenda de hoje à ADR-027 registra a medição.

**O convite não oferece `CONFIGURAR_LOJA` nesta rodada.** O fundador a tem, porque o
`ADMINISTRADOR` recebe `EnumSet.allOf`. **A tela de convite fica igual** — se a oferecesse, uma
rodada de back mudaria tela, e este projeto não faz isso. ⚠ *A rota de convite **aceita** a
permissão; a tela não a mostra. A assimetria vai ao razão, com gatilho.*

### 3. A rota **não** pede passada nenhuma, e a janela não tem vítima

**Esta seção dizia o contrário, e a medição a derrubou.** A versão anterior mandava a rota pedir
uma passada de abertura para aquela loja, e justificava assim: *"quem configurasse o horário e
marcasse `ESGOTADO_HOJE` dentro da janela da varredura veria a marcação desfeita"*. **Isso não
acontece.**

**Por que não acontece — e são dois motivos independentes:**

1. **O carimbo e a abertura levam o mesmo expediente.** A marcação carimba
   `expedienteParaCarimbo(agora)`, que pergunta `inicioDaFaixaEm` — o dia operacional **da faixa
   corrente**, calculado do horário. A abertura publica `DiaOperacional.de(inicio, fuso)` — **o
   mesmo dia**. E o `catalog` só reativa o que tem carimbo **anterior**:
   `expedienteDeReferencia.isBefore(expedienteQueAbriu)`. **`E.isBefore(E)` é falso.** *A
   marcação feita na janela não é alcançada pela abertura que a segue.*
2. **E no caso principal não há o que reativar.** Uma loja `semHorario()` **não consegue** ter
   `ESGOTADO_HOJE`: a marcação responde 409. Então a primeira abertura depois do primeiro
   horário **não tem nada marcado para desfazer.**

> **A varredura cobre o caso, e o que sobra é atraso de até um intervalo dela — sem consequência
> que alguém possa nomear.** A rota escreve o horário e termina.

**E a `ADR-046` não é emendada:** a `@Scheduled` continua sendo o único chamador da varredura.
*Acrescentar um segundo chamador seria uma segunda forma de disparar a abertura, para comprar 60
segundos que nada observa.*

⚠ **O que fica registrado, porque é verdade e não é óbvio:** entre escrever o horário e a próxima
passada, a loja **está aberta** e o `catalog` **ainda não soube**. O que depende disso é a
reativação de marcações de **expedientes anteriores** — e essas esperam, no máximo, um intervalo.

### 4. A recusa atravessa a fronteira com nome próprio

**O domínio recusa duas coisas, e só duas** — medido: faixa com **início igual ao fim**, e **a
mesma faixa repetida no mesmo dia**. As duas lançam `IllegalArgumentException`, e o `CLAUDE.md`
**proíbe tratador para ela**.

**Então a aplicação traduz**, como o `CadastroDeLojaRecusado` já faz na criação da loja: a
construção do horário é envolvida **estreitamente**, e a recusa sai com exceção própria —
`HorarioRecusado` — e um **motivo**. **A frase que vai à tela é do tratador, por motivo, e não a
mensagem do domínio** (W-E, a regra do `CLAUDE.md`): a mensagem do domínio carrega a faixa como
chegou, e fica na **causa**, para o log e os testes. *A primeira versão desta seção dizia "com a
mensagem do domínio", e isso contradizia o molde que ela mesma citava.*

**Os dois `catch` são estreitos, e a medição diz por quê.** Um envolve a construção de cada
`Faixa` — início igual ao fim. O outro envolve o `comHorario`, onde a `Disponibilidade` recusa a
faixa repetida. O `comHorario` passa também pelo construtor da loja, que verifica as áreas
(M9, M10) e também lança `IllegalArgumentException` — **mas sobre as mesmas áreas que já
passaram por essa verificação quando a loja foi reconstituída**, e por isso esse caminho não
se alcança a partir de um horário.

**E o que o domínio permite de propósito continua permitido:** **sobreposição é redundância, não
contradição**, e **fim antes do início é a faixa que cruza a meia-noite**. ⚠ *As duas vão a
teste, afirmando que são aceitas — senão a próxima pessoa as "conserta".*

### 5. O que esta ADR **não** faz

**Não faz a pausa manual**, que a H1.2 lista junto: é outro ato, imediato, e tem uma pergunta de
produto que ninguém respondeu — **quando ela expira**.

**Não faz as áreas de entrega, o desconto de retirada nem o pedido mínimo**, ainda que a
permissão nova já os cubra.

**Não faz tela.** ⚠ *É a figura que o vigésimo nono defeito nomeou — rota sem chamador — pela
quarta vez, e fica escrita aqui com nome em vez de ser descoberta depois.*

**Não muda o significado do `ExpedienteAlteradoV1`**, nem o consumidor do `catalog`, nem a
varredura — **nem a `ADR-046`**, que continua com a `@Scheduled` como único chamador.

**E não oferece a permissão nova no convite**, nem muda a tela dele.

**E não muda o semeador**, que continua construindo a loja pelos construtores.

## Consequências

**A coluna do marco 2 passa a ser alcançável numa loja criada por rota.** ⚠ *Alcançável não é
alcançada: ninguém a percorreu assim, e a tela do horário não existe.*

**A lista da ADR-059 perde um item** — e o gatilho dela se paga quando a lista esvaziar, não
agora.

**O `Permissao` cresce, e com ele o contrato, os tipos do front e um evento publicado.** É a
primeira vez que uma rodada de back muda arquivo gerado que o front compila. **Uma permissão sem
seção no painel é o esperado**, e o segundo mapa — o do convite — fica de fora pela §2.

**E é a primeira rota de escrita que altera um agregado existente.** Até aqui a escrita do
`merchant` **criava** — a loja, o convite, o aceite. Esta **altera**, e por isso autoriza
**antes** de buscar, como o convite faz: *o `MinhasLojasController` não serve de molde, porque é
a única escrita sem autorização contra vínculo (ADR-060) — ele cria a própria loja.*

## Alternativas consideradas

**`PATCH` com o horário entre outros campos.** **Recusada:** mistura atos de semânticas
diferentes — mudar o fundo de troco não começa expediente nenhum.

**Operações por faixa.** **Recusada pela §1:** estado intermediário que ninguém escolheu, e a
tela que vai montar isso monta a semana inteira.

**Autorizar por papel, só `ADMINISTRADOR`.** **Recusada pela §2:** segundo critério de
autorização, sem ADR que o admita fora da A1/A3.

**Uma permissão por campo de configuração.** **Recusada pela §2:** o enum cresceria com cada
linha da H1.2, e ninguém pediu a distinção.

**A rota pedir uma passada de abertura para aquela loja.** Era a decisão da versão anterior
desta ADR, e a razão dela era falsa — a §3 mostra por quê. **Recusada:** compraria até 60
segundos de antecipação em troca de um segundo chamador da varredura, e **nada observa esses 60
segundos**. *Foi o agente que a derrubou, medindo o carimbo contra a reativação, antes de
escrever uma linha.*

**Mudar as duas recusas do domínio para exceção própria**, em vez de traduzir na aplicação.
**Recusada:** o domínio mudaria por conveniência da rota, e os testes de unidade que afirmam
`IllegalArgumentException` mudariam com ele. *Medido na E-4: são dois caminhos de recusa, e só
dois; se um terceiro aparecer, esta alternativa volta.*

**Fazer a tela junto.** **Recusada pelo corte:** este projeto nunca fez back e front na mesma
rodada, e a mudança de arquivo gerado desta rodada já encosta nessa fronteira.

## Gatilho escrito

**A primeira tela que monte o horário da semana.** Aí a rota ganha chamador, e a coluna do marco
1 ganha a ponta que falta.

**O primeiro comerciante que pedir para fechar a loja agora, sem mexer no horário.** Aí nasce a
pausa manual, com a pergunta de quando ela expira.

**E o primeiro pedido para delegar um campo de configuração sem delegar os outros.** Aí o
`CONFIGURAR_LOJA` se parte.

**E a primeira tela de convite que ofereça `CONFIGURAR_LOJA`.** Hoje a rota a aceita e a tela não
a mostra.
