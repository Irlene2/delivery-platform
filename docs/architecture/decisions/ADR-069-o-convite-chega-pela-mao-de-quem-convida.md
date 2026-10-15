# ADR-069 — O convite chega pela mão de quem convida, e o token se lê uma vez

- **Status:** aceita
- **Data:** 15/10/2026
- **Rodada:** E-2
- **Relacionada:** ADR-011 (autorização comercial), ADR-012 e ADR-044 (o roteamento e a cadeia
  do gateway), ADR-037 (o token de pessoa), ADR-042 (o transporte humano do código de
  verificação), ADR-053 (o contrato declara), ADR-055 (corpo de erro), ADR-060 (a primeira
  rota de escrita), a M4 e a A2 do `estabelecimento.md`, a M7, e a coluna do marco 1 no
  `PRD.md` §10

## Contexto

**A coluna do marco 1 diz *"cadastrar a loja e o time"*, e metade dela não existe.** A loja
foi cumprida no navegador em 10/10. O time tem **domínio completo desde a B1 e a B2** — o
`Membro` como raiz, a `Equipe` como única entrada, o `Convite` com token e telefone, a A2
conferida na emissão **e de novo no aceite** — e **nenhuma rota de escrita**. O que tem rota
é o `GET .../team`, da C-A.

**E o marco 1 está declarado cumprido em dois lugares do repositório** — `README.md`:12 e
`ADR-016`:68 — **e negado em três**, todos na ADR-060. Medido no bloco 7 da E-3c.

**O convite tem o mesmo buraco do código de verificação, numa posição melhor.** Não há
`CanalPort`: o `conversation` não existe. A ADR-042 resolveu o código com **transporte
humano** — o operador lê a tabela e entrega. Aqui não é preciso chegar a tanto: **quem
convida é uma pessoa diante de uma tela**, e o convidado é alguém que ela já conhece e já
tem como alcançar.

## Decisão

> **O 201 devolve o link do convite, a tela o mostra uma vez, e o administrador o entrega
> por onde já fala com aquela pessoa. O token nunca mais é legível.**

```
POST   /api/v1/merchants/{estabelecimentoId}/team/convites   201   GERENCIAR_EQUIPE
GET    /api/v1/convites/{token}                              200   público
POST   /api/v1/convites/{token}/aceite                       200   autenticado, sem permissão
```

⚠ **A forma e os nomes seguem o que a §2 medir** — o precedente é a rota de criar loja
(ADR-060) e o `GET .../team` (C-A). **Vale o que estiver no controlador**, não o que esta ADR
desenhou.

### 1. O token é credencial, e credencial não se relê

O corpo do **201** traz o link. A tela o mostra para copiar. **A listagem de convites nunca
o traz** — ela mostra quem foi convidado, quando, para qual papel e até quando vale.

**Perdeu o link?** Cria outro. O anterior continua valendo até expirar, e isso é melhor do
que o contrário: um token que se pode reler é um token que vaza com a aba aberta, com o print
do grupo, com quem senta na cadeira depois. *A ADR-042 aceitou texto claro para o código de
verificação porque ele vive dez minutos e cinco tentativas; o convite vive dias.*

⚠ **A §2 mede se o agregado tem caminho para revogar.** Se tiver, a rota **não nasce nesta
rodada** — mas o fato vai escrito, com gatilho. Se não tiver, também vai escrito: *criar
outro não cancela o primeiro*, e quem lê a tela precisa saber disso.

> **Medido na E-2: tem.** `Equipe.cancelarConvite` existe desde a B2, confere A1 e só cancela
> o `PENDENTE`. **Não tem rota**, e criar outro convite continua não cancelando o primeiro.
> *Está no `dividas.md`, com gatilho.*
>
> **E o 201 devolve o token, não o link montado.** O endereço da página do convite é do
> front, e o `merchant` não o conhece; quem monta o link é a tela que o mostra para copiar.
> O token só é lido em um lugar fora da persistência: o `EquipeController`, na resposta do
> 201 — e nenhum log o escreve.

### 2. A página do convite é pública, e diz o mínimo que convence

`GET /api/v1/convites/{token}` responde **sem token de sessão**, e é a única rota pública que
esta rodada acrescenta. Ela existe porque **quem recebe um link quer saber o que é antes de
criar conta** — e uma tela que exige login primeiro faz a pessoa decidir no escuro.

**O que ela devolve:** o **nome da loja**, o **papel oferecido** e **até quando vale**.

> **Medido na E-2: o convite não tem papel.** O aceite cria o vínculo sempre como
> `COLABORADOR` (B2), e o que se oferece é um conjunto de permissões. O `papel` da resposta
> é, então, sempre `COLABORADOR` — e é a verdade, que é o que a página tem de dizer. As
> permissões oferecidas **não** saem: a decisão desta ADR é o mínimo que convence.

> **O que ela não devolve, e é medido:** **quem convidou**. O `merchant` guarda
> `usuarioId`, o nome mora no `identity`, e a rota *"o usuário do token"* **não existe** —
> está na tabela de pendências do `premissas-do-front.md` desde a G-B5, sem rodada marcada.
> **Inventar um nome na tela seria escrever o que nenhum serviço sabe.**

**E não devolve o telefone do convidado**, nem nada que identifique pessoa. A premissa P3 do
PRD — o comerciante é o cliente — não autoriza expor terceiros, e **um link é uma coisa que
se encaminha**.

**Gatilho escrito:** o dia em que a rota do usuário do token existir. Aí a página diz *"a
Marli te convidou"*, e **só aí**.

### 3. Token inválido, expirado e já usado são a mesma resposta

**404, com o mesmo corpo, nos três casos.**

