# ADR-047 — Onde o token do painel mora no navegador

**Status:** Aceita — 29/09/2026
**Relacionada:** ADR-015 (par de chaves RS256), ADR-016 (front mínimo antes da
PWA), ADR-037 (token de trinta minutos, sem refresh), ADR-044 (a cadeia de
filtros do gateway)
**Decide por:** `frontend/src/auth/armazenamento.ts`

## Contexto

A W-A põe no ar o primeiro consumidor real da API, e com ele a primeira pergunta
de segurança que este repositório nunca precisou responder: **um token válido
passa a existir fora de um teste.**

Três fatos do sistema restringem a escolha, e os três já estavam decididos:

1. **A credencial é um cabeçalho, não um cookie.** A ADR-044 escreve isso com
   essas palavras, e o CORS do gateway declara `allowCredentials: false`. Não há
   cookie de sessão a considerar, e a alternativa "cookie `HttpOnly` emitido pelo
   servidor" exigiria mudar o `identity`, o gateway e a ADR-044 — é uma decisão
   maior do que esta rodada.
2. **O token vale trinta minutos e não há refresh.** A ADR-037 diz que *"o access
   token é a sessão"* e que o refresh está *"adiado com registro"*. Persistir
   além da aba não guarda uma sessão: guarda um token que expira antes da próxima
   vez que a pessoa abrir o navegador.
3. **Não há nada dentro do token em que se possa confiar para decidir acesso.**
   Seis claims, nenhuma permissão. Quem autoriza é o `merchant`, por loja, por
   requisição.

## Decisão

**A sessão mora em `sessionStorage`**, sob a chave `delivery.sessao`, como
`{ token, expiraEm }`.

E quatro regras que vêm com ela:

### 1 · Um arquivo só toca o armazenamento

`frontend/src/auth/armazenamento.ts`. Uma regra de ESLint
(`no-restricted-globals`) proíbe `localStorage` em todo o projeto, e outra proíbe
`atob`.

Isto é o mesmo argumento da emenda de 26/09 à ADR-001, que virou task de build em
vez de comentário: **uma decisão de segurança que depende de todo mundo lembrar
não é decisão, é esperança.**

### 2 · O token é opaco

Nada no front decodifica o JWT. A validade vem do `expiresIn` do login — 1800 —,
e não de um `exp` lido do token. Ler claim no cliente é o primeiro passo para
confiar em claim no cliente.

### 3 · Todo acesso ao armazenamento tolera indisponibilidade

Em aba privada, com dados de site bloqueados ou dentro de um iframe de outra
origem, o acesso **estoura** em vez de devolver vazio. Cada ponto tem `try`, e o
front funciona sem guardar nada: a pessoa reloga a cada recarga, o que é ruim e
não é quebrado.

### 4 · Token expirado é apagado na leitura, não escondido

Quem lê a sessão e encontra `expiraEm` no passado **remove a entrada** e responde
como se não houvesse nada. Devolver vazio sem limpar faria a requisição seguinte
sair com o token morto, tomar 401, e a tela de sessão expirada aparecer depois de
um instante de painel vazio.

E o 401 de qualquer requisição apaga a sessão no mesmo lugar. São dois caminhos
para o mesmo fim — o temporizador é a cortesia, o 401 é a rede — e o segundo é o
que vale, porque o relógio do cliente pode estar errado.

## Consequências

**Positivas**

- **Fechar a aba encerra a sessão**, o que para um painel operado em balcão ou em
  computador compartilhado é o comportamento que se quer.
- **Nada vaza entre abas nem entre janelas.** Duas lojas abertas em duas abas não
  se misturam — o que vai importar quando houver troca de loja.
- **Recarregar a página não desloga**, e num painel que é recarregado o dia todo
  isso é a diferença entre usável e irritante.
- **A superfície é pequena e verificada por lint.** Um arquivo, duas regras.

**Negativas**

- **`sessionStorage` é legível por JavaScript**, e portanto por um XSS bem
  colocado. Não há como fingir o contrário: a defesa real é o React não injetar
  HTML, e é CSP — que **esta ADR não decide** e que fica como pendência abaixo.
  A escolha entre `localStorage` e `sessionStorage` não muda esse risco; muda só
  por quanto tempo a janela fica aberta.
- **Abrir uma segunda aba exige login outra vez.** Consequência direta de não
  vazar entre abas, e é o preço aceito.
- **Trinta minutos continuam sendo trinta minutos.** Esta ADR não conserta a
  falta de refresh; ela só garante que a expiração seja tratada em vez de
  descoberta no meio de um formulário.

## Alternativas consideradas

- **Só em memória.** O mais seguro: nada persiste, e um XSS não encontra token
  guardado. **Rejeitada** porque recarregar a página deslogaria, e o painel de um
  comerciante é aberto e recarregado o dia todo — somado aos trinta minutos sem
  refresh, o custo de uso ficaria alto para um ganho que a falta de CSP anula em
  boa parte. Se um dia houver refresh token em cookie `HttpOnly`, **esta é a
  escolha certa** e esta ADR deve cair.
- **`localStorage`.** O que quase todo front faz. **Rejeitada** porque sobrevive
  a fechar o navegador e, aqui, isso só significa guardar um token expirado —
  paga o risco de persistência sem comprar nada, já que não há refresh para
  aproveitá-la.
- **Cookie `HttpOnly` emitido pelo `identity`.** A escolha correta em termos de
  exposição a XSS. **Rejeitada nesta rodada, não em geral:** exigiria o
  `identity` emitindo `Set-Cookie`, o gateway com `allowCredentials: true`, CSRF
  de volta (porque cookie é enviado automaticamente) e a ADR-044 emendada — cuja
  cadeia de filtros desliga o CSRF com o comentário, na `SecurityConfig` do
  gateway, *"CSRF protege sessão em navegador, e não há sessão"*. É uma decisão de arquitetura de autenticação, e ela vem junto com o
  refresh token, não antes.

## Pendência que esta ADR cria

**Não há CSP.** A defesa real contra XSS é uma Content-Security-Policy, e este
repositório não tem nenhuma — nem no `index.html`, nem em cabeçalho de resposta
no gateway. Enquanto isso, a consequência negativa acima está aberta, e a
diferença entre `localStorage` e `sessionStorage` é menor do que parece.

**Gatilho escrito:** o primeiro ambiente alcançável de fora da máquina de
desenvolvimento. Aí a CSP entra com ADR própria, junto com a decisão de quem a
emite — a tag `meta` do `index.html` ou o gateway —, e esta pendência fecha.

Fica registrado como *"peça que nunca rodou não é peça, é intenção"* aplicado ao
contrário: a peça que **falta** e cuja falta está escrita.
