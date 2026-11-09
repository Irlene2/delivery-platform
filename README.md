# AKI Delivery — delivery-platform

Plataforma de pedidos e operação para **comércio de bairro**: a pizzaria, a lanchonete, o
restaurante que hoje recebe pedido pelo WhatsApp, anota no caderno e fecha o caixa no fim da noite
sem saber direito quanto vendeu e quanto o entregador tem a acertar.

> **O cliente do produto é o comerciante, não o consumidor.** Não é marketplace, não cobra
> comissão sobre a venda e não fica com o dinheiro: o cliente paga direto à loja, e o sistema
> registra. O entregador é da loja. Se uma decisão parecer estranha, as premissas estão em
> [`docs/PRD.md`](docs/PRD.md) §5.

---

## Como funciona

1. **A loja se cadastra e monta o cardápio** — seções, produtos com foto, opções (como ponto da
   carne e adicionais), horário de funcionamento, taxa de entrega e a equipe com permissões.
2. **O link do cardápio vai na resposta automática do WhatsApp Business** da loja. Quem chama a
   loja recebe o link.
3. **O cliente faz o pedido pelo link, sem criar conta**: carrinho, entrega ou retirada, endereço
   e como vai pagar (dinheiro com troco, cartão ou Pix). O servidor calcula o total com o preço do
   cardápio. Fora do horário, a loja aparece fechada e não recebe pedido.
4. **A loja opera o pedido no painel**: aceita, prepara, deixa pronto, entrega a um entregador da
   equipe e avisa o cliente pelo WhatsApp com a mensagem pronta. O sino do topo avisa de pedido
   novo e do que mudou.
5. **O entregador, com a conta dele**, vê só as entregas dele, marca a saída e a entrega e registra
   como recebeu: dinheiro, cartão ou Pix. O Pix a loja confere no extrato.
6. **O cliente acompanha** o pedido pelo link que recebeu.
7. **No fim do dia, o administrador fecha o caixa**: total por forma de pagamento, conferência do
   dinheiro contado, acerto de cada entregador (o repasse é a taxa de cada entrega) e o histórico,
   com as vendas de qualquer período e a planilha para a contabilidade.

### O que o aplicativo não faz, por decisão

| Fora do escopo | Por quê |
|---|---|
| Cobrar ou processar pagamento | O cliente paga direto à loja; o sistema registra como foi pago (ADR-023) |
| Emitir nota fiscal | A loja emite no sistema fiscal que já usa; o pedido traz os dados |
| Marketplace e comissão | O cliente é o comerciante (PRD §7) |
| Entregador de plataforma | O entregador pertence à loja |
| Mapa em tempo real | O cliente acompanha pelo status; o mapa é melhoria futura |

---

## Funcionalidades

| Para quem | O que já funciona |
|---|---|
| **Dono / administrador** | Cadastro da conta pelo telefone, recuperação de senha pelo código, e uma ou mais lojas, com o passo a passo inicial · cardápio com seções, foto, descrição, opções e publicação · estoque do dia com busca e filtros · horário e loja fechada · equipe com convite e permissões · pedidos do dia, despacho e mensagem pronta para o WhatsApp · sino de notificações · conferência do Pix · dashboard de vendas · fechamento do caixa com histórico, período e CSV · link do cardápio |
| **Colaborador** | O painel com o que a permissão dele deixa: pedidos, cardápio, estoque |
| **Entregador** | Conta própria, "minhas entregas", saída, entrega e registro do recebimento (dinheiro, cartão ou Pix) |
| **Cliente** | Cardápio pelo link, sem conta · carrinho, entrega ou retirada, troco · acompanhamento do pedido |

---

## Arquitetura

Microsserviços com **arquitetura hexagonal**, um banco por serviço e um gateway como porta única
do front.

| Serviço | Porta | Banco | O que faz |
|---|---:|---|---|
| `gateway` | 8080 | — | Porta única do front: autentica o token, roteia por recurso e libera sem token só o cardápio público e o acompanhamento |
| `identity-service` | 8081 | PostgreSQL | Conta, verificação do telefone, login e emissão do token (JWT) |
| `merchant-service` | 8082 | PostgreSQL | A loja, o horário, a equipe, o convite e as permissões; diz se a loja está aberta |
| `catalog-service` | 8083 | MongoDB | Seções, produtos, fotos, opções, publicação, disponibilidade e o cardápio público |
| `order-service` | 8085 | PostgreSQL | O pedido e seus estados, a taxa da loja, o entregador, o pagamento registrado, as entregas e o caixa do dia |
| `settlement-service` | 8084 | PostgreSQL | *Planejado:* jornada e custódia do entregador. Hoje o caixa do dia mora no `order` |
| `delivery-service` | 8087 | PostgreSQL | *Planejado:* rodízio e retorno do entregador |
| `conversation-service` | 8088 | MongoDB | *Planejado:* o pedido nascendo dentro da conversa do WhatsApp (marcos 7 e 9) |

