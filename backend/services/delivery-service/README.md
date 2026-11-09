# delivery-service — planejado, ainda sem código de negócio

**Este serviço não faz parte da entrega de 15/11/2026.** É um esqueleto: a classe de entrada, as
pastas das camadas da arquitetura hexagonal, o teste de arquitetura e o pipeline de CI. Não sobe
nos perfis `marco2` e `pedidos` do Compose.

| | |
|---|---|
| Porta | 8087 |
| Banco | PostgreSQL (`delivery_db`) |
| Vai cuidar de | o **rodízio de entregadores**, a posição do entregador e o **mapa da entrega** (marco 11) |
| As regras desenhadas | [`entrega.md`](../../../docs/dominio/entrega.md) |

Para a entrega de 15/11, o entregador e o despacho são campos do pedido, no `order-service`.

Está no repositório para que o próximo marco comece pela regra de negócio, e não pela estrutura.
O lugar dele na arquitetura está em [`docs/architecture/containers.md`](../../../docs/architecture/containers.md).
