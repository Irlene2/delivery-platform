# Evolução do AKI Delivery

## Etapas de arquitetura

[x] 1. Backend
[x] 2. Banco de dados — estrutura PostgreSQL preparada
[x] 3. Login e autenticação reais
[x] 4. Cadastro de usuários
[x] 5. Cadastro de produtos
[x] 6. API de pedidos
[x] 7. Estrutura de pagamentos
[x] 8. Estrutura WhatsApp Business
[x] 9. Localização do entregador
[x] 10. Estrutura para mapas e rotas
[x] 11. Notificações em tempo real
[x] 12. Histórico de pedidos
[x] 13. Relatórios financeiros
[x] 14. Dashboard com dados da API
[x] 15. Sistema de permissões

## Revisão frontend atual

[x] Cadastro visual de cliente conectado à API
[x] Área Minha Conta
[x] Logout real no frontend
[x] Identificação dinâmica do usuário logado
[x] Catálogo preparado para carregar produtos do PostgreSQL via API
[x] Checkout preparado para criar pedido na API
[x] Formas de pagamento convertidas para os códigos do banco
[x] Remoção da dependência de pedidos fictícios no dashboard
[x] Número do pedido gerado pelo banco
[x] Histórico de status com trigger no banco
[x] Auditoria e carrinho persistente preparados no banco

## Próximos refinamentos funcionais

[ ] Tela administrativa completa de usuários/permissões
[ ] CRUD visual completo de produtos e categorias
[ ] CRUD visual de endereços do cliente
[ ] Recuperação de senha por e-mail/WhatsApp
[ ] Integração OAuth Google/Apple
[ ] Gateway de pagamento real
[ ] WhatsApp Business API real
[ ] Google Maps/rotas real
[ ] GPS do entregador via dispositivo
[ ] Push notifications


## Frontend aberto — 27/09/2026

[x] Removida a exigência de login para navegação do frontend
[x] Criado acesso direto aos quatro perfis
[x] Criado acesso direto ao cardápio
[x] Mantido backend separado para futura autenticação/banco
[x] Mantido HTML + CSS + JS como frontend principal

## Imagens locais do cardápio — 30/09/2026

[x] Criada a pasta `assets/cardapio/`
[x] Adicionadas 24 imagens locais em SVG, sem dependência de CDN externa
[x] Produtos do frontend apontando para os arquivos locais
[x] Seed do PostgreSQL atualizado para os caminhos locais das imagens
[x] Banner do cardápio ajustado para usar asset local
[x] Removida a dependência das imagens externas no carregamento do cardápio

## Revisão rigorosa do front-end e imagens locais — 02/10/2026

[x] Mantido o `index.html` como tela inicial de login.
[x] Mantida a seleção dos perfis Cliente, Administrador, Colaborador e Entregador.
[x] Criada/reorganizada a pasta única `assets/cardapio/` para os assets visuais do cardápio.
[x] Removidas as antigas imagens SVG ilustrativas do cardápio.
[x] Adicionadas 24 imagens PNG locais em estilo fotográfico, com resolução padronizada de 900x650.
[x] Aproveitados visuais fotográficos equivalentes já disponíveis nos materiais do projeto.
[x] Para produtos sem equivalente adequado, usados visuais fotográficos gerados/combinados para manter aparência realista.
[x] Banner do cliente/cardápio movido para `assets/cardapio/banner-hamburguer.jpg`.
[x] Corrigidos todos os caminhos das imagens no catálogo.
[x] IDs dos 24 produtos reorganizados de 1 a 24, sem alterar nomes, preços ou categorias.
[x] Corrigida a autenticação local: páginas internas sem sessão retornam ao login.
[x] `auth.js` incluído nas páginas internas que utilizam sessão.
[x] Botões de saída adicionados aos perfis operacionais.
[x] Adicionado estilo visual para o botão de saída.
[x] Verificados scripts JavaScript com `node --check`.
[x] Verificadas referências locais de HTML/CSS/JS e ausência de caminhos de imagem quebrados.
[x] Verificada a existência das 24 imagens do catálogo e do banner.


## Ajustes solicitados — 17/10/2026

[x] Removido o seletor de tipo de acesso da tela de login demonstrativa
[x] Cadastro aceita e-mail ou telefone (ao menos um é obrigatório)
[x] `index.html` abre a página inicial para visualização direta
[x] Login e cadastro mantidos como páginas independentes, sem integração nesta etapa
[x] Removido bloqueio de visualização das páginas por falta de sessão
[x] Imagens do cardápio organizadas em `assets/cardapio/`


## Ajuste de visualização sem autenticação — 17/10/2026

- Removida qualquer dependência de validação de senha na tela de login demonstrativa.
- A tela de login não possui seletor de tipo de acesso/perfil.
- O botão da tela de login abre a página inicial de demonstração, sem criar sessão.
- `index.html` continua abrindo diretamente a área do cliente.
- As páginas internas permanecem acessíveis diretamente, sem redirecionamento para login.
- Cadastro aceita e-mail ou telefone e valida que pelo menos um dos dois seja informado.
- Atualizada a documentação para refletir que login/cadastro não estão integrados a backend.


## Login na página inicial e backend independente — 17/10/2026

[x] Restaurado o layout de login como `index.html`
[x] Login demonstrativo: sem validação de senha, sem autenticação e sem bloqueio das outras páginas
[x] Links do login ajustados para a estrutura da raiz
[x] Criada pasta `backend/` com API Express de referência, separada do frontend
[x] Documentado que frontend e backend ainda não estão integrados