**Nenhum serviço acessa o banco de outro.** Os desenhos de contexto e de containers estão em
[`docs/architecture/`](docs/architecture/containers.md).

### O que está no repositório e ainda não roda

Para quem avalia o repositório: estas peças estão aqui **de propósito, para os próximos marcos**,
e nada da entrega de 15/11 depende delas. Não sobem nos perfis `marco2` e `pedidos`.

| Peça | O que tem hoje | Para quando |
|---|---|---|
| `settlement-service` | esqueleto: a classe de entrada, as pastas das camadas, o teste de arquitetura e o pipeline | a jornada e a custódia do entregador. O caixa do dia, que seria dele, foi feito no `order` |
| `delivery-service` | esqueleto, igual | o rodízio, a posição do entregador e o mapa da entrega (marco 11) |
| `conversation-service` | esqueleto, igual | o pedido nascendo na conversa do WhatsApp (ADR-075) e o atendimento com IA (marcos 7 e 9) |
| Redis e MinIO, no Compose | os contêineres, nos perfis `core` e `full` | o cache do cardápio público e as fotos fora do banco |
| [`docs/dominio/`](docs/dominio/README.md) `liquidacao.md`, `entrega.md` e `conversa.md` | as regras desenhadas para esses três serviços | servem de partida quando cada um ganhar código |
| [`contracts/eventos.md`](contracts/eventos.md) | o catálogo de eventos, inclusive os desses três serviços | idem |

Cada esqueleto tem um `README.md` na própria pasta dizendo isto.

### Stack

- **Back-end:** Java 21, Spring Boot 4, Gradle (Kotlin DSL), PostgreSQL com Flyway, MongoDB com
  Mongock, RabbitMQ com Outbox, JWT assinado pelo `identity`.
- **Front-end:** React 19, Vite, TypeScript estrito, com o visual do protótipo `aki_delivery`
  ([`frontend/README.md`](frontend/README.md)).
- **Qualidade:** ArchUnit vigiando as camadas, Testcontainers nos testes de banco, contratos
  OpenAPI gerados do código e conferidos no CI, um pipeline por serviço e varredura de segredos
  em todo push.

### Dentro de cada serviço

```
api ────────────────┐
                    v
infrastructure -> application -> domain
```

- `domain` não conhece Spring, JPA nem MongoDB;
- `application` orquestra os casos de uso e depende de interfaces (`port/out`);
- `infrastructure` implementa persistência, mensageria e clientes;
- `api` traduz HTTP em comando.

Cada serviço tem um `HexagonalArchitectureTest` que quebra o build se a regra for violada.

### Estrutura do repositório

```
delivery-platform/
├── backend/                    build Gradle multi-projeto
│   ├── build-logic/            convention plugins — a configuração compartilhada
│   ├── infra/gateway/
│   ├── value-types/            os tipos de valor compartilhados, como Money
│   └── services/               os sete serviços
├── contracts/                  OpenAPI dos serviços e o catálogo de eventos
├── docs/                       produto, domínio, decisões, operação e a entrega
├── frontend/                   o painel, o cardápio público e o app do entregador
├── infra/                      configuração de PostgreSQL e MongoDB
└── .github/workflows/          um pipeline por serviço, front, contratos e segurança
```

---

## Como rodar

**Para testar, comece por [`docs/como-testar.md`](docs/como-testar.md):** os três jeitos (pelo link
de quem já subiu, na sua máquina e aberto para o celular pela Cloudflare), passo a passo no
Windows com Docker Desktop, e o roteiro do cadastro da loja ao caixa fechado.

**Requisitos:** JDK 21, Docker com Docker Compose, Node 22 ou mais novo, e uns 10 GB livres em
disco. O resumo:

```bash
cp .env.example .env                       # preencha as senhas; o .env nunca vai para o Git
mkdir secrets                              # a chave do login, com os dois comandos openssl do .env.example
cd backend && ./gradlew bootJar && cd ..   # os jars que as imagens usam (no Windows: .\gradlew.bat)
docker compose --profile marco2 --profile pedidos build
docker compose --profile marco2 --profile pedidos up -d --wait
cd frontend && npm ci && npm run dev       # http://localhost:5173
```

