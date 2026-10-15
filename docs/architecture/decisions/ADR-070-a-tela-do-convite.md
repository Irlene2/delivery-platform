# ADR-070 — A tela do convite, e o segredo que já está no navegador de quem o recebeu

- **Status:** aceita
- **Data:** 15/10/2026
- **Rodada:** W-G
- **Relacionada:** ADR-016 (front mínimo), ADR-047 (a sessão em `sessionStorage`), ADR-055
  (corpo de erro), ADR-061 (a loja escolhida mora na URL), ADR-067 (o cardápio ganha
  escrita), ADR-069 (o convite chega pela mão de quem convida), a P3 do PRD, e a coluna do
  marco 1 no `PRD.md` §10

## Contexto

**A E-2 deu rota a convidar, ver e aceitar, e nenhuma tela as chama.** É a mesma figura que
a E-1 e a E-3c produziram, e que o vigésimo nono defeito nomeou: rota sem chamador.

**A coluna do marco 1 diz *"cadastrar a loja e o time"*.** A loja foi cumprida em 10/10; o
cardápio do marco 2 em 15/10. **Esta é a última metade aberta** — e o `README.md`:12 continua
afirmando, desde 26/09, que o marco 1 está concluído.

**E a E-2 deixou um fato visível que decide o tamanho desta rodada:** o `merchant` guarda
`usuarioId`, **o nome mora no `identity`, e a rota que o traria não existe**. Uma lista de
membros hoje seria uma lista de identificadores.

## Decisão

> **A seção "Equipe" nasce com o convite e sem lista; a página do convite é pública; e o
> token mora na URL, do começo ao fim.**

```
/painel/lojas/:estabelecimentoId/equipe   convidar, e o link mostrado uma vez
/convite/:token                           pública: o que é, e os dois caminhos
```

### 1. A seção nasce com o convite, e sem lista — e o motivo é o nome

`GERENCIAR_EQUIPE` já está no mapa de seções do painel, **com tela nenhuma**. Esta rodada a
liga, e o que há dentro é **o formulário de convite**.

**Não há lista de membros**, e não é economia: o `GET .../team` existe desde a C-A, e **a
tela não teria o que escrever na coluna da pessoa**. Uma lista que responde *"quantos"* e não
*"quem"* convida a primeira pergunta que ninguém consegue responder.

**Nem lista de convites pendentes** — essa rota não existe. ⚠ **Consequência que vai escrita
na tela e no razão:** quem convida copia o link **agora**, porque depois não há onde
reencontrá-lo nem como saber quem está pendente.

### 2. O link é montado aqui, e mostrado uma vez

**O `merchant` devolve o token, não o link** — ele não conhece o endereço das telas, e foi
certo ter me corrigido nisso. **Quem monta o link é o front**, com a própria origem.

A tela o mostra **uma vez**, em texto selecionável, com um botão de copiar **e** o texto à
vista: `navigator.clipboard` exige contexto seguro e pode falhar, e **um link que só existe
dentro de um botão quebrado é um link perdido** (ADR-069 §1 — não há como relê-lo).

**E diz o que está em jogo:** *copie agora; este link não aparece de novo*.

### 3. O token mora na URL — e o inventário do segredo é dos lugares onde **outros** leem

O quadragésimo quinto defeito ensinou a contar onde a pilha escreve um segredo, e essa lição
**não pode virar reflexo**. A distinção que vale:

> **Quem abre o link já tem o token no navegador dele.** Ele chegou por mensagem, está na
> barra de endereços e no histórico **daquela pessoa**. O que a ADR-069 impede é
> **multiplicá-lo em lugares que outros leem** — o corpo de erro que o servidor monta, o log
> do servidor, uma resposta de API.

Por isso o desvio do cadastro leva o destino **na query** — `/entrar?destino=/convite/…` — e
**não** em `sessionStorage`. Guardar em storage seria uma cópia a mais, num lugar que
sobrevive à navegação, **para não ganhar nada**: a URL de origem já tem o valor.

> **Medido na W-G: o mecanismo de destino já existia, e não é a query.** O `ExigeSessao`
> manda para `/entrar` com `state: { de }` (estado do roteador, guardado no histórico
> **daquela aba**), e o `EntrarPage` lê `state.de` e volta para ele. O pacote mandava
> seguir a forma que existisse, e esta rodada a segue: o convite entra pelo mesmo
> `state.de`. **O que a decisão protege continua protegido** — não é `sessionStorage`, não
> é estado global, e não sai do navegador de quem abriu o link. O que mudou foi o
> `CadastrarPage`, que navegava sempre para `/painel` e passou a honrar o mesmo destino.

