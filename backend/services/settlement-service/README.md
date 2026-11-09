# settlement-service — planejado, ainda sem código de negócio

**Este serviço não faz parte da entrega de 15/11/2026.** É um esqueleto: a classe de entrada, as
pastas das camadas da arquitetura hexagonal, o teste de arquitetura e o pipeline de CI. Não sobe
nos perfis `marco2` e `pedidos` do Compose.

| | |
|---|---|
| Porta | 8084 |
| Banco | PostgreSQL (`settlement_db`) |
| Vai cuidar de | a **jornada e a custódia do entregador**: abertura e fechamento do turno, o dinheiro que ele carrega e o extrato dele |
| As regras desenhadas | [`liquidacao.md`](../../../docs/dominio/liquidacao.md) |

O caixa do dia, que o desenho original punha aqui, foi feito no `order-service` para a entrega de 15/11 (`FechamentoDoDia`), junto com o repasse e o acerto do entregador. Este serviço volta à pauta se a loja precisar controlar o turno do entregador.

Está no repositório para que o próximo marco comece pela regra de negócio, e não pela estrutura.
O lugar dele na arquitetura está em [`docs/architecture/containers.md`](../../../docs/architecture/containers.md).
