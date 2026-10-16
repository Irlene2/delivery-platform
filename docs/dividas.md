# O razão das dívidas

**Gerado em 10/10/2026 na rodada R-1, sobre o commit `6fb73e6`; refeito na E-1, sobre `d7dbcc4` mais a ADR-060, na W-E, sobre `398d828` mais a ADR-061, na E-3b, sobre `9fbcee1` mais a ADR-064, na E-3c, sobre `cbd633c` mais as ADRs 065 e 066 e o `PublicarProdutoService`, na E-3d, sobre `a3ffe07` mais a ADR-068, na W-F, sobre `ec89666` mais a ADR-067, na E-2, sobre `ef82bc9` mais a ADR-069, na W-G, sobre `92e8099` mais a ADR-070, na R-2, sobre `e43d227` mais a emenda de 16/10/2026 à ADR-012, na R-3, sobre `0e545c0` mais as emendas às ADRs 024 e 038 e as frases trocadas no lugar, e na R-4, sobre `4904159` mais a ADR-072 e a emenda de 17/10/2026 à ADR-071. Este arquivo é
derivado e envelhece:** toda ADR, documento ou javadoc que escrever um gatilho novo o
torna incompleto. Refaça-o com os comandos abaixo antes de confiar nele.

```bash
# os candidatos — toda menção a gatilho, em documento e em código
grep -rn -i "gatilho" docs/ contracts/ CLAUDE.md README.md frontend/README.md --include=*.md --exclude=dividas.md
grep -rn -i "gatilho" backend/ frontend/src/ --include=*.java --include=*.ts --include=*.tsx | grep -v "/build/\|/node_modules/\|\.tsbuild"

# o recorte estreito, que é o que a R-1 mandou contar
grep -rn "Gatilho escrito\|gatilho escrito" docs/ contracts/ CLAUDE.md backend/ frontend/ --include=*.md --include=*.java --include=*.ts --include=*.tsx --exclude=dividas.md | grep -v "/build/\|/node_modules/" | wc -l
```

**Segunda passada, pelas formas em prosa.** O comando acima só acha o que usa a fórmula.
Condição escrita como *"nesta altura"*, *"por enquanto"*, *"ainda não"*, *"quando houver"*
é gatilho também, e some deste arquivo — a da `RegraDoCatalogoViolada` venceu na G-E e
nunca apareceu aqui:

```bash
grep -rn -i "nesta altura\|por enquanto\|ainda não existe\|quando houver\|até que haja" \
  docs/ backend/*/*/src/main --include=*.md --include=*.java --exclude=dividas.md \
  | grep -v "/build/"
```

**O `--exclude=dividas.md` não é detalhe.** Sem ele a varredura conta **este arquivo**: o
cabeçalho que a descreve e as linhas que copiam as fórmulas viram ocorrências, e o número
cresce quando o razão cresce. Medido na W-F — uma das ocorrências novas era a frase escrita
no cabeçalho na mesma rodada. **Instrumento que se mede a si mesmo não está medindo.**

Medido na E-3b: **32 linhas**; na E-3c, **36** — que já envelheceu na própria rodada: a frase abaixo, escrita depois da medição, levou o número a **37**, e a E-3d mediu 37, com a mesma contagem por arquivo; na W-F, **38**, e a nova é a ADR-067:23 — *"a seção, por enquanto, não significa nada"* —, condição que a própria rodada desfez ao dar tela à seção — e que também envelheceu na própria rodada: a frase desta linha, ao citar *"por enquanto"*, virou ocorrência, e o número real da W-F é **39**; a E-2 mediu **39**, com a mesma contagem por arquivo — mais duas da ADR-063 (que cita a frase vencida), uma da ADR-066 e três deste próprio razão, que cita as fórmulas; menos a da `RegraDoCatalogoViolada`, cuja frase vencida saiu na E-3b. Nenhuma é sobre a publicação. **Até a W-G o comando não excluía este arquivo**, e os números acima o contam. Na R-2, sobre `e43d227`: **39** sem a exclusão, **32** com ela — sete eram este razão. Das 32, três já tinham linha (43, 44 e 63); vinte foram triadas, e **as vinte eram peça, nenhuma estado de dado**: seis venceram, onze não, três não dá para dizer. Doze delas viraram as linhas 79 a 86. **Nove ficaram por triar**, pelo teto de vinte: `estabelecimento.md`:441 e :467, `premissas-do-front.md`:140, `mensagem-na-fila-morta.md`:124, `reconciliacao-de-pagamento.md`:161, `recuperacao-de-acesso.md`:135, `ConfiguracaoDaEscuta.java`:23, `ExpedienteCorrenteResponse.java`:15 e `Equipe.java`:313. **Na R-3 as nove foram triadas**, e a segunda passada passou a **31**: saíram `catalogo.md`:279 e `estabelecimento.md`:441, frases falsas trocadas no lugar, e entrou a ADR-038:133, que cita na emenda a frase do corpo. O que a segunda passada
achar entra na tabela **com a fórmula**, não com a paráfrase.

**O que a segunda passada acha, e o que disso é dívida.** O comando colhe de propósito mais do
que precisa, e colhe **duas espécies diferentes**: *condição que espera* e *frase que já está
errada*. A triagem separa, nesta ordem:

1. **A condição já venceu e a frase ainda a afirma?** → **não é dívida: é frase falsa.** Ela
   não espera nada; está errada agora. **Conserta-se o texto** — no lugar, se for descrição;
   por emenda datada, se for ADR. *Vem ao razão como linha paga, para a história; mas a entrega
   é o texto consertado, e a linha sozinha não basta. Aprendido na R-3.*
2. **O sujeito da frase é uma peça do sistema** — uma rota, uma porta, um consumidor, um
   índice: **é gatilho.** *"O catálogo ainda não tem rota."*
3. **O sujeito é o estado de um dado:** **não é dívida, e nunca será.** *"Esta loja ainda não
   tem produto."* A loja pode ficar vazia para sempre, e isso não é débito de ninguém.
4. **A frase descreve uma consequência, não uma pendência** — ela explica o efeito de uma
   decisão e não pede nada: **fica fora.** *Foi o ramo que o uso achou, e o que mais cortou.*
5. **Não dá para dizer qual dos quatro:** fica **fora**, e volta quando alguém souber. Linha
   que entra sem se saber o que a dispara é ruído dentro do instrumento que existe para não ter
   ruído.

Os ramos 3, 4 e 5 vão à seção *"Da segunda passada, e deliberadamente fora do razão"*, no fim
deste arquivo, com `arquivo:linha` — não só ao relatório da rodada.

