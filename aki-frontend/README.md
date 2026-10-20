# AKI Delivery — Front-end

Versão **somente frontend** do AKI Delivery, construída com HTML, CSS e JavaScript puro.

## O que está incluído

- Página inicial e acesso de demonstração
- Perfis de Cliente, Administrador, Colaborador e Entregador
- Cardápio completo
- 24 produtos com imagens locais em `assets/cardapio/`
- Busca e filtro por categoria
- Detalhes do produto
- Carrinho com quantidades e cálculo de valores
- Checkout demonstrativo com Pix, cartões, dinheiro e vale-refeição
- Criação de pedidos localmente no `localStorage`
- Histórico de pedidos local
- Dashboard e telas administrativas em modo demonstração
- Conta e endereços em modo frontend
- Tema claro/escuro
- Layout responsivo
- Cadastro demonstrativo salvo localmente
- Nenhum servidor, banco de dados ou API é necessário para abrir esta versão

## Como executar

Abra `index.html` para ver a tela de login demonstrativa. Nenhuma senha é validada e ela não bloqueia as outras telas. Para visualizar diretamente, abra qualquer arquivo em `pages/` (por exemplo, `pages/cliente.html` ou `pages/cardapio.html`).

Para uma experiência melhor, também é possível usar uma extensão de servidor local, como Live Server.

## Estrutura

```text
assets/
  cardapio/
css/
js/
pages/
index.html
README.md
EVOLUCAO.md
```

## Observação

Esta versão é exclusivamente frontend. Autenticação real, banco PostgreSQL, API, pagamentos reais, WhatsApp Business, localização em tempo real e demais integrações ficam fora desta edição.

## Tela inicial e login

O `index.html` abre diretamente a área do cliente para visualizar o sistema sem passar por login. A tela visual de login fica em `pages/login.html`, mas não valida nem salva senha. Nenhuma página exige autenticação: abra diretamente os HTML dentro de `pages/`.

## Imagens do cardápio — revisão 02/10/2026

Todos os assets visuais do cardápio ficam centralizados em `assets/cardapio/`.

A pasta contém o banner e 24 imagens individuais dos produtos em PNG. O catálogo não depende de imagens externas para os produtos. Os visuais fotográficos foram selecionados a partir dos materiais visuais disponíveis no projeto e, quando não havia equivalente adequado, foram gerados/combinados para manter aparência realista.

## Front-end sem backend

Esta versão é exclusivamente HTML, CSS e JavaScript. O carrinho e os pedidos demonstrativos podem usar `localStorage`. O login é independente e não autentica usuários. O cadastro valida os campos no navegador, aceita e-mail ou telefone e não exige backend. Não é necessário instalar Node.js ou PostgreSQL para abrir a interface.


## Modo de visualização atual

O `index.html` abre diretamente a página inicial visual do cliente para permitir avaliar o front-end sem login. A tela de login está isolada em `pages/login.html` e o cadastro em `pages/cadastro.html`; ambos são demonstrações independentes, sem integração de autenticação nesta etapa. O cadastro aceita e-mail ou telefone. As imagens do cardápio ficam em `assets/cardapio/`.


## Navegação sem login

- `index.html`: área inicial do cliente, abre diretamente.
- `pages/login.html`: tela de login demonstrativa, sem validação de senha.
- `pages/cadastro.html`: formulário de cadastro demonstrativo; aceita e-mail, telefone ou ambos, exigindo pelo menos um.
- As demais páginas em `pages/` podem ser abertas diretamente pelo navegador, sem redirecionamento para login.


## Backend separado (não integrado)

A pasta `backend/` contém uma API inicial independente. Ela não é chamada pelo frontend nesta versão. Consulte `backend/README.md` para executar a API e `backend/.env.example` para a configuração local.


## Integração com `delivery-platform`

Esta versão foi adaptada para usar o gateway do repositório `delivery-platform` quando ele estiver em execução:

- `index.html`: autentica via `POST /api/v1/auth/login` com telefone e senha; o token fica em `sessionStorage`.
- `pages/cadastro.html`: solicita código e cadastra via `POST /api/v1/auth/verification-code` e `/api/v1/auth/signup`. O contrato de identidade aceita telefone, nome, código e senha (mínimo 8 caracteres); o e-mail não é persistido pela API atual.
- `pages/catalogo-gestao.html`: lista/cadastra estabelecimento, categorias e produtos, publica produtos e altera disponibilidade usando as rotas reais de merchant/catalog.
- `pages/cardapio.html`, pedidos, pagamentos, entregas e painéis anteriores continuam disponíveis como interface demonstrativa. Os serviços de pedido, pagamento e entrega do repositório ainda são esqueleto e não devem ser tratados como fluxos operacionais integrados.

### Executar

1. Configure o `.env` e os segredos conforme o README principal do repositório.
2. Inicie a infraestrutura e os serviços de identidade, merchant, catalog e gateway conforme o perfil `marco2` documentado no projeto.
3. Sirva esta pasta por HTTP, por exemplo `python -m http.server 5500` dentro de `aki-frontend/`. Não abra o HTML diretamente como `file://`.
4. Configure `CORS_ALLOWED_ORIGINS` para incluir a origem usada para servir a interface (por exemplo `http://localhost:5500`) e reinicie o gateway.
5. Acesse `http://localhost:5500/`.

A URL padrão do gateway é `http://localhost:8080` e pode ser alterada definindo `window.AKI_API_BASE_URL` antes de carregar `js/platform-api.js`. O backend exige configuração real de PostgreSQL/MongoDB, chaves JWT e demais serviços; os arquivos de configuração não contêm credenciais reais.