Se a máquina tiver pouca memória, suba em grupos, como mostra o `docs/como-testar.md`.

| Perfil do Compose | Sobe |
|---|---|
| `marco2` | PostgreSQL, MongoDB, RabbitMQ, gateway, `identity`, `merchant` e `catalog` |
| `pedidos` | o `order-service`, somado ao `marco2` — **é o que se usa hoje** |
| `core` | só a infraestrutura, para rodar um serviço pelo IDE |
| `services` / `full` | todos os serviços, inclusive os planejados |

**Loja de teste pronta:** com `DELIVERY_SEMEADURA=true` e uma senha em `DELIVERY_SEMEADURA_SENHA`,
a subida grava a **"Pizzaria da Fixture"**, com 24 produtos, fotos e opções. Entre com
`+5511999990001` e ajuste o Horário para o dia do teste. Para cadastrar contas novas sem SMS, no
ambiente local, use `DELIVERY_CODIGO_NA_TELA=true`: o código do cadastro e do "Esqueci minha
senha" aparece na tela — só com contas de teste. O passo a passo está
em [`docs/como-subir-local.md`](docs/como-subir-local.md).

**Testes:**

```bash
cd backend && ./gradlew :services:order-service:test     # um serviço (no Windows: .\gradlew.bat)
cd frontend && npm run typecheck && npm run lint && npm test
npm ci && npm run contracts                                # na raiz: os contratos
```

**Hospedagem de teste sem custo, para abrir no celular:** [`docs/operacao/hospedagem-de-teste.md`](docs/operacao/hospedagem-de-teste.md)
— dois túneis da Cloudflare, usados nas evidências de 07 e 08/11/2026. **Não há ambiente de
produção.**

---

## Marcos

A numeração é a do [`docs/PRD.md`](docs/PRD.md) §10, medida pela pergunta *"o comerciante
consegue?"*. Situação em **08/11/2026**. 🟡 = funciona no recorte da entrega de 15/11 (ADR-077), mas
não é o marco inteiro.

| Marco | O comerciante consegue | Situação |
|---|---|---|
| 0 | — (esqueleto: build, módulos, Compose, CI) | ✅ |
| 1 | Cadastrar a loja e o time, com poderes diferentes | ✅ loja do zero, convite, permissões, sessão de 12 horas. **Falta:** enviar o código de verificação por SMS ou WhatsApp |
| 2 | Publicar o cardápio e marcar o que acabou | ✅ cardápio com foto, descrição e opções, correção de produto, estoque do dia com busca. **Falta:** criar grupos de opções pelo painel |
| 3 | Receber e acompanhar pedidos numa tela | 🟡 o pedido vem do link do cardápio, o painel leva por todos os estados, o cliente acompanha, a loja fechada não recebe, o sino avisa. **Falta:** o pedido nascer dentro da conversa (ADR-075) |
| 4 | Registrar como cada pedido foi pago de verdade | ✅ dinheiro com troco, cartão e Pix, registrados no pedido; o Pix informado pelo entregador a loja confere no extrato |
| 5 | Despachar e saber quem está com cada pedido | ✅ entregador da equipe com conta, despacho e "minhas entregas"; repasse por entrega, igual à taxa de entrega |
| **6** | **Fechar o dia com extrato por entregador** | ✅ caixa apurado e gravado, conferência do dinheiro, acerto por entregador, histórico, vendas por período e CSV |
| 7 | Receber pedido pelo WhatsApp | 🟡 a resposta automática do WhatsApp Business manda o link; o aviso ao cliente é a mensagem pronta (`wa.me`). **Falta:** o aviso sair automático |
| 9 | Atender sozinho a maior parte dos pedidos | ⏳ futuro: atendimento com IA no WhatsApp |
| 10 | Operar com contagem de estoque | ⏳ planejado: o produto que pode acabar ganha quantidade, que baixa a cada pedido |
| 11 | Mostrar a entrega no mapa | ⏳ futuro. Hoje o cliente acompanha pelo status |

Pagamento e nota fiscal acontecem na loja, e por isso não há marco 8 (PRD §7). **Ao fim do marco 6
existe produto completo:** a loja cadastra equipe e cardápio, recebe e prepara pedidos, despacha com
entregador próprio, registra como cada pedido foi pago e fecha o caixa com extrato.

### O que falta

**Para a entrega de 15/11** — a lista com esforço e responsáveis está em
[`docs/entrega-15-11/README.md`](docs/entrega-15-11/README.md):