**Os tetos, e o que cada um limita.** A **triagem** para em vinte linhas por rodada: lista
longa demais vira carimbo. A **correção pelo ramo 1 não tem teto quando a medição já está
feita** — *deixar de pé uma frase que se sabe falsa, por economia de rodada, é o quadragésimo
sétimo defeito; e arquivá-la como dívida para depois é o quinquagésimo primeiro.*

Triado na R-2 e na R-3.

**Os comandos produzem candidatos; a tabela é a leitura deles.** Uma menção a
"gatilho" pode ser a definição de um, a citação de um que já está na tabela, ou o
relato de um que disparou. A conta, em 17/10/2026, na R-4:

| | linhas |
| --- | --- |
| o recorte estreito devolve | **87** |
| — cópias em `frontend/.tsbuild/`, saída de build que o filtro não exclui | −3 |
| — citação de um gatilho que já tem linha própria (ADR-045:17, ADR-054:15, ADR-055:12, ADR-059:21, `pedido.md`:397; e, na W-E, `PainelPage.tsx`:28 e as linhas 8, 18, 20 e 142 da ADR-061; e, na E-3b, `RegraDoCatalogoViolada`:25, que cita a linha 55; e, na E-3c, ADR-065:138, que relata o disparo da linha 56; e, na E-2, `estabelecimento.md`:234, que cita a linha 71) | −13 |
| — o mesmo gatilho repetido em outro lugar, ou substituído por outro (ADR-045:123, que a ADR-054 substituiu; ADR-043:210, ADR-057:101, ADR-058:91, `como-subir-local.md`:480, `ProdutoResponse`:30, `MarcarDisponibilidadeService`:64, `ContratoDeErrosIT`:73 e :105, `ReativacaoNoExpedienteIT`:58; e, na E-3d, ADR-068:57, a §1 repetindo o primeiro gatilho da seção; e, na W-F, ADR-067:95, a §4 repetindo o primeiro da seção) | −12 |
| — menção que nega um gatilho, e não o escreve (ADR-064:26, *"não havia gatilho escrito"*; e, na R-3, a ADR-012, emenda de 16/10 (2), *"Gatilho escrito: nenhum"*; e, na R-4, a ADR-071, emenda de 17/10, e a ADR-072, seção de gatilhos, *"Nenhum."*) | −4 |
| — menção ao título da seção, que fala dela e não escreve gatilho (R-4: ADR-072:13, :16, :27, :42, :65, :81 e :86; ADR-071:214, o título da emenda de 17/10; e o `README.md` das decisões :115 e :118). *A decisão sobre o título acrescentou dez linhas ao comando que lê o título* | −10 |
| **= linhas da tabela vindas do recorte estreito** | **45** |
| + entraram por outra redação — *"com gatilho:"* (linhas 1 e 2), *"Gatilho para trazê-lo de volta"* (4), *"Quando o segundo chegar"* (6), *"O gatilho para mudar"* (11), *"O gatilho para reabrir"* (19), *"O gatilho, escrito"* (22), *"Gatilho:"* (40 e 42), *"O gatilho para a função mudar de lugar"* (41), *"volta quando houver"* (43), *"Decidir antes do marco 5"* (44), *"Gatilho novo, e são dois"* (20 e 48); e, na R-3, *"Isso é linha de razão, com gatilho:"* (93) | **+15** |
| + segundos gatilhos dentro de uma seção "## Gatilho escrito", que o comando conta uma vez só, pela linha do título (ADR-057, ADR-058, ADR-059 — linhas 45 a 47; ADR-060 — linhas 50 e 51; ADR-061 — linha 54; ADR-063 — linha 56; ADR-064 — linhas 58, a outra metade do primeiro, 59 e 60; ADR-065 — linha 62; ADR-066 — linhas 63 e 65; ADR-068 — linhas 68 e 69; ADR-069 — linhas 74 e 75; ADR-070 — linha 77; ADR-071 — linhas 95 e 96) | **+20** |
| + escrito neste razão, porque a condição não tem outro lugar onde morar (linhas 52, 72 e 78) | **+3** |
| + entraram pela segunda passada, triada na R-2 — doze linhas em oito: ADR-024:140 (79), ADR-063:29 e :30 (80), ADR-067:23 (81), `catalogo.md`:279 (82), ADR-011:114 e ADR-048:247 (83), ADR-013:232 e ADR-031:173 (84), ADR-021:76 e `conversa.md`:291 (85), ADR-045:142 (86) | **+8** |
| + entraram pela segunda passada, triada na R-3 — `estabelecimento.md`:441 (88), `mensagem-na-fila-morta.md`:124 (89), `reconciliacao-de-pagamento.md`:161 (90), `recuperacao-de-acesso.md`:135 (91), `Equipe.java`:313 (92) | **+5** |
| + entrou pela segunda passada, reclassificada na R-4 — `mensagem-na-fila-morta.md`:124, que a R-3 tinha dado como a condição da 43 (97) | **+1** |
| − item que uma emenda declarou não ser gatilho — a 96, estornada pela emenda de 17/10/2026 à ADR-071 | **−1** |
| **= linhas da tabela que contam** | **96** |

**A tabela tem 97 linhas numeradas:** as 96 que contam e a 96, que fica estornada e não conta. O razão não apaga linha (ADR-072 §3).

**A coluna "disparou?" tem quatro respostas.** *sim* — disparou e a dívida foi paga;
*não* — e a coluna seguinte diz o que se procurou; **disparou e não produziu nada** —
a condição chegou e ninguém decidiu o que ela mandava decidir; e *rearmado* —
disparou, foi examinado, e a dívida continua com outro gatilho. **A terceira é a que
este arquivo existe para achar.**

---

## Disparou e não produziu nada

