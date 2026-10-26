import { ConteudoDoCardapio } from '../cliente/Cardapio';
import { CascaDaLoja } from './CascaDaLoja';
import { caminhoDaLoja } from './caminhos';
import { useLojaAtual } from './lojaAtual';

/** "Ver cardápio": o cardápio da loja como o cliente vê, com os produtos publicados. */
export function CardapioDaLoja() {
  const { estabelecimentoId, fonte } = useLojaAtual();
  const base = caminhoDaLoja(estabelecimentoId);
  return (
    <CascaDaLoja
      atual="cardapio"
      titulo="Cardápio"
      subtitulo="Os produtos publicados, como o cliente vê."
    >
      <ConteudoDoCardapio
        fonte={fonte}
        linkDoCardapio={`${base}/cardapio`}
        linkDosPedidos={`${base}#pedidos`}
      />
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Cardápio online
      </footer>
    </CascaDaLoja>
  );
}