⚠ **E nada de `Referer` para outra origem.** A política do front é
`strict-origin-when-cross-origin`, que já corta o caminho — **confirme na §2**, não suponha.

> **Medido na W-G: o repositório não declarava política nenhuma** — nem `<meta
> name="referrer">`, nem cabeçalho. `strict-origin-when-cross-origin` era só o **padrão
> do navegador**, e padrão de navegador não é decisão. **A página do convite chama o
> gateway, que é outra origem, com o token no caminho**: o `index.html` passa a declarar a
> política, e ela vira decisão do repositório.

### 4. A página pública diz o mínimo que convence, e oferece dois caminhos

Ela mostra **o nome da loja** e **até quando vale** — é o que a rota pública devolve
(ADR-069 §2), e **não há papel a mostrar**: o convite não carrega papel, e quem entra por ele
entra `COLABORADOR`.

> **Medido na W-G: a rota pública devolve `papel`**, sempre `COLABORADOR` — a E-2 o pôs na
> resposta e o relatório dela o disse. O convite não *carrega* papel, mas a resposta *diz*
> com que papel se entra. **A tela mostra o que veio**: *"você entra como colaborador"* é
> verdade, e é o que quem vai aceitar precisa saber. O que a tela não mostra é o que não
> veio — telefone, quem convidou, o token.

**Quem não tem sessão** vê *entrar* e *criar conta*, e volta para esta mesma URL.
**Quem tem** vê o botão de aceitar.

**Depois do aceite, o painel** — relendo as lojas, porque a loja nova não está na lista que
foi lida antes dela. *É o mesmo caminho que o cadastro de loja já faz.*

### 5. As recusas são as frases do servidor, e esta tela lê um recurso só

O `detail` vai à tela **como veio** (ADR-055, ADR-063): convite que não vale mais, quem
convidou perdeu o poder de conceder, você já faz parte da equipe.

**E aqui a mensagem genérica pode ser melhor do que a da W-F**, porque esta página lê **uma**
rota: não há dois recursos para colapsar numa frase. ⚠ *O erro que colapsa dois continua no
cardápio, registrado, e não é assunto desta rodada.*

### 6. O que esta ADR **não** faz

**Não pede rota nenhuma ao back.** ⚠ Se a tela precisar de uma, **pare e diga** — foi
exatamente assim que a W-F parou, e parar é o comportamento certo.

**Não lista membros nem convites pendentes.** **Não dá tela a promover, remover, suspender ou
sair** — as sete continuam só no agregado.

**E não corrige o `README.md`:12.** A coluna só se afirma depois de **duas pessoas** num
navegador, e trocar a frase antes disso é pôr data nova numa afirmação falsa — o erro que
esta coleção de páginas levou dezenove dias para admitir.

## Consequências

**O convite deixa de ser uma rota e passa a ser um caminho entre duas pessoas** — e é a
primeira vez neste projeto que **duas contas** são necessárias para provar alguma coisa.

**O `VinculoAlteradoV1` passa a ser disparado por um ato humano**, pela primeira vez desde a
C-B.

**E a página pública é a primeira tela deste front sem sessão que não é login nem cadastro.**
⚠ Ela fica **fora** do `ExigeSessao`, e a §2 mede como o roteador o declara hoje.

## Alternativas consideradas

**Guardar o token em `sessionStorage` durante o desvio.** **Recusada pela §3:** cópia a mais,
que sobrevive à navegação, para não ganhar nada.

**Listar os membros mesmo sem nome.** **Recusada pela §1:** uma coluna de identificadores não
responde a pergunta que a tela levanta.

**Fazer login e cadastro dentro da própria página do convite**, sem desvio. Mantém o token
numa URL só. **Recusada:** duplica duas telas que já existem, e o desvio com destino é o
padrão que o front vai precisar de qualquer forma.

**Incluir a rota de listar convites pendentes.** Resolve um buraco real de uso. **Recusada
pelo corte:** atravessa duas camadas, e esta rodada é a que fecha a coluna. *Está no gatilho.*

## Gatilho escrito

**O primeiro administrador que perguntar quem está pendente.** Aí nasce a listagem de
convites — rota e tela.

**O dia em que existir a rota do usuário do token.** Aí a lista de membros tem o que
escrever, e a página do convite passa a dizer quem convidou.

**E a primeira tela que peça para promover, remover ou suspender.** As sete esperam ali.