| # | Onde | O gatilho, copiado | O que mostra que disparou | O que o fecha |
| --- | --- | --- | --- | --- |
| 1 | ADR-044:97 (e `gateway/config/JwtProperties.java`:12) | *"Fica registrado como duplicação conhecida, com gatilho: o terceiro módulo que precisar do mesmo decoder."* | o `catalog` montou o terceiro decoder com validador de `iss`/`aud` em 28/09 (`e1e78fa`); hoje são quatro: `grep -rln "JwtIssuerValidator\|audience" backend --include=SecurityConfig.java` devolve `gateway`, `catalog`, `identity`, `merchant`. Nenhuma ADR posterior volta ao assunto | uma decisão: módulo comum de segurança (emenda à ADR-001) ou a duplicação assumida sem gatilho |
| 2 | `gateway/src/test/.../IdentityDeMentira.java`:36 | *"Fica registrado na ADR-044 como duplicação conhecida, com gatilho: o terceiro módulo que precisar do mesmo dublê."* | há três cópias: `gateway`, `merchant` e `catalog` têm `support/IdentityDeMentira.java`. A ADR-044 **não** registra esta duplicação — `grep -n "dublê\|IdentityDeMentira" docs/architecture/decisions/ADR-044-*.md` volta vazio | a mesma decisão da linha 1, para teste |
| 4 | `frontend/README.md`:32 | *"**Gatilho para trazê-lo de volta:** a primeira escrita que precise invalidar leitura de **outra** tela."* | **leitura, não medida:** na W-D, marcar uma opção na tela do produto muda o `vendavel` que a lista mostra; a lista é relida por remontagem ao voltar (`ListaDeProdutos.tsx`, `aoVoltar`). Se isso é "invalidar leitura de outra tela", disparou e foi resolvido sem o TanStack Query nem decisão escrita | decidir se a remontagem é a resposta, e escrevê-lo |
| 52 | este razão (W-E) | *"a primeira tela que mostre um desses três ao comerciante"* — o `TratadorDeErros` do `catalog` repassava `excecao.getMessage()` no `detail` em três tratadores: `ProdutoNaoEncontrado` (404), `LojaSemExpediente` (409), `RegraDoCatalogoViolada` (400), contra a regra do `CLAUDE.md` escrita na W-E | **nasceu disparado** (W-C) e **pago em um terço na E-3b**: o `RegraDoCatalogoViolada` passou a carregar motivo com dados, o tratador escreve a frase, e os pontos de integridade saem **500** (ADR-063). **Continuam** o `ProdutoNaoEncontrado` (404) e o `LojaSemExpediente` (409). O censo, em 11/10/2026: `grep -rn "getMessage()" backend/services/*/src/main/java/com/deliveryplatform/*/api/error/TratadorDeErros.java` devolve as linhas 91 e 101 do `catalog` fora de `log.warn` | a frase escrita por tratador nos dois que faltam |
| 97 | `mensagem-na-fila-morta.md`:124 (e ADR-026 §7) | *"**A métrica de profundidade ainda não existe.** … Requisito do marco 3, junto do primeiro consumidor"* | o primeiro consumidor com fila morta chegou em 10/10/2026, no marco 2: `ConfiguracaoDoConsumoDeExpediente` (`7b1c888`), *"a primeira fila durável e a primeira fila morta deste repositório"*. A métrica não veio, e não pode vir como a ADR-026 §7 a decidiu — profundidade exposta ao Prometheus, alerta em zero —, porque a ADR-041 tirou o Prometheus por decisão e aceitou *"o actuator fica sem métrica nenhuma"*, sem citar a ADR-026. Hoje a fila morta só se vê pela interface de management, à mão | uma decisão: quem olha a fila morta enquanto a ADR-041 vale, e a ADR-026 §7 emendada com ela. **Não é a linha 43:** aquela espera o stack voltar; esta já disparou |

## Rearmado

| # | Onde | O gatilho, copiado | O que mostra que disparou | O que o fecha |
| --- | --- | --- | --- | --- |
| 5 | ADR-048:262 → ADR-057:101 | *"o primeiro consumidor que **escreva em banco** — hoje previsto para o `order`, no marco 3."* Rearmado como: *"**O primeiro consumidor cujo efeito não seja uma comparação** — um que acrescente, some, registre ou publique para fora."* | o `OuvinteDeExpedienteAlterado` (G-C3b, `7b1c888`) escreve em banco e foi coberto pela exceção (b) da invariante 7. `grep -rn "@RabbitListener" backend --include=*.java` devolve dois ouvintes, os dois nas exceções | o `processed_messages`, quando o primeiro consumidor que some ou registre chegar — previsto para o `order` |
| 6 | ADR-037:210 → ADR-044:97 | *"**Não se decide agora porque só existe um serviço validando de verdade.** Quando o segundo chegar, a pergunta chega junto, com evidência."* | o segundo chegou (o `merchant`, `a5994c0`, 15/09/2026) e a pergunta foi respondida com adiamento e um gatilho novo, o da linha 1 — que disparou sem resposta | a linha 1. A ADR-038:124 afirmava o mesmo — *"Ele ainda não existe."* — até a emenda de 16/10/2026 (R-3) |

## Sim — disparou, e a dívida foi paga

