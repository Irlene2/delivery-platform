# ADR-050 — A documentação viva, e só no seu computador

- **Estado:** aceita
- **Data:** 30/09/2026
- **Relacionadas:** ADR-039 (o contrato é a especificação), ADR-044 (a cadeia de filtros), ADR-012 (o gateway roteia por recurso)
- **Invariantes:** nenhuma nova

## Contexto

Os contratos OpenAPI estão commitados e congelados por teste desde a ADR-039 —
*"O contrato OpenAPI é gerado do código e congelado no repositório"*, diz o
título dela. Eles são legíveis, e qualquer visualizador os abre.

O que não existe é a **página**: abrir o navegador em `localhost`, ver as rotas,
colar um token e apertar um botão. Pareciam faltar três coisas, e faltavam duas:

1. ~~o artefato do springdoc pode ser o `-api`, que gera o JSON e **não** traz a
   interface~~ — **não era obstáculo**, conferido na W-B: o artefato é o
   `springdoc-openapi-starter-webmvc-ui` em todos os serviços, pela convenção
   `delivery.spring-service-conventions`, e o `springdoc.swagger-ui.path` já
   estava declarado no `application.yml` do `merchant` e do `catalog`. A página
   existia; o que a escondia era a cadeia de filtros;
2. a cadeia de filtros de cada serviço termina em
   `anyRequest().authenticated()`, com só `/actuator/health/**` liberado, então
   `/swagger-ui/**` e `/v3/api-docs/**` respondem **401**;
3. o gateway roteia `/api/v1/**` e mais nada — a página seria **por serviço**,
   na porta de cada um, e não no 8080.

O terceiro parece um obstáculo e é a solução dos outros dois.

## Decisão

### 1. A página é por serviço, e o gateway não a roteia

Cada serviço serve a sua, na porta dele — `merchant` na 8082 e `catalog` na
8083, publicadas em `127.0.0.1` no compose. **Esta rodada abre duas:** o
`merchant` e o `catalog`, que são os dois serviços com rota protegida por token.

O `identity` fica de fora, e o motivo é método, não escopo: a cadeia de filtros
dele é diferente das outras duas — ele libera `/api/v1/auth/**` inteiro — e eu
nunca a abri. Depois da G-B5, escrever uma terceira cadeia de memória é
exatamente o defeito que eu prometi não repetir. **Gatilho escrito:** quem abrir
o `SecurityConfig` do `identity` para qualquer outra coisa acrescenta a
propriedade na mesma passada.

**Não se acrescenta rota no gateway para a documentação.** O gateway é a
superfície pública deste sistema, e acabamos de tirar o `/actuator/gateway` dela
justamente por publicar o mapa interno (ADR-044, emenda de 30/09). Rotear a
documentação por ali seria repor o mesmo problema com outro nome, no mesmo dia.

### 2. Fechada por padrão, aberta por propriedade

```yaml
delivery:
  docs:
    abertas: false     # o padrão, e o que vai para qualquer ambiente
```

A cadeia de filtros libera `/swagger-ui/**`, `/swagger-ui.html` e
`/v3/api-docs/**` **apenas** quando a propriedade é verdadeira.

O padrão é o ponto. O `/actuator/gateway` esteve exposto desde que alguém o
abriu para conferir um predicado até a G-B5 tirá-lo, e o comentário ao lado dele
já dizia que ele sairia "antes de qualquer ambiente exposto" — o que não é
mecanismo nenhum. A diferença aqui é que **esquecer a configuração fecha**, em
vez de abrir.

### 3. A propriedade não liga a documentação — liga o acesso sem token

O springdoc gera o documento de qualquer jeito, com a propriedade ligada ou não:
é dele que sai o `openapi.json` commitado, e o `ContratoOpenApiIT` depende
disso. O que a propriedade controla é uma coisa só: se a rota responde **sem
`Authorization`**.

Com ela desligada, `/v3/api-docs` continua existindo — e continua respondendo
401, como qualquer outra rota deste serviço.

### 4. Dois testes, e um deles não serve sozinho

