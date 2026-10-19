# Procedimento — preparar a pilha para a passagem humana

**Status:** executado na E-2 (15/10/2026), passos 1 a 4, sobre `merchant-service` e `gateway`
· **Base:** `docs/como-subir-local.md`, ADR-051 (a imagem recebe o jar pronto), ADR-058
(a pilha sobe em grupos)

> **O que este documento existe para impedir.** Em 11/10 a passagem humana da W-F parou
> duas horas porque **a pilha não era o código**. Três mentiras educadas em sequência: o
> Gradle sem Java imprimiu erro e seguiu, o Docker reusou a camada e disse *"Built"*, o
> compose reusou o contêiner e disse *"Running"*. **Nenhuma delas é um erro**, e as três
> juntas servem o jar de ontem.

> **De onde vêm os comandos.** Dos nomes reais do `docker-compose.yml` e dos módulos do
> Gradle, e do `como-subir-local.md`. **Os comandos exatos digitados em 11/10 não foram
> registrados no repositório**, e este documento não os reproduz de memória: o que está
> aqui foi **executado na E-2**, e a saída de cada passo está colada no fim.

---

## Antes de tudo: os dois shells

| Shell | Onde | Por quê |
|---|---|---|
| **PowerShell** | `C:\dev\delivery-platform\backend` | **O Java vive no Windows.** O Ubuntu não tem JDK |
| **WSL** (Ubuntu) | `/mnt/c/dev/delivery-platform` | **O Docker vive no WSL.** Não há `docker.exe` no Windows |

**Trocar um pelo outro não dá erro alto** — é a primeira das três mentiras.

---

## Quais serviços cada rodada reconstrói

Os que a rodada **mudou**. Na dúvida, `git diff --stat <commit-anterior> -- backend/` diz.

| Módulo do Gradle | Serviço do compose |
|---|---|
| `:services:identity-service` | `identity-service` |
| `:services:merchant-service` | `merchant-service` |
| `:services:catalog-service` | `catalog-service` |
| `:infra:gateway` | `gateway` |

**A E-2 mexe no `merchant-service` e no `gateway`.**

---

## 1. Construir o jar — PowerShell, em `backend/`

```powershell
.\gradlew.bat :services:merchant-service:bootJar :infra:gateway:bootJar
```

**Confira que terminou em `BUILD SUCCESSFUL`.** Sem Java no caminho, o wrapper imprime o
erro e devolve o controle — e o passo seguinte usa o jar velho que estava lá.

## 2. Conferir a data do jar — WSL, na raiz

```bash
ls -l --time-style=+'%F %T' backend/services/merchant-service/build/libs/app.jar \
                            backend/infra/gateway/build/libs/app.jar
```

**A data tem de ser de agora.** Se for de ontem, o passo 1 não rodou — volte a ele.

## 3. Construir a imagem e recriar o contêiner — WSL, na raiz

```bash
docker compose --profile marco2 up -d --build --force-recreate --wait --wait-timeout 300 \
  merchant-service gateway
```

**Sem o `--force-recreate`, imagem nova e contêiner velho convivem em silêncio** — o compose
diz *"Running"* sobre o contêiner que já estava de pé. Sem o `--build`, o compose nem olha
para o jar novo.

## 4. As duas provas — WSL

```bash
docker compose --profile marco2 ps --format '{{.Service}} {{.Status}}'
docker compose --profile marco2 logs --since 5m merchant-service | grep -E "Started|Successfully applied|APPLIED"
```

- **`ps` diz segundos**, ou poucos minutos — nunca *"Up 2 days"* para o serviço que a rodada
  mudou;
- **o log mostra o arranque novo** (`Started ...Application`) — e, quando a rodada trouxer
  migração, a migração aplicada: no Flyway, `Successfully applied`; no Mongock, o
  `changeUnit` **`APPLIED`**.

**Só as duas juntas provam.** `ps` sem log diz que o contêiner é novo, não que o jar é.

## 5. Só então o navegador