| # | Onde | O gatilho, copiado | O que mostra | Pago por |
| --- | --- | --- | --- | --- |
| 7 | ADR-026:226 | *"o primeiro consumidor que escreva em banco. É ele que precisa desta política de verdade"* | `ConfiguracaoDoConsumoDeExpediente` com a política 1/4/16 s (G-C3b, `7b1c888`) | emenda de 10/10 à ADR-026 |
| 8 | ADR-048:117 | *"o primeiro consumidor que escreva em banco — ele vai precisar da política da ADR-026 de verdade"* | a mesma; a ADR-048 registra *"Disparou em 10/10/2026 (G-C3b)"* | o mesmo; o bloco `retry` do YAML do `catalog` saiu na I-B |
| 9 | ADR-043:193 e :210 | *"o primeiro serviço que precisar autorizar uma requisição própria contra um vínculo do `merchant`"* | o `catalog` (G-B3, `e1e78fa`) | ADR-045 |
| 10 | ADR-053:123 (e `ContratoDeErrosIT`:73) | *"a primeira tela que mostre `detail` a alguém — a W-C."* | a W-C | ADR-055 |
| 11 | ADR-040:212 (e `Permissao.java`:9) | *"O gatilho para mudar é o segundo serviço que precise nomear uma permissão"* | o `catalog` passou a nomear `VER_PRODUTO` (G-B3) | respondido com "não" — `PermissaoDoCatalogo`, recorte próprio |
| 12 | `frontend/src/api/useRecurso.ts`:47 (o da W-A) | o gatilho da W-A para o TanStack Query, *"para esta rodada decidir"* | a W-B | decidido: saiu (`main.tsx`:9). O de volta é a linha 4 |
| 3 | `frontend/src/painel/MenuDoPainel.tsx:34` (hoje citado na linha 32 do mesmo arquivo) | *"**Gatilho escrito:** a segunda seção com tela. Aí a URL passa a precisar dizer onde a pessoa está — para recarregar, para voltar e para mandar link a alguém —, e isso é rota, não estado."* | venceu na **W-C**; a **W-D** o agravou pondo a terceira tela pelo mesmo precedente, sem o reler; a **R-1** o nomeou; pago na **W-E**: `App.tsx` tem `/painel/lojas/:estabelecimentoId/{produtos,disponibilidade}[/:produtoId]`, e o estado que escolhia a seção saiu do `PainelPage` | ADR-061 — três rodadas entre vencer e ser lido |
| 13 | ADR-044:157 | *"sai da lista de exposição antes de qualquer ambiente exposto, junto com a decisão do limite de taxa."* | **pago antes de disparar**: o `/actuator/gateway` saiu na G-B5; `include: health,info` no `application.yml` do gateway | emenda de 30/09 à ADR-044 |
| 56 | ADR-063, seção "Gatilho escrito" | *"**E a primeira rota de escrita do `catalog`** — a E-3c. Ela vai gerar corpos malformados de verdade, e aí se mede quais dos trinta pontos passaram a ser alcançáveis."* | a E-3c: `POST .../catalog/produtos` monta o `Produto` a partir de um corpo. Medido: `categoriaId`, `precoBase` e `modoDeControle` ausentes chegavam ao construtor como nulo (`INTEGRIDADE`, 500), e `ordem` ausente estourava no desempacotamento | os quatro `@NotNull` do `ProdutoRequest`, com o `CriacaoDeProdutoIT` afirmando 400 para cada campo ausente (E-3c) |
| 79 | ADR-024:140 | *"**Pendência nomeada, sem prazo** — vale quando houver comerciante real reclamando, não antes."* (o pedido mínimo) | **pago antes de disparar**: a ADR-028 fecha a consequência que a ADR-024 nomeou, e a `Operacao` do `merchant` tem `pedidoMinimoPorModalidade` | ADR-028; a ADR-024 ganhou emenda datada na R-3, porque o corpo continuava afirmando a pendência |
| 80 | ADR-063:29 e :30, citando o javadoc antigo da `RegraDoCatalogoViolada` | *"Uma exceção só para toda a rodada, de propósito: **nesta altura o catálogo não tem rota**, e quem traduz regra violada em código HTTP é a camada que ainda não existe."* | o tradutor existe desde a G-E; a frase saiu do javadoc na E-3b. É o gatilho em prosa que fez nascer a segunda passada | ADR-063 (E-3b, `9fbcee1`) |
| 81 | ADR-067:23 | *"**A seção, por enquanto, não significa nada para quem olha.**"* | a W-F deu tela à seção: `CardapioPorSecao` (`ec89666`) | ADR-067 (W-F) |
| 82 | `catalogo.md`:279 | *"**Quem o chama ainda não existe** — o consumidor do evento é a G-C3b; hoje o único chamador é o teste de integração."* | o `OuvinteDeExpedienteAlterado` chama `ReativarNoExpediente` desde a G-C3b (`7b1c888`). A frase de origem dizia o contrário até a R-3, que a trocou no lugar | a G-C3b; a frase, na R-3 |
| 87 | ADR-012, emenda de 16/10/2026 | *"**Gatilho escrito:** a próxima rota que o gateway ganhar. Quem a acrescenta acrescenta a linha aqui — ou tira a tabela, e deixa o `application.yml` ser a única lista."* | **pago antes de disparar**, no mesmo dia: a tabela saiu de normativa, e o `RoteamentoIT` compara os ids dos casos com as rotas que o gateway carregou (R-3, `0e545c0`) | ADR-071; ADR-012, emenda de 16/10 (2) |
| 88 | `estabelecimento.md`:441 | *"**A porta pela qual os outros serviços perguntam ainda não existe**, e é deliberado: nenhum dos seis tem código"* | `GET /internal/merchants/{estabelecimentoId}/me/contexto-de-acesso` (`100f71f`, 28/09/2026), consumida pelo `AutorizacaoComercialHttp` do `catalog` (`e1e78fa`, 28/09/2026). **Frase falsa, ramo 1** | `100f71f` e `e1e78fa`; a frase trocada no lugar na R-3 |
| 89 | `mensagem-na-fila-morta.md`:124 (a segunda frase do item) | *"o `prometheus.yml` só aponta para o `/actuator` dos serviços"* | o `prometheus.yml` não está na árvore: saiu com o stack de observabilidade (ADR-041). **Frase falsa, ramo 1.** A primeira frase do mesmo item — a métrica de profundidade — tem linha própria desde a R-4, a 97 | a ADR-041; a frase trocada no lugar na R-3 |

## Não

