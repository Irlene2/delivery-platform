# conversation-service — planejado, ainda sem código de negócio

**Este serviço não faz parte da entrega de 15/11/2026.** É um esqueleto: a classe de entrada, as
pastas das camadas da arquitetura hexagonal, o teste de arquitetura e o pipeline de CI. Não sobe
nos perfis `marco2` e `pedidos` do Compose.

| | |
|---|---|
| Porta | 8088 |
| Banco | MongoDB (`conversation_db`) |
| Vai cuidar de | o **pedido nascendo dentro da conversa do WhatsApp** (ADR-075, marco 7) e, depois, o **atendimento com IA** (marco 9) |
| As regras desenhadas | [`conversa.md`](../../../docs/dominio/conversa.md) |

Para a entrega de 15/11, o pedido nasce pelo link do cardápio (ADR-077), e o WhatsApp Business da loja só manda o link na resposta automática.

Está no repositório para que o próximo marco comece pela regra de negócio, e não pela estrutura.
O lugar dele na arquitetura está em [`docs/architecture/containers.md`](../../../docs/architecture/containers.md).