Com os semeadores **desligados** (`DELIVERY_SEMEADURA=false` no `.env`). Confira sem abrir o
arquivo: `grep -c '^DELIVERY_SEMEADURA=false$' .env` deve imprimir `1`. Os contêineres leem a
chave **na subida**: se ela mudou depois da última, recrie `identity-service`,
`merchant-service` e `catalog-service` com `--force-recreate`, como no passo 3.

**Desligar não apaga.** Com a chave ligada, os semeadores rodam em toda subida e gravam o que
falta, por id fixo. Com ela desligada, não gravam nada, e o que já gravaram fica. Medido em
18/10/2026 (M-1): com a chave em `false` no `.env` e nos três contêineres, o banco tinha a
loja da fixture, o usuário dela e o produto, além de 8 contas, 2 lojas e 2 produtos de
passagens anteriores.

**E isso não atrapalha a passagem.** Uma conta nova não tem vínculo com a loja semeada nem com
as outras, e nenhuma delas aparece no painel dela. O painel vazio passa a ser prova do
vínculo, não de banco vazio. **A colisão é o telefone:** `+5511999990001` é o do usuário da
fixture. Use telefones que nunca foram usados nesta máquina, e anote quais foram.

O roteiro da passagem é da rodada que a pede; este documento só garante que o que está no
navegador é o código do commit.

---

## O que a execução da E-2 mostrou

*Colado da execução de 15/10/2026, sobre `merchant-service` e `gateway`.*

**Antes:** `docker compose --profile marco2 ps` dizia *"Up 2 days"* para os sete — inclusive o
`merchant-service` e o `gateway`, que a E-2 tinha acabado de mudar. **É a pilha que não é o
código**, medida.

```
== 1 · PowerShell
BUILD SUCCESSFUL in 26s

== 2 · a data do jar (2026-10-15 10:54:23, relógio do WSL)
2026-10-15 10:53:48 backend/infra/gateway/build/libs/app.jar
2026-10-15 10:53:56 backend/services/merchant-service/build/libs/app.jar

== 3 · build e recriação
 Image delivery-platform-gateway Built
 Image delivery-platform-merchant-service Built
 Container delivery-platform-gateway-1 Recreated
 Container delivery-platform-merchant-service-1 Recreated
 Container delivery-platform-gateway-1 Healthy
 Container delivery-platform-merchant-service-1 Healthy

== 4 · as duas provas
gateway Up 32 seconds (healthy)
merchant-service Up 31 seconds (healthy)
catalog-service Up 2 days (healthy)          ← não mudou na E-2, e não foi recriado
Current version of schema "public": 8        ← a E-2 não traz migração
Started MerchantServiceApplication in 23.294 seconds
Started GatewayApplication in 15.319 seconds
```

**E a prova pela rota, que é a que vale** (ADR-058: *"a prova é rota protegida pelo gateway,
não `/actuator/health`"*), sem token, pela porta do gateway:

```
GET  /api/v1/convites/nao-existe          404   ← público na cadeia, e o merchant responde
POST /api/v1/convites/nao-existe/aceite   401   ← barrado no gateway, antes do serviço
```

*Os nomes deste documento envelhecem com o compose. Ao renomear um serviço, emende a tabela
"Quais serviços cada rodada reconstrói" na mesma rodada.*

---

## 6 · O que a passagem produz

**Uma passagem humana produz texto, não um "funcionou".** A cada passo, anote **o que
apareceu na tela**: a frase exata, o estado do item, o número, o endereço na barra.

**Por quê:** em 15/10 a coluna do marco 2 foi cumprida e a frase que a provava não foi
copiada por ninguém. O que ficou foi *"tudo certo"* — e *"tudo certo"* não distingue a tela
que mostrou a frase certa da tela que mostrou qualquer outra.

**A regra, e ela vale para teste também:** prova que passa por um olho e não vira texto está
perdida. A memória de quem viu não é fonte.

**A W-G mexe só no front**: o `frontend` não é serviço do compose — ele roda no Vite, no
Windows (`npm run dev`) —, e nenhum dos passos 1 a 4 se aplica a ele. Os jars do
`merchant-service` e do `gateway` são os da E-2, recriados em 15/10 por este roteiro.