**É a M7, aplicada a um identificador que não está sob autorização nenhuma.** Distinguir
*"não existe"* de *"expirou"* conta, a quem tenta tokens no escuro, **quais já existiram** —
e o custo de não distinguir é uma frase um pouco mais vaga para quem tem um link velho:
*"este convite não vale mais; peça outro a quem te convidou"*.

**E isso vale também para o aceite:** aceitar duas vezes dá o mesmo 404 da primeira linha.
Um convite usado não é mais um convite.

⚠ **Se o agregado distinguir esses casos com exceções diferentes**, o tratador os une **na
borda**, e o domínio não muda. *A §3 do pacote diz como.*

### 4. O aceite exige sessão e **nenhuma permissão**

Quem aceita ainda não é da loja — **exigir permissão seria exigir que já fosse**. O que o
aceite exige é **saber quem está aceitando**, e isso vem do `sub` do token de pessoa
(ADR-037).

**O token do convite autoriza; o telefone endereça** — a decisão é da B2, e esta rodada
**não a toca**: quem apresenta o token entra, seja qual for o número dele. Conferir os dois
exigiria perguntar ao `identity` o telefone de um `usuarioId`, e essa porta não existe.
*Está no razão, com gatilho, desde o marco 1.*

**E quem volta pelo convite volta `COLABORADOR`** — também da B2, e pelo motivo escrito: sem
isso, um gerente restauraria um administrador removido sem nenhum administrador na jogada.

### 5. A A2 é conferida no aceite, e isso é a regra que mais importa aqui

A B2 decidiu: **a A2 vale na emissão e de novo no aceite**, porque entre as duas o convidante
pode ter perdido a permissão, sido suspenso ou saído.

**Essa regra existe no agregado e nunca foi exercida por uma rota.** Esta rodada lhe dá
chamador — e o teste que a prova é o que separa *"a regra está escrita"* de *"a regra
acontece"*.

> **Medido na E-2: a recusa é 409**, com frase própria. O domínio a lança com as mesmas
> exceções que, na emissão, querem dizer *"você ofereceu o que não tem"* — e quem aceita não
> tem o que corrigir. O caso de uso do aceite as traduz numa exceção própria; o domínio não
> muda. **Distinguir esta recusa do 404 não conta nada a quem adivinha**: ela só acontece
> depois de o token conferir.

### 6. O que esta ADR **não** faz

**Não dá rota a promover, rebaixar, remover, suspender nem sair.** A coluna diz *cadastrar* o
time; as outras cinco são **gestão**, e entram com a tela que as peça. *Cada uma com gatilho.*

**Não manda mensagem.** Não há canal, e o transporte é humano — como na ADR-042, com a
condição de volta escrita.

**Não dá tela.** É a rodada de front seguinte, e **a coluna do marco 1 só se afirma depois de
duas pessoas num navegador** — a que convida e a que aceita.

**E não corrige o `README.md`:12.** Enquanto a coluna não for cumprida por gente, trocar a
frase seria pôr data nova numa afirmação ainda falsa.

## Consequências

**O `GerenciarEquipeService` ganha o primeiro chamador que não é teste.** ~~Ele é o funil das
oito escritas desde a C-B, e emite o `VinculoAlteradoV1` num ponto só~~ — *corrigido na
E-2, medido:* ele emite em **dois** pontos, o `executar` das sete operações e o
`registrarVinculoNascido` do aceite. **Convidar mora nele e não emite**: não muda vínculo.
**O outbox do `merchant` passa a ser exercido por uma pessoa no aceite**, que é onde o
vínculo nasce.

**O gateway ganha um prefixo público**, e é a primeira mudança nele desde a E-A. ⚠ A §2 mede
como os públicos são declarados hoje; **a forma é a de lá**.

~~**A revogação de acesso em segundos deixa de ser teoria.**~~ *Corrigido na E-2:* esta
rodada não dá rota a nada que revogue — remover e suspender continuam sem borda. O
`VinculoAlteradoV1` passa a ser disparado por um ato humano **no aceite**, que concede; a
revogação em segundos continua teoria até a rota de remover.

## Alternativas consideradas

**O operador lê o token no banco**, como o código de verificação. **Recusada:** ali não havia
ninguém com tela; aqui há. Pedir ao administrador que abra o `psql` é desenhar para o
operador quando o usuário já está na frente do sistema.

**A lista de convites mostra o link enquanto vale.** Resolve o caso real de perder a
mensagem. **Recusada pela §1:** faz a credencial legível para qualquer um com
`GERENCIAR_EQUIPE`, a qualquer momento, até expirar. *Está no gatilho.*

**O link exige sessão e manda para o login.** Nada de rota pública nem tela nova.
**Recusada pela §2:** a primeira coisa que o convidado veria é um formulário sem contexto.

**Distinguir expirado de inexistente.** Mensagem melhor. **Recusada pela §3:** conta quais
tokens existiram.

**As sete operações da equipe nesta rodada.** **Recusada pelo corte da coluna** — e pelo
trigésimo quarto defeito: sete verbos numa linha escondem quantas camadas cada um atravessa.

## Gatilho escrito

**A primeira tela que peça para promover, remover, suspender ou sair.** Aí as cinco viram
rotas, e o `GerenciarEquipeService` já as tem.

**O dia em que a rota do usuário do token existir** — a página do convite passa a dizer quem
convidou.

**O dia em que alguém perder o link e reclamar.** Aí se decide entre revogar-e-recriar e
mostrar o token na lista, **com o caso real na mão** em vez de com a minha imaginação.

**E o dia em que houver `CanalPort`.** O transporte deixa de ser humano, e esta ADR vira
sobre o canal — como a ADR-042 já previu para o código.