| Caso | O que ele prova |
|---|---|
| propriedade **ligada** → `GET /v3/api-docs` devolve `200` e um JSON com a chave `openapi` | que a interface existe **e** que está aberta |
| propriedade **desligada** → `GET /v3/api-docs` devolve `401` | que o padrão fecha |

**O segundo sozinho é um verde falso.** Se o springdoc do `-ui` não estiver no
classpath, a rota não existe — mas a cadeia de filtros responde 401 antes de o
roteamento decidir, e o teste passa por um motivo que não tem nada a ver com o
que ele afirma. É a mesma forma do
`o_actuator_do_gateway_exige_token`, que a G-B5 achou: um 401 que vem antes de
tudo prova pouco.

**Por isso o primeiro é obrigatório**, e ele é o que falha se o artefato for o
errado.

**Medido na W-B**, com a liberação desligada de propósito no `merchant` e depois
restaurada: os dois casos da classe aberta — o documento e a página — ficaram
vermelhos, e a classe fechada continuou verde. É a prova de que ela, sozinha,
não percebe nada.

### 5. `127.0.0.1`, nunca `0.0.0.0`

A regra do `CLAUDE.md` não abre exceção para documentação: uma página de API
servida na interface de rede é a planta da casa na calçada.

**Se alguma porta do compose estiver publicada sem o `127.0.0.1`, isso é achado
de rodada** e se conserta antes de a página existir — a página não cria o
problema, ela só o torna clicável.

Conferido na W-B: as dezesseis portas publicadas no `docker-compose.yml`, em
catorze contêineres — bancos, brokers, gateway e os oito serviços —, estão todas
em `127.0.0.1`.

## Consequências

**Positivas**

- Dá para clicar. A distância entre "o contrato diz" e "eu vi responder" cai
  para um navegador aberto.
- O contrato continua sendo a fonte: a página **lê** o mesmo documento que o
  teste congela. Não há segunda descrição da API para envelhecer.
- O padrão fechado é a configuração inteira — não há passo a lembrar para
  fechar.

**Negativas**

- **Dois serviços ganham a mesma classe de propriedades e a mesma linha na
  cadeia**, e é duplicação honesta: cada serviço tem a sua cadeia de filtros, e
  não há módulo compartilhado onde pôr isto. **Gatilho escrito:** o dia em que a
  cadeia de filtros virar módulo comum, esta propriedade vai junto — e aí o
  `identity` entra de graça.
- **A página não atravessa o gateway**, então ela não exercita CORS, nem os
  filtros do gateway, nem o roteamento. Quem quiser ver a cadeia inteira usa o
  front. A página é para ler e experimentar uma rota, não para validar a
  topologia.
- **Um token ainda é preciso** para as rotas protegidas, e como o `identity`
  fica de fora desta rodada, ele se pega por fora: `POST /api/v1/auth/login` no
  gateway, e o token vai no botão "Authorize" da página do `merchant` ou do
  `catalog`. A página não facilita o login, e isso é de propósito — facilitar
  seria guardar credencial em algum lugar.

## Alternativas consideradas

- **Liberar sem propriedade, "porque é só desenvolvimento".** Rejeitada: é
  exatamente a frase que deixou o `/actuator/gateway` aberto. "Só
  desenvolvimento" é uma intenção, não um mecanismo.
- **Um perfil `dev` do Spring em vez de uma propriedade.** Perfil funcionaria, e
  foi recusado por um motivo pequeno e concreto: os testes de integração deste
  repositório configuram propriedades com `@TestPropertySource`, e um perfil
  exigiria `@ActiveProfiles` mais uma segunda forma de dizer a mesma coisa. Uma
  propriedade é testável com o mecanismo que já existe — e os dois testes da §4
  são a razão de a decisão existir.
- **Rotear `/v3/api-docs` de todos os serviços pelo gateway, numa página só.**
  É a opção mais confortável e a mais perigosa: uma única URL pública listando a
  API inteira. Rejeitada pela §1.
- **Nada, e continuar lendo o JSON commitado.** É o que se fazia, e funciona —
  com um detalhe que custa dez minutos toda vez: o `ContratoOpenApiIT` remove o
  bloco `servers` de propósito, então nenhum visualizador sabe para onde mandar
  a requisição.
