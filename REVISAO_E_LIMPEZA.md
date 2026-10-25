# Revisão e limpeza do projeto AKI Delivery + delivery-platform

## Limpeza aplicada

- Removida `aki-frontend/backend/`, que era um scaffold Node/Express demonstrativo, sem persistência e sem integração, incompatível com o backend real do projeto. A pasta podia induzir a execução da API errada; o backend oficial permanece em `backend/` (Java/Spring Boot, multi-módulo).
- Mantidos `frontend/` (painel React/Vite original) e `aki-frontend/` (front-end estático AKI), pois são duas interfaces diferentes, não cópias idênticas.
- Mantidas as migrations, contratos, documentação, arquivos de configuração e exemplos de ambiente necessários ao monorepo.
- Nenhum arquivo `.env` com segredos locais foi adicionado ao pacote.

## Verificações executadas

- `node --check` em todos os arquivos JavaScript do `aki-frontend/`: aprovado.
- Teste de integração isolado com `fetch` simulado: login guarda o `accessToken`, chamadas autenticadas enviam `Authorization: Bearer`, e resposta 401 invalida a sessão: aprovado.
- Corrigida a leitura das listas paginadas do catálogo: a API usa a propriedade `conteudo`; o adaptador do front agora também aceita `conteudo` (além de `content` e `itens`).
- Melhorado o tratamento de erros HTTP (mensagem `detail`/`message`/`title` e mensagens específicas para 401, 403, 404 e 5xx). O login agora rejeita respostas sem `accessToken` válido.
- Referências locais `href` e `src` dos HTML do `aki-frontend/`: nenhuma referência ausente encontrada.
- Duplicatas byte a byte: nenhuma encontrada no inventário do projeto antes da remoção do scaffold obsoleto. Arquivos com nomes ou funções parecidas foram preservados quando pertencem a aplicações, serviços ou ambientes diferentes.
- Integridade do arquivo ZIP: verificada após a criação.

## Limites desta revisão

- Não foi executado o stack Docker, nem foram iniciados serviços/bancos reais; portanto, não é uma confirmação de teste end-to-end.
- O build e os testes automatizados do React/Vite não puderam ser executados: a instalação de dependências foi interrompida e o binário `vitest`/tipos TypeScript não ficaram disponíveis.
- O build e os testes Gradle do backend Java não puderam ser executados: o Gradle Wrapper precisa baixar a distribuição e o ambiente não tem acesso à rede (`UnknownHostException: services.gradle.org`). Docker/Compose também não está instalado nesta sessão.
- Portanto, os testes de contrato HTTP com serviços ativos e o teste end-to-end com bancos reais continuam pendentes. A integração do AKI com identidade, estabelecimentos e catálogo depende das variáveis de ambiente, chaves JWT, serviços e bancos locais descritos em `INTEGRACAO_AKI.md`.
- Os serviços de pedidos, pagamento e entrega ainda são esqueleto no repositório de origem; as telas correspondentes do AKI permanecem demonstrativas, conforme documentado.

## Como abrir o front-end AKI

Sirva a pasta `aki-frontend/` por HTTP, por exemplo com `python -m http.server 5500`, e abra `http://localhost:5500/`. A API padrão do gateway é `http://localhost:8080`.