- o contrato OpenAPI e os testes de integração do `order`;
- o ensaio da apresentação;
- a aprovação da ADR-077 pelo grupo.

Já feitos: a passagem completa no navegador e no celular, o vídeo da entrega, a hospedagem de
teste pela Cloudflare, a resposta automática do WhatsApp Business com o link e os contratos dos
outros serviços regenerados.

**Para vender, depois:** o código de verificação por SMS ou WhatsApp (serve ao cadastro e ao
"Esqueci minha senha"), grupos de
opções pelo painel, o aviso automático ao cliente, a hospedagem de produção (domínio, HTTPS e cópia
de segurança), os termos de uso e a política de privacidade, e os marcos 7, 9, 10 e 11. As dívidas
assumidas estão em [`docs/dividas.md`](docs/dividas.md).

---

## Documentação

| Onde | O que responde |
|---|---|
| [`docs/PRD.md`](docs/PRD.md) | O produto: para quem, o problema, o escopo, o que fica fora e os marcos |
| [`docs/entrega-15-11/`](docs/entrega-15-11/README.md) | A entrega de 15/11: o fluxo, as etapas, as rotas do pedido, como testar e o que falta |
| [`docs/dominio/`](docs/dominio/README.md) | As regras de cada serviço: agregados, invariantes, estados |
| [`docs/architecture/decisions/`](docs/architecture/decisions/README.md) | As decisões de arquitetura (ADRs), com alternativas e consequências |
| [`docs/como-testar.md`](docs/como-testar.md) | **Comece aqui para testar:** pelo link, na sua máquina ou pela Cloudflare, e o roteiro do teste |
| [`docs/como-subir-local.md`](docs/como-subir-local.md) | A referência completa da subida: tempos medidos, rodar um serviço pelo IDE, e cada problema já visto |
| [`docs/operacao/`](docs/operacao/README.md) | Procedimentos: hospedagem de teste, exclusão de dados, fila morta |
| [`frontend/README.md`](frontend/README.md) | As telas, as rotas do front e o que cada uma chama |
| [`CLAUDE.md`](CLAUDE.md) | As invariantes e as convenções do código, escritas para o assistente de código (Claude Code) que ajudou a construir o projeto — e úteis para gente |

### Decisões que mais moldam o código

| ADR | Decisão |
|---|---|
| 021 | Sete serviços de negócio e o gateway, um banco por serviço |
| 023 | O pedido registra o pagamento; o produto não cobra |
| 012 | O gateway roteia por recurso, não por serviço |
| 015 | Só o `identity` emite token; todos os serviços validam |
| 035 | O domínio fala português; o meio técnico, inglês |
| 059 | A loja de teste é semeada pelo agregado, nunca pelas tabelas |
| 077 | O recorte da entrega de 15/11: o pedido nasce pelo link do cardápio |

---

## Regras do projeto

- **Migrations, sempre.** Toda estrutura nasce em Flyway (SQL) ou Mongock (MongoDB).
- **Quem publica evento usa Outbox; quem consome guarda o que já processou.**
- **Nenhum segredo no Git.** Só o `.env.example`, com os nomes; a varredura roda em todo push.
- **Timeout em toda chamada entre serviços.**
- **O valor cobrado vem do servidor**, nunca de um número enviado pela tela.
- **Nenhuma entrega ou retirada conclui sem o pagamento registrado.**

---

## Como colaborar

1. **Peça acesso** a quem administra o repositório (GitHub → *Settings → Collaborators*).
2. **Clone e suba local** pelo [`docs/como-testar.md`](docs/como-testar.md). As convenções do
   código estão no [`CLAUDE.md`](CLAUDE.md).
3. **Uma branch por tarefa**, a partir da `main` atualizada:
   `git switch main; git pull; git switch -c <tipo>/<assunto>` — por exemplo
   `feat/grupos-de-opcoes` ou `fix/loja-fechada`.
4. **Antes do push**, o que o CI vai rodar:
   - back: `.\gradlew.bat :services:<servico>:test`, em `backend`;
   - front: `npm run format`, `npm run lint`, `npm run typecheck` e `npm test`, em `frontend`;
   - contratos: `npm ci` e `npm run contracts`, na raiz.
5. **Abra um Pull Request para a `main`** e espere o CI verde e uma revisão.
6. **Mensagens de commit** no formato do histórico: `feat(order): …`, `fix(front): …`,
   `docs: …`, em português.
7. **Nunca suba** `.env`, `secrets/` nem chave `.pem`.
