# Histórico de versões

O que entrou em cada fase do projeto, da mais nova para a mais antiga. Cada linha remete aos
commits da `main` (`git log --oneline`) e às decisões em
[`docs/architecture/decisions/`](docs/architecture/decisions/README.md). Os marcos são os do
[`docs/PRD.md`](docs/PRD.md) §10, resumidos no [README](README.md#marcos).

Só a v1.0.0 tem tag. As anteriores são as fases do desenvolvimento, nomeadas aqui para
facilitar a leitura do histórico.

## v1.0.0 — 15/11/2026 · a entrega do curso

A versão avaliada, marcada na tag `v1.0.0`. O recorte é o da ADR-077: o pedido nasce pelo link do
cardápio que a resposta automática do WhatsApp Business entrega.

- **Ajustes dos testes no celular (13/11):** excluir produto, tirar do cardápio, o selo e o "pode
  acabar" gravados pelo painel; Termos de uso e Aviso de privacidade (`/termos` e `/privacidade`);
  textos que quebram linha e o menu lateral que fecha no celular.
- **Repositório para a avaliação (09 e 15/11):** o guia de teste para o time
  ([`docs/como-testar.md`](docs/como-testar.md)), a limpeza do que não seria usado, as ADRs
  revistas depois da mudança de rumo, os links da entrega e a documentação das APIs no README.
- **Hospedagem de teste (07 e 08/11):** o túnel da Cloudflare para abrir no celular, a passagem
  completa da loja nova ao caixa fechado e o vídeo de 1 minuto.

## v0.6 — 01 e 02/11/2026 · marcos 3 a 6: do pedido ao caixa fechado

- O caixa do dia apurado e gravado, com acerto por entregador, conferência do dinheiro,
  histórico, vendas por período e planilha.
- O sino de notificações, o Pix informado pelo entregador e conferido pela loja, o estoque com
  busca e filtros.
- "Esqueci minha senha" pelo código de verificação, entrada só pelo telefone, a foto de cada item
  no acompanhamento.
- Loja fora do horário não recebe pedido; corrigir produto, foto do produto, sessão de 12 horas.
- A loja de teste é a "Pizzaria da Fixture", com o cardápio do protótipo; o pagamento pelo app
  sai, e o pedido passa a registrar como foi pago (ADR-023).

## v0.5 — 26 a 31/10/2026 · o pedido de verdade

- O `order-service`: o pedido e seus estados, a taxa da loja, o entregador da equipe com conta
  própria e as entregas dele (ADR-077).
- O cardápio público e as rotas da vitrine no `catalog` e no `gateway`.
- O front com o visual do protótipo `aki_delivery` em React: cardápio pelo link, checkout,
  acompanhamento, painel de pedidos, dashboard e o app do entregador.

## v0.4 — 10 a 19/10/2026 · marco 2: a loja monta e publica o cardápio

- O marco 2 de pé no Docker Compose (perfil `marco2`), com limite de memória por contêiner.
- A loja nasce pela tela, com o fundador; seções e produtos criados por rota e publicados por
  quem monta o cardápio.
- A equipe com convite por link, o horário da semana e a reativação do que esgotou, pelo evento
  de abertura do expediente.

## v0.3 — 24 a 30/09/2026 · o gateway e o catálogo

- O gateway com a cadeia de filtros e o roteamento por recurso (ADR-012).
- O modelo do catálogo (produto, grupo, opção, categoria e disponibilidade), a persistência no
  MongoDB e a primeira rota protegida, com a autorização atravessando dois serviços.
- Outbox e o primeiro consumidor de evento; a documentação viva (Swagger), fechada por padrão
  (ADR-050).
- A fundação do front: entrar, cadastrar e o painel.

## v0.2 — 14 e 15/09/2026 · marco 1: a loja e o time

- Cadastro com código de verificação por telefone (ADR-042).
- A equipe da loja: vínculo, papéis, convite e aceite; áreas de entrega.

## v0.1 — agosto e setembro de 2026 · marco 0: o esqueleto

- O monorepo, o build Gradle multi-projeto com convention plugins, os serviços com arquitetura
  hexagonal e o teste que a vigia, o Docker Compose, os pipelines de CI por serviço e a
  varredura de segredos.
