# Implementação operacional dos Marcos 3 a 6 — AKI Delivery

## Entrega desta revisão
Foi adicionada a página `aki-frontend/pages/operacao.html`, ligada ao menu do painel administrativo, e sua lógica em `aki-frontend/js/operacao.js`.

### Funcionalidades disponíveis no modo piloto local
- Registro de pedidos com cliente, telefone, descrição dos itens, total, taxa de entrega e meio de pagamento.
- Ciclo de preparo: Recebido, Em preparo, Pronto, Saiu para entrega, Entregue e Cancelado.
- Registro manual de pagamento pendente/confirmado e valor recebido para cálculo de troco.
- Cadastro de entregadores com telefone, diária e comissão por entrega.
- Atribuição de entregador a pedido.
- Fechamento de expediente condicionado à conclusão ou cancelamento de todos os pedidos.
- Extrato congelado com vendas, valores confirmados/pendentes, pedidos e remuneração calculada por entregador.
- Abertura de novo expediente sem apagar o histórico dos fechamentos anteriores.
- Persistência local via `localStorage` no navegador.

## Limitações que impedem declarar os marcos concluídos em produção
Esta entrega é um piloto funcional de interface, não substitui os serviços de backend. Os dados ficam no navegador e não são compartilhados entre computadores/usuários nem gravados no PostgreSQL. O pagamento Pix é confirmado manualmente; não existe confirmação bancária automática. Não existe ainda trilha de auditoria imutável no servidor, controle transacional de concorrência, autenticação/autorização por função nesta tela, integração fiscal ou conciliação bancária. A remuneração é calculada por diária e comissão fixa por entrega concluída, sem regras contratuais mais sofisticadas.

Os módulos Java `order-service`, `payment-service`, `delivery-service` e `settlement-service` continuam precisando de implementação de domínio, persistência, migrações, APIs, testes de integração e roteamento pelo gateway. Portanto, Marcos 3–6 ficam **demonstráveis no piloto local**, mas **não certificados como concluídos para operação comercial**.

## Verificações executadas
- `node --check aki-frontend/js/operacao.js`: aprovado.
- Verificação das referências locais da nova página: executada na revisão do pacote.
- ZIP validado com `unzip -t`.

## Como testar
Sirva a pasta `aki-frontend` por HTTP (por exemplo `python -m http.server 5500`) e abra `http://localhost:5500/pages/operacao.html`. Crie um pedido, avance os estados, cadastre um entregador, atribua-o ao pedido, confirme o pagamento, marque como entregue e feche o expediente.
