# ADR-076 — O convite aceita antes de entrar

- **Status:** aceita
- **Data:** 19/10/2026
- **Rodada:** W-I
- **Relacionada:** ADR-069 (o convite chega pela mão de quem convida), ADR-070 (a tela do
  convite — emendada por esta), ADR-047 (a sessão), ADR-016 (front mínimo), e o
  `docs/front/premissas-do-front.md`

## Contexto

**A tela do convite pedia duas intenções para uma coisa.** Sem sessão, ela oferecia *Entrar* e
*Criar conta*; com sessão, *Aceitar convite*. **O retorno já funcionava** — o `state.de` leva de
volta à mesma URL, e a tela anunciava isso —, e **a ordem é que estava invertida**: a pessoa
declarava que queria entrar na conta, não que queria entrar na equipe. A ordem foi decidida na
ADR-070 §4: *"Quem não tem sessão vê entrar e criar conta, e volta para esta mesma URL. Quem tem
vê o botão de aceitar."*

**Visto no navegador em 19/10/2026**, na passagem registrada em
`docs/operacao/passagem-humana.md`: duas telas, a mesma URL, e a frase de quem passou — *"só
aceita o convite depois de logado e isso NÃO é para ser esperado"*.

**E a volta não levava intenção nenhuma.** A `EntrarPage` e a `CadastrarPage` liam o `de` do
`state` e voltavam a ele com `navegar(destino, { replace: true })`, **sem `state`**: o que
viesse ao lado do `de` se perdia no caminho de volta.

## Decisão

**A ação única da tela do convite é aceitar.** Sem sessão, *Aceitar convite* leva ao login, de
onde o cadastro se alcança pelo link que já existia; **a intenção viaja no `state` do
roteador**, ao lado do caminho de volta, com nome próprio; e **na volta o convite é aceito sem
segundo clique**.

**A intenção tem nome e não é booleano.** `acao: 'aceitar-convite'` — uma marca booleana só
serviria a esta tela.

**A intenção volta com o destino.** A `EntrarPage` e a `CadastrarPage` passam a voltar ao `de`
levando a `acao`, quando ela veio. Sem isso, a decisão acima não acontece.

**A intenção sozinha não autoriza nada.** O aceite automático exige **sessão e intenção**; sem
sessão, a tela volta a pedir o aceite, e **nada é enviado**. A marca atravessa o login como
pedido, nunca como permissão.

**Quem chega com sessão e sem intenção continua vendo o botão.** Abrir a URL não é pedir para
entrar na equipe.

**O aceite automático acontece uma vez.** Se o servidor recusar, a recusa vai à tela como
viria pelo botão, o botão volta, e nada tenta de novo sozinho.

## Consequências

**A pessoa convidada declara o que quer antes de provar quem é.**

**A intenção mora no histórico daquela aba.** O `BrowserRouter` guarda o `state` em
`window.history.state` (`react-router` 7.18.4, medido no código resolvido): ele **sobrevive a
recarregar** a mesma entrada e a voltar até ela, e **não chega** a um link aberto de novo nem a
outra aba. Depois do aceite, a navegação com `replace` tira da aba a entrada que a carregava.

**A intenção é da aba, não da pessoa.** Se, na mesma aba, alguém clicar *Aceitar convite*,
desistir, e outra pessoa entrar na conta dela ali, quem entrou é quem aceita — a guarda é sessão
e intenção, e as duas estão presentes.

**E o convite continua de uso único, sem lista de pendentes.** Esta decisão **não** muda isso:
quem perde o link perde o convite, e quem convidou não tem onde vê-lo. **O que mudou é o número
de telas entre receber e entrar.**

## Alternativas consideradas

**Aceitar sozinho sempre que houver sessão.** Recusada: quem abre a URL com sessão por
curiosidade entraria na equipe sem pedir.

**Guardar a intenção no `sessionStorage`, ao lado da sessão.** Recusada: ela seguiria **a
navegações que não têm nada a ver com o convite**, e a limpeza dela seria um segundo mecanismo.
O histórico da aba já carrega o caminho de volta, por entrada; carregar a intenção no mesmo
lugar é um mecanismo só.

**Pôr a intenção na URL de volta** (`/convite/:token?aceitar=1`). Recusada: dispensaria mexer
no login e no cadastro, e transformaria a intenção em parte de um endereço que se copia e se
cola.

**Aceitar no servidor antes do login, pelo código do link.** Recusada: o aceite cria vínculo, e
vínculo sem identidade é o que a ADR-069 recusou quando o convite nasceu.

## Gatilho escrito

**A primeira pessoa que pedir o reenvio de um convite.** Aí nascem a lista de pendentes e o link
que não morre no primeiro uso, no `merchant`.

**A primeira pessoa que quiser recusar um convite**, em vez de só não aceitá-lo. Aí a tela deixa
de ter uma ação única e a recusa precisa de rota.