| # | Onde | O gatilho, copiado | O que se procurou | O que o fecha |
| --- | --- | --- | --- | --- |
| 14 | ADR-001:227 | *"o dia em que houver uma segunda regra de build."* | `grep -rn "tasks.register" backend/build-logic` — uma só, `verificarDependenciaEntreModulos` | suíte própria do `build-logic` |
| 15 | ADR-009:299 | *"quando alguém precisar ordenar o cardápio por preço no banco"* | nenhuma ordenação por `precoBase` no `catalog` | `changeUnit` para `Decimal128` |
| 16 | ADR-012:95 | *"**Gatilho escrito, para os dois:** o primeiro ambiente exposto à internet."* (limite de taxa e `correlationId`) | nenhum workflow implanta: `grep -lni "deploy\|environment:" .github/workflows/*` vazio | as duas peças, com ADR |
| 17 | ADR-044:139 | *"o primeiro ambiente exposto à internet."* (limite de taxa) | o mesmo da 16 | a mesma |
| 18 | ADR-047:123 | *"o primeiro ambiente alcançável de fora da máquina de desenvolvimento. Aí a CSP entra com ADR própria"* | o mesmo da 16 | CSP |
| 19 | ADR-044:226 | *"**O gatilho para reabrir:** no dia em que houver rota demais para conferir de cabeça."* (o `/actuator/gateway`) | é julgamento, não fato; a contagem de rotas do gateway não foi medida | — |
| 20 | ADR-054:48 (substitui o da ADR-045:123) | *"**a primeira chamada de serviço para serviço em que o serviço chamado precise saber quem chamou** para decidir o que responder."* | as duas chamadas que existem (`catalog` → `merchant`, `AutorizacaoComercialHttp` e `ExpedienteCorrenteHttp`) mandam o token do portador, e o `merchant` responde sobre ele | identidade de serviço |
| 21 | ADR-046:179 | *"o dia em que uma passada da varredura levar mais que o intervalo entre passadas."* | **não há medida** da duração de uma passada; o log só conta aberturas publicadas. Não disparou até onde se vê, e não se vê muito | uma consulta restrita, já desenhada |
| 22 | ADR-046:284 (e `pedido.md`:397) | *"**O gatilho, escrito:** quando o `order` ganhar código — marco 3 —"* (emitir fechamento, pausa e retomada) | `order-service` tem 1 arquivo Java, a `*Application` | decisão (a) ou (b) da ADR-046 |
| 23 | ADR-049:150 | *"se algum dia o horário deixar de ser semanal (feriado, exceção por data), esta busca muda junto."* | `Disponibilidade` continua `Map<DayOfWeek, List<Faixa>>` | a busca da próxima abertura |
| 24 | ADR-050:42 | *"quem abrir o `SecurityConfig` do `identity` para qualquer outra coisa acrescenta a propriedade na mesma passada."* | nenhum commit no `SecurityConfig` do `identity` desde a ADR-050 (30/09) | a documentação aberta no `identity` |
| 25 | ADR-050:128 | *"o dia em que a cadeia de filtros virar módulo comum"* | não virou | — |
| 26 | ADR-051:128 | *"O dia em que as imagens tiverem de ser construídas num lugar que **não tem o build do Gradle disponível**"* | nenhum workflow constrói imagem | a imagem construtora |
| 27 | ADR-052:101 | *"a segunda coleção com mais de um escritor concorrente."* | um `@Document` só, `ProdutoDocumento` | `@Version` na segunda |
| 28 | ADR-052:104 (e `ContratoDeErrosIT`:105, `ReativacaoNoExpedienteIT`:58) | *"o primeiro relato de 409 que ninguém consegue explicar."* | nada no repositório registra relato de uso | teste HTTP → 409 |
| 29 | ADR-053:151 | *"O primeiro serviço **sem** rota autenticada, ou com um esquema de erro diferente — o `conversation`"* | `conversation-service` tem 1 arquivo Java | o contrato de erro do webhook |
| 30 | ADR-055:56 | *"a primeira tela que precise de **outro** campo do `ProblemDetail` além do `detail`"* | nenhum `.type` lido em `frontend/src` | o esquema de erro no contrato |
| 31 | ADR-055:72 | *"a primeira que precisar — e aí o servidor ganha um discriminador legível por máquina, que é o campo `type`"* | o mesmo da 30 | o `type` nos tratadores |
| 32 | ADR-056:119 | *"A primeira rota cujo identificador na URL **não** pertença a uma loja"* | todas as rotas com id estão sob `/merchants/{estabelecimentoId}` | a regra para recurso global |
| 33 | ADR-058:114 (e :91) | *"**A primeira rodada que precise de um dos cinco esqueletos de pé.**"* | as rodadas I-B a W-D subiram só o `marco2` | `mem_limit` e `on-failure:3` nos cinco |
| 34 | ADR-059:138 (e `como-subir-local.md`:480) | *"a rodada que der ao `merchant` as rotas de escrita troca esta fixture por uma sequência de chamadas HTTP, e **a semeadura por `ApplicationRunner` sai do repositório nessa rodada**."* | o `merchant` não tem `@PostMapping`, `@PutMapping` nem `@PatchMapping` | as rotas de escrita, e os três semeadores saem |
| 35 | `catalogo.md`:192 (e `MarcarDisponibilidadeService`:64) | *"o `conversation-service` ganhar código, que é quem o consome."* (o `DisponibilidadeAlteradaV1`) | 1 arquivo Java no `conversation` | outbox no `catalog` e o evento |
| 36 | `catalogo.md`:243 (e `ProdutoResponse`:30) | *"a primeira tela que mostre "a partir de"."* | nenhum "a partir de" em `frontend/src` | `precoMinimoPossivel` na resposta |
| 37 | `catalogo.md`:44 (e `ModoDeControle`:32) | *"o valor nasce junto com a primeira baixa de estoque, no marco 10."* | o enum tem `SEM_CONTROLE` e `QUALITATIVO` | marco 10 |
| 38 | `ProdutoDocumento`:40 | *"quando alguém precisar consultar `marcadoEm` por intervalo"* | nenhuma consulta por `marcadoEm` no repositório Mongo | `Date` com truncamento explícito |
| 39 | `ProdutoRepositorioMongo`:129 | *"a primeira loja com mais de mil produtos, ou a primeira varredura que apareça em log de consulta lenta"* | a única loja tem um produto | o índice `002` |
| 40 | ADR-021:221 | *"**Gatilho:** o starter volta com o primeiro leitor do cardápio em cache, no marco 7"* | o build do `catalog` cita Redis só em comentário | marco 7 |
| 41 | `DiaOperacional.java`:34 | *"O gatilho para a função mudar de lugar continua sendo o segundo serviço que precise **calcular**"* | `DiaOperacional` não aparece fora do `merchant` | o `settlement` ou o `order` |
| 42 | `CLAUDE.md`:413 | *"**Gatilho:** o primeiro `bootRun` de `catalog` ou `conversation` que alguém precise fazer"* | nada registra um; as rodadas I-B a W-D usaram contêiner | medir o `bootRun` sem `?replicaSet` |
| 43 | ADR-041:300 | *"Observabilidade volta quando houver **um comerciante real usando o sistema**"* | não há | o stack de observabilidade |
| 44 | `usuario.md`:177 | *"**Decidir antes do marco 5**, quando houver dinheiro na conta."* (número de telefone reciclado) | é prazo, não condição; o marco 5 não começou | a política |
| 45 | ADR-057, seção "Gatilho escrito" | *"**E um segundo, menor:** a primeira mensagem cuja chave de domínio **não venha no corpo** e precise ser buscada."* | o `ExpedienteAlteradoV1` traz o `expedienteDeReferencia` no corpo; é o único consumido com a exceção (b) | o `processed_messages` para esse consumidor |
| 46 | ADR-058, seção "Gatilho escrito" | *"**E um segundo:** o primeiro serviço cuja ocupação passar de 80% dos 512 MiB."* | `docker stats` em 10/10/2026, com a pilha de pé: o maior é o `merchant`, 302,2 MiB — 59% | reescolher o teto |
| 47 | ADR-059, seção "Gatilho escrito" | *"**E um segundo, que mede o andaime:** a primeira vez que alguém quiser semear algo que o agregado não deixa construir."* | os três semeadores constroem pelo agregado; o `identity` usa `reconstituir` para fixar o id, e o agregado aceita | descobrir por que o domínio recusa |
| 48 | ADR-054:51 | *"**a primeira rota `/internal/` que não receba o token de um portador.**"* | as duas rotas `/internal/` do `merchant` caem no `anyRequest().authenticated()` do `SecurityConfig` dele | identidade de serviço |
| 49 | ADR-060, seção "Gatilho escrito" | *"**A primeira rota que crie um recurso cujo dono ainda não exista** — hoje só esta."* | só o `POST /api/v1/me/estabelecimentos` cria sem confrontar vínculo | a regra *"escrita sem vínculo só quando o vínculo é o produto"* vira linha do `CLAUDE.md` |
| 50 | ADR-060, seção "Gatilho escrito" | *"**O dia em que o `merchant` precisar de um GET de uma loja por id.**"* | não há `GET /api/v1/me/estabelecimentos/{id}` nem outro GET de loja por id; o predicado do gateway para a coleção é exato (G-B5) — e por isso a criação responde 201 **sem `Location`** | o predicado prefixado e o `Location`, com ADR própria |
| 51 | ADR-060, seção "Gatilho escrito" | *"**O dia em que uma pessoa física precisar ser impedida de criar lojas sem limite.**"* | não há limite nem unicidade de documento, e ninguém pediu | decisão de produto |
| 53 | ADR-061, seção "Gatilho escrito" | *"**A primeira tela do painel que não pertença a uma loja** — relatório que cruze duas lojas, conta da pessoa, preferências."* | toda rota sob `/painel` em `App.tsx` é o seletor, o cadastro ou `lojas/:estabelecimentoId/…` | um nível acima de `/painel/lojas/:id` |
| 55 | ADR-063, seção "Gatilho escrito" | *"**O dia em que alguém for ler o `catalog` e se confundir com uma `RegraDoCatalogoViolada` que é 500.**"* — a classe virou duas espécies com um tipo só, separadas por motivo, e **o nome dela mente para metade** | é julgamento, não fato medível; nenhum relato | a classe partida em duas, com nomes honestos |
| 57 | ADR-064, seção "Gatilho escrito" | *"**A primeira tela que renomeie, reordene, ative ou desative uma seção.** Aí as três sem evento viram rotas — e **`reordenar` arrasta o outbox do `catalog` junto**."* — o `catalog` **não tem outbox**, e o `CategoriasReordenadasV1` está declarado com o `conversation` como consumidor; enquanto não houver, **reordenar categoria não vira rota** (invariante 7) | a tela do cardápio (W-F) cria e lista seções, e não renomeia, reordena, ativa nem desativa nenhuma; na E-3b, nenhuma tela tocava em seção. Medido na E-3a, decidido na E-3b | o outbox do `catalog`, e a rota |
| 58 | ADR-064, seção "Gatilho escrito" (o mesmo gatilho, a outra metade) | `renomear`, `ativar` e `desativar` categoria existem no agregado e não têm rota. *"Entram com a tela, e o gatilho está escrito."* | o mesmo da 57 | as três rotas, sem evento |
| 59 | ADR-064, seção "Gatilho escrito" | *"**O dia em que alguém quiser separar quem cria produto de quem mexe nas seções.**"* | criar categoria exige `CRIAR_PRODUTO`; ninguém pediu a separação | `GERENCIAR_CARDAPIO`, com o pedido na mão |
| 60 | ADR-064, seção "Gatilho escrito" | *"**E o dia em que uma loja tiver mais seções do que cabem numa resposta.**"* | não há medida de quantas seções uma loja tem; a fixture tem uma | a listagem paginada |
| 61 | ADR-065, seção "Gatilho escrito" | *"**A primeira tela que monte um produto com grupos** — é a E-3d, e ela arrasta o método novo na raiz para acrescentar opção."* — `acrescentarGrupo` tem método na raiz e não tem rota; **acrescentar opção não tem método**, e o `GrupoDeOpcoes` é *record* imutável | o corpo do `POST .../produtos` não carrega grupos (E-3c). Medido na E-3a, decidido na E-3c | a primeira tela que monte um produto com grupos |
| 62 | ADR-065, seção "Gatilho escrito" | *"**O dia em que alguém pedir para despublicar pela tela.** O agregado tem o caminho; a rota não nasce sem chamador."* | a tela do cardápio (W-F) publica e não despublica: o publicado mostra o estado e não tem botão de volta (ADR-067 §6) | a rota de despublicar |
| 63 | ADR-066, seção "Gatilho escrito" | *"**E o dia em que alguém apagar uma categoria que tem produtos.**"* — não há rota de apagar categoria, e quando houver, a regra da ADR-066 fica do lado errado do tempo: o produto passaria a apontar para coisa que existiu | não há rota nem tela que apague seção | a decisão do que acontece com os produtos da seção |
| 64 | ADR-066, seção "Gatilho escrito" | *"**A primeira outra referência de corpo que atravesse raízes** — o `entregadorId` de um pedido, o `estabelecimentoId` de uma conversa."* | a `categoriaId` do `ProdutoRequest` é a única (E-3c) | a ADR-066 vira regra da casa, com outro nome |
| 65 | ADR-066, seção "Gatilho escrito" | *"**O dia em que o front precisar distinguir um campo inválido de outro na mesma resposta.**"* | o formulário do produto (W-F) mostra o `detail` inteiro num aviso, sem destacar campo — e ninguém pediu o destaque | um `campo` no corpo de erro, e a ADR-055 emendada |
| 66 | `PublicarProdutoService`:30 | *"**Gatilho escrito:** o `conversation` ganhar código, que é quem o consome."* — o `catalogo.md` §8 declara `ProdutoPublicadoV1` na transição `RASCUNHO → ATIVO`, e a rota de publicar (E-3c) não o emite: o `catalog` não tem outbox. A mesma decisão da linha 35, para o outro evento | `conversation-service` sem código de domínio | o outbox do `catalog` e o evento |
| 67 | ADR-068, seção "Gatilho escrito" (e §1) | *"**O primeiro chamador que precise de um subconjunto por estado.** Aí nasce o parâmetro, com ele."* — a listagem do painel devolve **todos os estados** e **não tem filtro** (ADR-068 §1): parâmetro sem chamador é superfície, e quem precisaria de *"só os publicados"* entra pela porta da ADR-018 | o único chamador da listagem é o `ListaDeProdutos.tsx` do painel; o `SemeaduraDoCatalogo` a usa só como teste de idempotência (E-3d) | o parâmetro, com o chamador |
| 68 | ADR-068, seção "Gatilho escrito" | *"**O dia em que a lista do painel não couber na tela.** A rota pagina desde a G-B3; a tela não"* | a tela mostra a primeira página, de 20, e avisa | a paginação da tela |
| 69 | ADR-068, seção "Gatilho escrito" | *"**E o dia em que o cardápio de quem compra existir.** Ele entra pela ADR-018, e se alguém o fizer passar por esta rota, esta ADR é a que está errada"* | `POST /internal/catalog/quote` não existe em código (E-3d) | a porta da ADR-018 |
| 70 | ADR-067, seção "Gatilho escrito" (e §4) | *"**O segundo cliente da rota de criar produto.** Aí o `ordem` derivado vira regra duplicada em dois lugares, e o conserto é no contrato."* — o `ProdutoRequest` exige `ordem` com `@NotNull`, e **nenhum comerciante tem opinião sobre "ordem 0"**. A tela calcula o maior da seção mais um e não mostra o campo; **a regra "o produto entra no fim da seção" é de domínio e está vivendo no cliente** | o único cliente é o `NovoProduto.tsx` do painel (W-F); a seção nova tem a mesma conta, no `CardapioPorSecao.tsx` | `ordem` opcional no contrato, e o fim da seção decidido no servidor |
| 71 | ADR-069 §2 (e `estabelecimento.md`:234) | *"**Gatilho escrito:** o dia em que a rota do usuário do token existir. Aí a página diz *'a Marli te convidou'*, e **só aí**."* — a página pública do convite **não diz quem convidou**: o `merchant` guarda o vínculo de quem convidou, o nome mora no `identity`, e a rota *"o usuário do token"* não existe | a rota não existe no `identity` (`grep @RequestMapping` no `identity-service`: `/api/v1/auth` e o JWKS); na tabela do `premissas-do-front.md` desde a G-B5, sem rodada | a página diz quem convidou |
| 72 | este razão (E-2) | *"o primeiro administrador que peça para cancelar um convite"* — `Equipe.cancelarConvite` existe desde a B2, confere A1 e só cancela o `PENDENTE`, e **não tem rota**: criar outro convite não cancela o primeiro | nenhuma rota de cancelar no `merchant` (E-2) | a rota de cancelar convite |
| 73 | ADR-069, seção "Gatilho escrito" | *"**A primeira tela que peça para promover, remover, suspender ou sair.** Aí as cinco viram rotas, e o `GerenciarEquipeService` já as tem."* — medido na E-2, são **sete** as operações do `GerenciarEquipeService` sem borda: promover, rebaixar, suspender, reativar, remover, sair e alterar permissões | a única escrita da equipe com rota é convidar, e o aceite (E-2) | as rotas, com a tela que as peça |
| 74 | ADR-069, seção "Gatilho escrito" | *"**O dia em que alguém perder o link e reclamar.** Aí se decide entre revogar-e-recriar e mostrar o token na lista, **com o caso real na mão**"* | não há tela de equipe | a decisão, com o caso |
| 75 | ADR-069, seção "Gatilho escrito" | *"**E o dia em que houver `CanalPort`.** O transporte deixa de ser humano, e esta ADR vira sobre o canal"* | `CanalPort` não existe em código | o convite enviado pelo canal |
| 76 | ADR-070, seção "Gatilho escrito" | *"**O primeiro administrador que perguntar quem está pendente.** Aí nasce a listagem de convites — rota e tela."* — **não há lista de convites pendentes**: quem convida copia o link na hora, e depois não sabe quem está pendente nem reencontra o link (ADR-069 §1, ADR-070 §1). A tela diz isso a quem convida | não há rota de listar convites no `merchant` (E-2), nem tela (W-G) | a listagem de convites, rota e tela |
| 77 | ADR-070, seção "Gatilho escrito" | *"**O dia em que existir a rota do usuário do token.** Aí a lista de membros tem o que escrever"* — **não há lista de membros na tela de equipe**, porque não há nome a mostrar. A outra metade do mesmo gatilho é a linha 71, a página do convite que não diz quem convidou | o `GET .../team` existe desde a C-A e devolve `usuarioId`; a rota do nome não existe | a lista de membros, com nome |
| 78 | este razão (W-G; achado na E-2) | *"o primeiro log de acesso, no gateway ou no `merchant`"* — **o token do convite vai na URL** do `GET` público e do aceite, como a ADR-069 desenhou; hoje nenhum dos dois serviços grava log de acesso, e se um dia gravar, grava o token. No front ele fica também no `state.de` do roteador, na mesma entrada do histórico que já tem a URL | nenhuma configuração de log de acesso no `merchant` nem no `gateway` (E-2) | o caminho mascarado no log, ou o token fora do caminho |
| 54 | ADR-061, seção "Gatilho escrito" | *"**O dia em que o seletor tiver mais de uma dúzia de lojas.**"* | não há medida de quantas lojas alguém tem; a fixture e a prova da W-E têm uma cada | busca no servidor, e paginação no `GET /api/v1/me/estabelecimentos` |
| 90 | `reconciliacao-de-pagamento.md`:161 | *"**Se a reconciliação vira automática** quando houver volume. Hoje é console e leitura"* | o `payment-service` tem 1 arquivo Java; não há pagamento a reconciliar | a reconciliação automática |
| 91 | `recuperacao-de-acesso.md`:135 | *"Fluxo no painel, quando houver volume. Hoje é console e julgamento"* | nenhuma rota nem tela de recuperação de acesso no `merchant` ou em `frontend/src` | o fluxo no painel |
| 92 | `Equipe.java`:313 | *"O outro lugar que a quebrará é a automação da ADR-029, que cria ou promove membro sem passar por A1 — e que ainda não existe."* (a A3) | nenhum código de recuperação de estabelecimento no `merchant` | a A3 verificada também na automação |
| 93 | ADR-071 §2 (e o javadoc do `RoteamentoIT`) | *"**um caso pode afirmar o `id` errado e passar.** Isso **é** linha de razão, com gatilho: *o dia em que o teste resolver o caminho pelo próprio roteador em vez de pelo `id` declarado.*"* | o `RoteamentoIT` liga cada caso à rota por um `id` escrito à mão (R-3) | o caso resolvido pelo roteador |
| 94 | ADR-071, seção "Gatilho escrito" | *"**A próxima regra que alguém quiser escrever num segundo lugar.** Ela não se escreve: escreve-se o ponteiro, ou o comando que a deriva."* | não há comando que ache regra escrita em dois lugares (ADR-071 §4) | o ponteiro, ou o comando |
| 95 | ADR-071, seção "Gatilho escrito" | *"**A próxima lista que aparecer transcrita num teste.**"* | ninguém procurou depois do `RoteamentoIT`, e não há comando que as ache | a lista derivada do dono |
| 96 | ADR-071, seção "Gatilho escrito" | *"**E o dia em que existir comando que enumere regra duplicada.**"* | **não era gatilho.** A condição dependia de nós decidirmos escrever o comando, e a §4 da própria ADR-071 recusa esta linha. **Estornada pela emenda de 17/10/2026 à ADR-071** (R-4) | — |
| 83 | ADR-011:114 (e ADR-048:247, que o cita) | *"No MVP há uma instância por serviço e isso é invisível. Está escrito aqui porque é a primeira coisa a quebrar quando houver duas, e a falha é silenciosa."* | o compose sobe uma instância de cada serviço; a fila exclusiva por instância já existe no `catalog` (`AnonymousQueue`, `ConfiguracaoDaEscuta`), e nunca rodou com duas | a primeira execução com duas instâncias, medida |
| 84 | ADR-013:232 (e ADR-031:173, o evento da mesma saga) | *"é o desenho certo, e entra quando houver titular para exercer o direito."* (a saga de exclusão; o `TitularSolicitouExclusaoV1` é *"um nome já escolhido para um evento que ainda não existe, e quando existir passa por esta regra"*) | a exclusão é manual (`docs/operacao/exclusao-de-titular.md`); nada no repositório registra pedido de titular | a saga de exclusão, e o evento |
| 85 | ADR-021:76 (e `conversa.md`:291) | *"Volta a ser serviço quando houver aplicativo nativo e push, não antes."* (a notificação) | aplicativo nativo de consumidor está fora do escopo (`CLAUDE.md`); o painel avisa por consulta periódica | o serviço de notificação |
| 86 | ADR-045:142 | *"Continua sendo a resposta certa quando houver malha de serviço."* (mTLS) | nenhuma malha de serviço no compose nem em workflow | mTLS entre serviços |

