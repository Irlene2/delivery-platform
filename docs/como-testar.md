# Como testar o AKI Delivery

**Para quem:** quem é do time e quer ver o sistema funcionando, do cadastro da loja ao caixa
fechado. **Escrito em 08/11/2026**, com o sistema que foi gravado no vídeo da entrega.

Há três jeitos de testar. Escolha pelo que você tem:

| Jeito | Precisa de | Serve para |
| --- | --- | --- |
| **A. Pelo link de quem já está com o sistema no ar** | um navegador | testar as telas, no computador ou no celular |
| **B. Na sua máquina** | Docker Desktop, JDK 21, Node 22, uns 10 GB livres | testar e mexer no código |
| **C. Na sua máquina, aberto para o celular de outras pessoas** | o B e o `cloudflared` | mostrar para quem não está na sua rede, gravar evidências |

Depois de escolher, siga o [roteiro do teste](#roteiro-do-teste-do-cadastro-ao-caixa): é o
mesmo nos três.

> **Produção não está coberta aqui.** Não há ambiente de produção: domínio, HTTPS, cópia de
> segurança e servidor fixo estão nos próximos passos do README, porque custam dinheiro. Os
> jeitos B e C rodam no computador de alguém e servem para teste e apresentação.

---

## A. Pelo link de quem já está com o sistema no ar

Peça o link para quem subiu o sistema (o jeito C gera um endereço
`https://…trycloudflare.com`). Abra no navegador ou no celular e vá para o roteiro.

- O endereço **muda toda vez** que o túnel sobe, e só funciona enquanto o computador de quem
  subiu estiver ligado. Se parar de abrir, peça o link de novo.
- Use **só dados de teste**: o código de verificação aparece na tela e qualquer pessoa com o
  link consegue criar conta.

---

## B. Na sua máquina (Windows com Docker Desktop)

São uns 30 minutos na primeira vez, quase tudo esperando download e build. Da segunda vez em
diante, só o passo 6.

### 1. Instale, uma vez

- **Docker Desktop**, com WSL 2 (o instalador oferece). Abra e espere ficar pronto.
- **JDK 21** (Temurin). Outra versão, como a 23, não serve: o build pede a 21.
- **Node 22** e **Git** para Windows.
- **Espaço:** uns **10 GB livres** no `C:` para as imagens, e uns **4 GB de memória** só para os
  contêineres.

### 2. Baixe o projeto

```powershell
git clone https://github.com/camilasatie99/delivery-platform.git
cd delivery-platform
```

Todos os comandos abaixo partem desta pasta, no **PowerShell**.

### 3. Crie o `.env`

```powershell
Copy-Item .env.example .env
notepad .env
```

Preencha **pelo menos** estas linhas. As senhas você inventa: são só da sua máquina.

| Variável | O que pôr |
| --- | --- |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | um usuário e uma senha quaisquer |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | um usuário e uma senha quaisquer |
| `CORS_ALLOWED_ORIGINS` | deixe `http://localhost:5173` |
| `DELIVERY_SEMEADURA` | `true`: a subida cria a loja de teste, a **Pizzaria da Fixture**, com 24 produtos e fotos |
| `DELIVERY_SEMEADURA_SENHA` | a senha da conta da Pizzaria da Fixture, de 8 a 72 caracteres |
| `DELIVERY_CODIGO_NA_TELA` | `true`: o código do cadastro e do "Esqueci minha senha" aparece na tela, porque não há SMS |

`REDIS_PASSWORD` e as do `MINIO` podem ficar vazias: nada do teste usa esses dois. Os
`JWT_*` também podem ficar vazios, e o Compose usa o padrão.

**Copiar sem preencher não funciona:** o `postgres` sobe sem senha e fica `unhealthy`. **O
`.env` nunca vai para o Git** (está no `.gitignore`) e não se manda por mensagem.

### 4. Gere a chave do login

O `identity` assina o token com uma chave que cada máquina gera. O `openssl` vem com o Git:

```powershell
mkdir secrets
& "C:\Program Files\Git\usr\bin\openssl.exe" genrsa -out secrets/jwt-private.pem 2048
& "C:\Program Files\Git\usr\bin\openssl.exe" rsa -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem
```

**Gere antes de subir.** Se subir sem o arquivo, o Docker cria uma *pasta* com o nome
`jwt-private.pem` e o `identity` não sobe; apague a pasta, gere a chave e suba de novo.

### 5. Construa (na primeira vez, e quando o código mudar)

```powershell
cd backend; .\gradlew.bat bootJar; cd ..
docker compose --profile marco2 --profile pedidos build
```

O primeiro compila os serviços Java (uns 2 minutos); o segundo monta as imagens com os jars.
Se o segundo terminar em segundos sem construir nada, faltaram os `--profile`.

### 6. Suba, em grupos

Um comando de cada vez. Cada um só volta quando o grupo está saudável:

```powershell
docker compose --profile marco2 --profile pedidos up -d --wait --wait-timeout 300 postgres mongodb mongo-init rabbitmq
docker compose --profile marco2 --profile pedidos up -d --wait --wait-timeout 300 identity-service
docker compose --profile marco2 --profile pedidos up -d --wait --wait-timeout 300 merchant-service catalog-service
docker compose --profile marco2 --profile pedidos up -d --wait --wait-timeout 300 order-service
docker compose --profile marco2 --profile pedidos up -d --wait --wait-timeout 300 gateway
docker compose --profile marco2 --profile pedidos ps
```

No último, todos devem aparecer `healthy`. **Por que em grupos:** os serviços dependem do
`identity`, e subir os quinze contêineres do Compose de uma vez já derrubou uma máquina. O
perfil `marco2` mais o `pedidos` sobe só o que o sistema usa.

Da segunda vez em diante, com nada mudado no código, basta este passo.

### 7. Suba o site

```powershell
cd frontend
npm ci
npm run dev
```

Abra **http://localhost:5173**. Entre com o telefone **`+5511999990001`** e a senha do
`DELIVERY_SEMEADURA_SENHA`: é o dono da Pizzaria da Fixture.

### 8. Ajuste o horário da loja de teste

A Pizzaria da Fixture abre só uma faixa de 3 horas, contada da primeira subida. Fora dela, a
vitrine diz *"loja fechada"* e recusa pedido. No painel, em **Horário**, ponha a faixa do dia em
que vai testar.

### Para parar

```powershell
docker compose --profile marco2 --profile pedidos stop
```

O `stop` guarda os dados. O `down -v` **apaga** o banco inteiro: use só se quiser começar do
zero.

---

## C. Aberto para o celular de outras pessoas (Cloudflare)

Faça o B primeiro. Aqui o site sai do seu computador por dois túneis gratuitos da Cloudflare,
sem conta e sem cartão: um para o site e um para a API. O passo a passo com as explicações está
em [`operacao/hospedagem-de-teste.md`](operacao/hospedagem-de-teste.md); o resumo é este.

1. Instale, uma vez: `winget install --id Cloudflare.cloudflared`.
2. **Túnel do site**, num terminal que fica aberto:
   `cloudflared tunnel --url http://localhost:4173`. Anote o endereço
   `https://…trycloudflare.com` que aparece no meio do log: é o **endereço B**.
3. **Túnel da API**, em outro terminal: `cloudflared tunnel --url http://localhost:8080`. Anote o
   **endereço A**.
4. No `.env`, libere o site na API e recrie o gateway:
   ```
   CORS_ALLOWED_ORIGINS=http://localhost:5173,<endereço B>
   ```
   ```powershell
   docker compose --profile marco2 --profile pedidos up -d gateway
   ```
5. Gere o site apontando para a API pública e sirva o build, num terceiro terminal:
   ```powershell
   cd frontend
   $env:VITE_API_BASE_URL = "<endereço A>"; npm run build
   npm run preview
   ```
6. Abra o **endereço B** no celular. O link do cardápio, em Configurações, já sai com o endereço
   público: é ele que vai na resposta automática do WhatsApp Business da loja.

"Connection refused" no túnel antes do passo 5 é normal: o site ainda não estava servindo.
**Os dois endereços mudam toda vez** que os túneis sobem, e aí repete os passos 4 e 5. **Feche os
túneis quando terminar** (Ctrl+C nos dois terminais): enquanto estão abertos, o sistema é público.

---

## Roteiro do teste: do cadastro ao caixa

É o roteiro do vídeo da entrega. Use três "pessoas": a **loja** (o computador), o **entregador**
(uma janela anônima ou outro navegador) e o **cliente** (o celular, ou outra janela anônima).

| # | Quem | O que fazer | O que tem de acontecer |
| --- | --- | --- | --- |
| 1 | Loja | Criar conta com um telefone de teste, informar o código que aparece na tela | Cai em "Cadastrar minha loja" |
| 2 | Loja | Cadastrar a loja: nome, CNPJ, telefone, endereço, troco, entrega e formas de pagamento | A loja aparece em "Minhas lojas" |
| 3 | Loja | Em **Horário**, abrir a loja agora; em **Catálogo**, criar dois produtos com foto e publicar | Os produtos aparecem em "Ver cardápio" |
| 4 | Loja | Em **Equipe**, convidar o entregador com a permissão "Entregador" e copiar o link do convite | Sai um link de convite |
| 5 | Entregador | Abrir o convite, criar a conta dele (ou entrar, se já tiver) e aceitar | Entra em "Minhas entregas", sem acesso ao resto do painel |
| 6 | Loja | Em **Configurações**, definir a taxa de entrega e copiar o **link do cardápio** | O link abre a vitrine sem pedir login |
| 7 | Cliente | Pelo link, montar o carrinho, escolher entrega, endereço e forma de pagamento, finalizar | Aparece "A loja já está com seu pedido" e a tela de acompanhamento |
| 8 | Loja | Ver o **sino** avisar; em **Pedidos**, aceitar, preparar, deixar pronto e entregar ao entregador | O status muda sozinho na tela do cliente |
| 9 | Loja | Clicar em avisar pelo WhatsApp | O WhatsApp abre com a mensagem pronta e o link de acompanhamento |
| 10 | Entregador | Marcar a saída, registrar o pagamento e marcar entregue | O pedido fica "Entregue" e o ganho do dia dele aparece |
| 11 | Loja, se foi Pix | Em Pedidos, **"Conferi o Pix no extrato"** | O aviso de Pix a conferir sai do sino |
| 12 | Loja | Em **Fechamento do dia**, conferir os totais e fechar o caixa | Total por forma de pagamento e acerto do entregador; com Pix a conferir, o caixa **não** fecha |
| 13 | Loja | Em "Vendas por período", escolher as datas e baixar a planilha | Sai um CSV com as vendas |

**Atalho:** a Pizzaria da Fixture já tem cardápio. Para testar só o pedido, entre com ela e
comece no passo 6.

**Pela API**, sem telas: [`entrega-15-11/testes-pedido.http`](entrega-15-11/testes-pedido.http),
na ordem (extensão REST Client do VS Code).

**Testes automáticos:** no `backend`, `.\gradlew.bat test` (os de integração precisam do Docker
aberto); no `frontend`, `npm run typecheck`, `npm run lint` e `npm test`.

---

## Quando der errado

| Sintoma | O que fazer |
| --- | --- |
| A vitrine diz *"loja fechada"* | Ajuste o **Horário** da loja (passo 8 do B) |
| O painel carrega e nenhuma chamada responde | `CORS_ALLOWED_ORIGINS` no `.env` sem `http://localhost:5173`, ou, pelo túnel, sem o endereço B. Corrija e recrie o `gateway` |
| `postgres` `unhealthy` na primeira subida | O `.env` não foi preenchido. Preencha e rode `docker compose --profile marco2 --profile pedidos down -v` (apaga os dados) antes de subir de novo |
| O `identity` não sobe | Falta `secrets/jwt-private.pem`, ou ele virou pasta (passo 4), ou falta `DELIVERY_SEMEADURA_SENHA` com a semeadura ligada |
| `COPY failed: no source files were specified` no build | Faltou o `bootJar` (passo 5) |
| `read-only file system` no build | Disco cheio. Libere uns 10 GB e reabra o Docker Desktop |
| `Cannot find a Java installation … languageVersion=21` | Instale o JDK 21 e rode `.\gradlew.bat --stop` |
| `.\gradlew.bat` não é reconhecido | O Gradle mora em `backend/`: `cd backend` |
| O túnel abre e o celular não carrega o site | Confira se o `npm run preview` está rodando, na porta 4173 |
| Esqueceu a senha de uma conta de teste | "Esqueci minha senha" na tela de entrar; o código aparece na tela |

Os casos mais raros, e o porquê de cada um, estão em
[`como-subir-local.md`](como-subir-local.md), seção 5.

## Para olhar dentro dos bancos

No PowerShell, mande a consulta pela entrada do comando: o PowerShell do Windows tira as aspas
de dentro de um `sh -c '…"…"…'`, e o banco recebe a consulta pela metade.

```powershell
# as contas e os telefones
"select nome, telefone from usuario order by nome;" | docker compose exec -T postgres sh -c 'psql -U $POSTGRES_USER -d identity_db'

# os últimos pedidos
"select numero, status, total, entregador from pedido order by criado_em desc limit 5;" | docker compose exec -T postgres sh -c 'psql -U $POSTGRES_USER -d order_db'
```

As senhas não aparecem em lugar nenhum: o banco guarda só o hash (BCrypt). Para trocar, use o
"Esqueci minha senha".
