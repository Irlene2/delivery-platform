# ADR-071 — A lista tem um dono, e a cópia não é lista

- **Status:** aceita
- **Data:** 16/10/2026
- **Rodada:** R-3
- **Relacionada:** ADR-012 (rotas por recurso no gateway), ADR-038, ADR-044 §6, ADR-047,
  ADR-060 (o marco se mede pela coluna), o `README.md` da raiz, o `README.md` das decisões,
  `docs/dividas.md`, e o quadragésimo sétimo defeito — *parar de afirmar o falso nunca tem
  gatilho*

## Contexto

**A R-2 foi triar dívida e achou outra coisa.** De trinta e duas ocorrências da segunda
passada do razão, vinte foram classificadas — e, no caminho, apareceram frases que **não
esperam nada**. Elas não têm condição pendente: **já estão erradas, agora.**

**E o razão não as vê**, porque ele rastreia **condição com gatilho**: cada linha dele promete
que alguma coisa vai acontecer e que, quando acontecer, a linha se paga. **Uma contradição não
promete nada.** Não há o que esperar: há texto a consertar.

### As duplicações que a medição da R-3 achou

⚠ **Nenhuma delas foi achada por varredura.** Apareceram de lado, numa triagem que buscava
outra coisa, e **a última foi achada por quem aplicou esta rodada, criticando a §2 do primeiro
rascunho dela**. Os números abaixo são os medidos no relatório da R-3.

1. **A regra da emenda, escrita em dois lugares — e são duas regras, não duas versões da
   mesma.** O `README.md` da raiz manda **emenda datada ao fim, e o corpo fica**. O
   `README.md` das decisões manda **bloco de emenda no topo**, e diz *"o texto abaixo do bloco
   é sempre o vigente — a versão anterior vive no histórico do Git"*: **isso é reescrever o
   corpo.** *O primeiro rascunho desta ADR afirmava que as duas concordavam sobre o corpo e
   discordavam só do lugar. Não é o que está escrito, e a premissa que o afirmava era a única
   da rodada com um `pare`.*
2. **A tabela de rotas da ADR-012 contra o `application.yml`:** a tabela lista 15 rotas, o
   arquivo roteia **16**.
3. **O nome do parâmetro, duas grafias em três lugares:** `{merchantId}` na tabela e nas nove
   rotas do `application.yml`; `{estabelecimentoId}` no Contexto da ADR-012 e na emenda dela de
   26/08.
4. **O `RoteamentoIT` é uma terceira cópia da lista de rotas.** Ele afirma as dezesseis, uma a
   uma, num `@CsvSource` escrito à mão — **e não lê o `application.yml`**. Rota nova sem caso
   escrito **passa em silêncio**.
5. **E as próprias formas de emendar divergem dentro dos arquivos:** a ADR-009 e a ADR-014
   carregam **as duas** — bloco v1.1 no topo *e* `## Emenda` ao fim —, e a ADR-012 tem uma
   **terceira**: uma emenda em nota de rodapé no meio do corpo (linha 91, 24/09, ADR-044 §6).

**A primeira é a mais grave, e o motivo é de categoria:** é a única que faz o repositório **se
contradizer sobre como se corrigir**. Toda emenda escrita daqui para frente — e este projeto
conserta por emenda — herdava a dúvida.

**A quarta é a mais instrutiva**, e é a que mudou esta decisão: o primeiro rascunho tirava a
tabela da ADR-012 e apontava o teste do gateway como quem verifica o dono. **O teste não
verifica o dono: ele o transcreve.** *Tirar uma cópia e apontar outra como garantia é trocar de
cópia.*

## Decisão

> **Toda regra e toda lista deste repositório tem um lugar só. O segundo lugar não se
> sincroniza: ele vira ponteiro, deixa de ser normativo, ou passa a ser derivado do dono por
> comando.**

### 1. A regra da emenda mora com as ADRs, e é uma regra só

O dono é **`docs/architecture/decisions/README.md`**, porque quem precisa da regra é quem está
escrevendo uma ADR, e essa pessoa está nesse diretório. O `README.md` da raiz **perde o texto
da regra** e fica com **uma linha** que diz onde ela está.

**E a regra é a que a prática já segue:** **emenda datada ao fim, e o corpo registra a decisão
como ela foi tomada, inclusive quando estava errada.** Medido: **33** ADRs têm `## Emenda de
…`, e **todas** depois de *"Alternativas consideradas"*. **Três** têm bloco no topo com o corpo
reescrito — 009, 010 e 014 —, e **todas as três são da revisão v1.1, de agosto**.