**Fora da tabela, de propósito:** a ADR-037:203 adia o refresh token *"para ADR
própria"* e não escreve condição — o motivo dado, *"um marco que ainda não tem
painel para exercitar sessão longa"*, deixou de valer com a W-A, mas sem condição
escrita não há o que disparar. **É uma dívida sem gatilho**, e é a seção seguinte.

---

## Dívidas sem gatilho escrito

Achadas nas rodadas, sem condição que as reabra. **Sem gatilho, ninguém olha.**

| Dívida | De onde |
| --- | --- |
| O refresh token, com a baixa do TTL de 30 min | ADR-037:203 |
| O login com hash sem prefixo de cifrador responde **500**, não 401 (`IllegalArgumentException` do `DelegatingPasswordEncoder`) | medido na W-D |
| A rota que marca a opção devolve o resumo, sem os grupos | W-D; `CLAUDE.md`, armadilha "Vai mexer numa opção" |
| A falha de busca do JWKS responde 500 com o corpo do `/error`, e não `ProblemDetail` | I-B; ADR-055, emenda de 10/10 |
| Os seis blocos `listener.simple.retry` com `max-attempts`, que o Boot 4.1.1 não lê | ADR-048, nota de 10/10 |
| O par de chaves na raiz do repositório, que não é o de `secrets/` | I-B |
| `JWT_PUBLIC_KEY_PATH` no `.env.example`, que nada lê | I-B |
| A ADR-041 atribui à ADR-037 uma frase entre aspas que ela não tem — *"decidir quando o segundo consumidor existir"*; a ADR-037:210 diz *"Quando o segundo chegar, a pergunta chega junto, com evidência"* | medido na R-1 |
| As modalidades não estão no contrato: o mapa `metodosPorModalidade` é declarado com chave `string`, e o formulário da W-E nomeia `ENTREGA` e `RETIRADA` numa constante — a única lista dele sem o contrato atrás. Uma divergência com o domínio vira 400 sem `detail` | medido na W-E |
| A correção do Mongo no `conversation`, que nenhum teste exerce | `CLAUDE.md`, armadilha do MongoDB no Boot 4 |

