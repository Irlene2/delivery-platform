# Integração AKI Delivery com delivery-platform

## O que foi conectado

O front-end estático original foi preservado na pasta `aki-frontend/`; o front React/Vite original do repositório permanece em `frontend/`.

| Tela / recurso AKI | Contrato utilizado |
|---|---|
| Login por telefone | `POST /api/v1/auth/login` |
| Solicitação de código | `POST /api/v1/auth/verification-code` |
| Cadastro por telefone + código | `POST /api/v1/auth/signup` |
| Listagem/criação de estabelecimentos | `GET/POST /api/v1/me/estabelecimentos` |
| Listagem/criação de categorias | `GET/POST /api/v1/merchants/{estabelecimentoId}/catalog/categorias` |
| Listagem/criação de produtos | `GET/POST /api/v1/merchants/{estabelecimentoId}/catalog/produtos` |
| Publicar produto | `PUT /api/v1/merchants/{estabelecimentoId}/catalog/produtos/{produtoId}/publicacao` |
| Disponibilidade | `PUT /api/v1/merchants/{estabelecimentoId}/catalog/produtos/{produtoId}/disponibilidade` |

A integração segue os contratos OpenAPI versionados em `contracts/openapi/`. O token é guardado em `sessionStorage` e enviado no cabeçalho `Authorization: Bearer`. A sessão é removida quando a API devolve 401. Não há refresh token no contrato atual.

## Como executar localmente

1. Instale JDK 21 e Docker/Compose conforme o `README.md` principal.
2. Copie `.env.example` para `.env`; preencha as credenciais locais do Postgres/RabbitMQ e gere as chaves JWT conforme as instruções do README. Não coloque segredos no Git.
3. Inicie o perfil recomendado do projeto (`marco2`) com Docker Compose, seguindo as etapas documentadas pelo repositório. Não é necessário subir todos os serviços para testar identidade, estabelecimento e catálogo.
4. Em outro terminal, entre em `aki-frontend/` e execute `python -m http.server 5500` (ou sirva a pasta por outro servidor HTTP).
5. Abra `http://localhost:5500/`. A API padrão usada pelo front é `http://localhost:8080`.
6. Se usar outra origem, inclua-a em `CORS_ALLOWED_ORIGINS` e reinicie o gateway.

Não abra as páginas usando `file://`; o navegador precisa servir os arquivos por HTTP.

## Limites atuais da integração

O repositório de origem declara que os serviços de pedidos, pagamento, entrega, liquidação e conversa ainda são esqueleto. Por isso, as telas AKI de checkout, histórico de pedidos, entregador, pagamentos, estoque e indicadores permanecem demonstrativas e não foram conectadas a endpoints fictícios. A página `aki-frontend/pages/cardapio.html` continua sendo a experiência visual de cardápio para cliente; `aki-frontend/pages/catalogo-gestao.html` é o catálogo conectado de gestão do estabelecimento.

O contrato de identidade aceita telefone, nome, código e senha; não aceita e-mail. O e-mail opcional do formulário de cadastro é apenas informativo e não é persistido. O contrato atual de produto não possui campo de imagem, então as imagens locais do cardápio não são enviadas ao backend. Login social e recuperação de senha também não existem nos contratos disponíveis.

## Dados e persistência observados no repositório

- PostgreSQL: identidade e estabelecimento/loja têm migrations SQL em seus respectivos serviços e bancos lógicos separados.
- MongoDB: catálogo de produtos/categorias é persistido pelo `catalog-service`; suas migrations são implementadas no serviço.
- O README de origem documenta oito bancos lógicos isolados por serviço e proíbe acesso direto ao banco de outro serviço.
- As pastas de migration de pedido, pagamento e entrega ainda contêm `.gitkeep`; não foi identificado schema relacional operacional para esses domínios no estado do ZIP analisado.
- O esquema SQL demonstrativo que existia no scaffold anterior do AKI não é usado por esta integração e não deve ser aplicado sobre os bancos do `delivery-platform`.

## Verificações realizadas

Foram feitas verificações estáticas de sintaxe JavaScript e de caminhos locais do front. A integração não pôde ser validada contra serviços ativos nem contra bancos em execução nesta sessão; a confirmação final exige iniciar o ambiente com as variáveis e segredos locais e testar login, criação de loja e operações do catálogo.