⚠ **A regra do bloco no topo não some: ela deixa de ser regra e passa a ser registro.** A v1.1
foi um evento datado, e **regra escrita para um evento que já passou não é regra** — é a
descrição de uma coisa que se fez uma vez. *Que a ADR-009 e a ADR-014 tenham hoje as duas
formas é a prova de que a do topo não sobreviveu ao próprio mês.*

### 2. A lista de rotas do gateway é o `application.yml`, e o teste passa a derivar dele

A tabela da ADR-012 **deixa de ser normativa**, por emenda datada. A lista das rotas é o
`application.yml`.

**E isso só vale porque esta rodada conserta o verificador.** O `RoteamentoIT` passa a **ler as rotas
que o gateway carregou do `application.yml`** — o `GatewayMvcProperties`, o mesmo objeto de
configuração de onde o gateway as monta, pedido ao contexto do teste, e não o arquivo aberto à
mão — e a falhar quando existir entrada `- id:` sem caso afirmado. Antes desta
mudança ele era a terceira cópia da lista; depois, é o comando que confere o dono.

⚠ **O que torna um verificador aceitável é a comparação ser total**, não o arquivo ser gerado.
A asserção é **igualdade de conjunto nos dois sentidos** entre os `id` do `yml` e os `id` que
os casos afirmam: rota nova sem caso falha, e caso sobre rota que não existe também. *Uma
verificação parcial — "todo id do caso existe no yml" — deixaria passar exatamente a falha que
motivou esta decisão.*

⚠ **E o que resta de cópia fica nomeado:** a ligação entre o caso e a rota é o `id` escrito à
mão na coluna do caso, então **um caso pode afirmar o `id` errado e passar**. Isso **é** linha
de razão, com gatilho: *o dia em que o teste resolver o caminho pelo próprio roteador em vez de
pelo `id` declarado.* É dívida de verdade — tem condição, e a condição não é *"alguém
decidir"*.

⚠ **Sem essa mudança a decisão não se sustentaria.** A alternativa era escrever um gatilho
cuja condição é *"a próxima rota que o gateway ganhar"* — **exatamente o gatilho que esta
rodada está retirando da emenda anterior da ADR-012, no mesmo dia, por ser adiamento
disfarçado de método**. *Repetir a forma do quinquagésimo defeito para fugir dele é pior do que
gastar dez linhas de teste.*

⚠ **A decisão da ADR-012 não muda, e o corpo dela não se reescreve.** *Rotas por recurso, e
não por serviço* continua valendo, e é o que o `application.yml` faz. O que a emenda retira é a
pretensão da **tabela** de ser a lista.

**E com ela cai a terceira duplicação:** o parâmetro divergia porque havia duas listas.

### 3. Sincronizar não é saída, e o motivo é mecânico

Duas cópias com obrigação de sincronizar criam **um gatilho que dispara em toda edição** — e
gatilho que dispara sempre não é lido por ninguém. Foi o que aconteceu duas vezes com a tabela
da ADR-012: ela envelheceu em silêncio, e **quem acrescentou a rota não tinha por que
suspeitar que havia um inventário em outro arquivo**.

> **A cópia não avisa que envelheceu. O dono não tem com quem divergir.**

**As três saídas, e a ordem de preferência:**

| Saída | Quando | Exemplo nesta rodada |
|---|---|---|
| **ponteiro** | o segundo lugar só precisa dizer onde a coisa está | o `README.md` da raiz |
| **derivar por comando** | a cópia tem função própria — verificar, publicar | o `RoteamentoIT` lendo o `yml`; o `dividas.md`, que é derivado desde que nasceu |
| **deixar de ser normativa** | a cópia é registro histórico e não se reescreve | a tabela da ADR-012 |

⚠ **Verificar não é derivar.** Uma cópia que confere o dono mas foi escrita à mão continua uma
cópia: ela só acusa divergência nos pontos que alguém lembrou de escrever.

### 4. O que esta ADR **não** faz

**Não varreu o repositório procurando duplicações.** As cinco apareceram por acidente, em duas
rodadas. Podem ser cinco; podem ser cinquenta.

⚠ **E isto não vira linha de razão**, pelo próprio critério do razão: não existe comando que
enumere *"regra escrita em dois lugares"*, então não existe gatilho, e **linha sem gatilho é
ruído dentro do instrumento que existe para não ter ruído**. Fica escrito aqui, onde se lê a
decisão, e não lá, onde se cobra uma promessa.