---

## A pergunta de produto que precede o marco 3

**De onde nasce o pedido nos marcos 3 a 6?**

O PRD tem 29 histórias, e a única que cria pedido é a **H4.2** — *"Como cliente, quero
montar meu pedido e saber o total antes de confirmar"* —, que nasce na conversa. O canal
é o **marco 7**. O painel de pedidos é o **marco 3**, quatro marcos antes.

O perfil do estabelecimento (PRD §3.1) descreve a origem como *"WhatsApp
majoritariamente; telefone e balcão em menor volume"*, e **nenhuma história trata do
atendente registrando o pedido que chegou por telefone ou pelo balcão**.

Os caminhos, e nenhum é óbvio: uma história nova de lançamento manual; adiantar o mínimo
do canal para antes do marco 3; ou reordenar os marcos. **É decisão de produto, e ela
precede o código do marco 3.**

**Gatilho escrito:** o primeiro pacote do marco 3. Medido na R-1. *(Fora da tabela de
cima porque não é dívida de código — e fora da conta do cabeçalho, porque este arquivo
se exclui dos próprios comandos.)*

---

## Da segunda passada, e deliberadamente fora do razão

**Isto não é dívida: é o resto da varredura, nomeado para não ser triado outra vez.** Sem esta
lista a varredura seguinte reencontra cada uma destas linhas e refaz o trabalho — foi o que
custou a R-3, e o que as quatro da R-2 custaram, que só existiam no relatório e na mensagem do
commit.

