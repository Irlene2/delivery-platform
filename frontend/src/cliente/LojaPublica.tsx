import { useEffect, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router';
import { carrinhoDaLoja, lembrarPedido, limparCarrinho } from '../aki/pedidos';
import { toast } from '../aki/toast';
import type { PedidoPublico } from '../api/pedidos';
import { vitrineDe } from '../pedido/fontes';
import { ConteudoDoCardapio } from './Cardapio';
import { CascaDaVitrine } from './CascaDaVitrine';

/**
 * `/loja/:estabelecimentoId` — o link que a loja manda ao cliente (ADR-077). Público: o
 * cardápio vem da rota sem token do `catalog`, e o carrinho fecha um pedido de verdade no
 * `order`. Com `demo` no lugar do identificador, é o mesmo fluxo com os dados de exemplo.
 */
export function LojaPublica() {
  const { estabelecimentoId = '' } = useParams();
  const navegar = useNavigate();
  const vitrine = useMemo(() => vitrineDe(estabelecimentoId), [estabelecimentoId]);

  useEffect(() => carrinhoDaLoja(estabelecimentoId), [estabelecimentoId]);

  function criado(pedido: PedidoPublico) {
    lembrarPedido({
      codigo: pedido.codigoDeAcompanhamento,
      estabelecimentoId,
      numero: pedido.numero,
      total: pedido.total,
      criadoEm: pedido.criadoEm,
    });
    limparCarrinho();
    toast(`Pedido #${pedido.numero} enviado à loja`);
    void navegar(`/pedido/${pedido.codigoDeAcompanhamento}`);
  }

  return (
    <CascaDaVitrine
      estabelecimentoId={estabelecimentoId}
      atual="cardapio"
      titulo="Cardápio"
      subtitulo="Escolha seus favoritos e monte seu pedido."
    >
      <ConteudoDoCardapio
        fonte={vitrine.cardapio}
        linkDoCardapio={`/loja/${estabelecimentoId}`}
        linkDosPedidos={`/loja/${estabelecimentoId}/pedidos`}
        vitrine={vitrine}
        aoCriarPedido={criado}
        textoSemProdutos="Esta loja ainda não publicou o cardápio. Volte mais tarde."
      />
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Cardápio online
      </footer>
    </CascaDaVitrine>
  );
}