**Não unifica as formas de emenda que já estão nos arquivos.** A ADR-009, a ADR-014 e a ADR-012
continuam como estão — **o corpo não se reescreve**, e isso vale também para o corpo que
contém uma emenda malposta.

**Não decide onde mora o validador de `iss` e `aud`.** Essa pergunta é da ADR-038, continua
aberta, e esta rodada apenas conserta a frase dela que dizia que o segundo consumidor não
existia.

## Consequências

**A ADR-012 passa a ter duas emendas em dois dias, e a segunda retira o gatilho que a primeira
escreveu.** A emenda da R-2 dizia, sobre tirar a tabela: *"a segunda saída é melhor, e exige
decisão que esta emenda não toma"*. **Eu já sabia qual saída era melhor e escrevi um gatilho em
vez de fazer a pergunta** — é o quinquagésimo defeito, e o custo dele está medido: um arquivo
de decisões com duas entradas para registrar uma.

**O `RoteamentoIT` deixa de ser lista e passa a ser verificador**, e com isso o gateway ganha a
primeira garantia de **completude** de rota deste projeto. *Antes dela, todo teste de
roteamento afirmava um conjunto que alguém digitou.*

**A segunda passada do razão ganha uma saída que não existia.** Até aqui ela tinha três
destinos — gatilho, nunca-será, e fora. Agora tem um quarto, e é o primeiro que **não é uma
linha**: *a condição venceu e a frase ainda a afirma* → **conserta-se o texto, agora**. A R-2
mandou vencidas para o razão e deixou as frases de pé: **registrar não é consertar**, e é o
quinquagésimo primeiro defeito.

**E o repositório passa a ter uma regra sobre as suas próprias regras**, que é o tipo de texto
que este projeto só escreve depois de tropeçar.

## Alternativas consideradas

**Sincronizar as cópias e seguir.** Mais barato hoje. **Recusada pela §3:** era o que estava em
vigor, implicitamente, e produziu as cinco divergências.

**Deixar o `README.md` da raiz ser o dono da regra da emenda.** Ele é o primeiro texto que
qualquer pessoa lê. **Recusada:** quem vai escrever uma ADR abre o diretório das ADRs, e a
regra tem de estar onde o ato acontece.

**Manter as duas regras com escopos distintos** — revisão de escopo reescreve o corpo com bloco
no topo; mudança de decisão ganha emenda ao fim. É o que a história sugere. **Recusada:** a
fronteira entre as duas é julgamento, e a ADR-009 e a ADR-014 **já têm as duas formas** sem que
ninguém tenha decidido qual se aplicava. *Regra cuja aplicação depende de classificar o caso
produz exatamente a divergência que ela existe para evitar.*

**Manter os dois textos, marcando um como cópia.** **Recusada:** o rótulo não impede a
divergência, só dá a quem a encontrar uma pista de qual acreditar — e a divergência que
tínhamos já era legível; ninguém a tinha lido.

**Manter a tabela da ADR-012 com o comando que a recalcula**, como o `dividas.md` faz no
cabeçalho. **Recusada:** o comando revela a divergência **depois** que ela existe, e continuam
sendo duas listas. *No `dividas.md` o comando é o dono — o arquivo é derivado. A tabela da
ADR-012 não é derivada de nada: é uma transcrição.*

**Apagar a tabela do corpo da ADR-012.** **Recusada por regra**, não por gosto: o corpo da ADR
registra a decisão como ela foi tomada, inclusive quando envelheceu.

**Tirar a tabela e deixar o teste como estava**, com uma linha de razão para o dia em que
alguém o fizesse derivar. **Recusada pela §2:** a condição desse gatilho é *"a próxima rota"* —
o mesmo gatilho que esta rodada retira. **Recusar uma cópia escrevendo o gatilho que acabei de
condenar seria a decisão se contradizendo na mesma página.**

## Gatilho escrito

**A próxima regra que alguém quiser escrever num segundo lugar.** Ela não se escreve:
escreve-se o ponteiro, ou o comando que a deriva.

**A próxima lista que aparecer transcrita num teste.** O `RoteamentoIT` foi a primeira achada;
⚠ **ninguém procurou as outras**, e não há comando que as ache.

**E o dia em que existir comando que enumere regra duplicada.** Aí se sabe se são cinco ou
cinquenta. **Sem comando não há varredura**, e por isso a §4 não virou linha de razão.