| arquivo:linha | ramo | por quê |
|---|---|---|
| ADR-027:142 | 5 | *"A verificação de build ainda não existe."* Registra uma ausência sem condição que a dispare (R-2) |
| ADR-038:133 | 4 | a emenda de 16/10 cita a frase do corpo para dizer que ela envelheceu; a condição é a das linhas 1 e 6 |
| ADR-047:78 | 4 | *"o que vai importar quando houver troca de loja"* — descreve um benefício da decisão, que vale desde o seletor da W-E (R-2) |
| ADR-056:91 | 5 | *"uma regra para as rotas que ainda não existem"* — é regra, não condição (R-2) |
| `estabelecimento.md`:292 | 4 | por que a disponibilidade não responde se a loja está aberta. A R-2 a pôs no ramo 5, que era o que havia; com o ramo 4 escrito, é ele, como a `ExpedienteCorrenteResponse.java`:15 |
| `estabelecimento.md`:471 | **3** | *"as M9 e M10 passam a valer quando houver duas"* — duas áreas de entrega de uma loja é estado de dado. **A primeira vez que o ramo 3 separou alguma coisa**, na vigésima nona linha triada |
| `ConfiguracaoDaEscuta.java`:23 | 4 | explica por que o consumidor declara a exchange de novo: ligar fila a uma exchange inexistente falha |
| `ExpedienteCorrenteResponse.java`:15 | 4 | explica por que a resposta não tem `aberto` |

**O marco da varredura.** As ocorrências da segunda passada no commit que acrescentou esta
seção (R-3) foram **todas** triadas: **31** no total — 23 com a condição no razão, 8 nesta
tabela. O commit sai de `git log --format=%h -S"O marco da varredura" -- docs/dividas.md | tail -1`,
e não está escrito aqui porque o arquivo não sabe o hash do commit que o grava. A varredura
seguinte **compara o número com este e olha só a diferença**.

**A comparação é pela condição, não pelo endereço.** O razão cita a frase, e a mesma condição
aparece em `arquivo:linha` diferentes conforme o arquivo cresce — comparar por endereço deu
**17** falsos "sem linha" na R-3, contra **9** reais. *E se o número tiver caído sem rodada que
o explique, alguém apagou uma frase sem triá-la: isso é para se dizer, não para se somar.*
