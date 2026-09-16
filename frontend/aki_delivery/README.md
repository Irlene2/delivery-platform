# AKI Delivery — Cliente + Cardápio + Carrinho + Pedidos

Projeto estático em HTML, CSS e JavaScript com navegação integrada entre:

- Início / área do cliente
- Cardápio visual com categorias, busca e fotografias de referência da internet
- Carrinho compartilhado entre as páginas via localStorage
- Meus Pedidos com histórico e ticket
- Minha Conta
- Acompanhamento visual do pedido e trajeto do entregador

## Fluxo

`cliente.html` → `cardapio.html` → carrinho → finalizar → `pedidos.html` → acompanhamento em `cliente.html#trajeto`.

As fotos do cardápio utilizam URLs de imagens hospedadas externamente (Unsplash). Em produção, recomenda-se baixar imagens licenciadas/autorizadas para o diretório `assets/` e substituir as URLs por arquivos locais.
