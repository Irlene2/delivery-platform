import { useMemo } from 'react';
import { fonteDaDemonstracao } from '../painel/catalogo';
import { ConteudoDoCardapio } from './Cardapio';
import { CascaDoCliente } from './CascaDoCliente';

/**
 * O cardápio da área do cliente. O catálogo do servidor exige login de quem
 * vende, e ainda não há rota pública de cardápio: aqui é o de demonstração —
 * que mostra também o que foi salvo no cadastro de produtos em demonstração.
 */
export function CardapioDoCliente() {
  const fonte = useMemo(() => fonteDaDemonstracao(), []);
  return (
    <CascaDoCliente
      atual="cardapio"
      icone="carrinho"
      titulo="Cardápio"
      subtitulo="Escolha seus favoritos e monte seu pedido."
    >
      <ConteudoDoCardapio
        fonte={fonte}
        linkDoCardapio="/cliente/cardapio"
        linkDosPedidos="/cliente/pedidos"
      />
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Cardápio online
      </footer>
    </CascaDoCliente>
  );
}
