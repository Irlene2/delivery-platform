import type { VitrineDaLoja } from '../api/pedidos';
import { quandoAbre } from '../pedido/regras';

/**
 * A loja fechada, dita antes de o cliente montar o carrinho (ADR-077, emenda de 01/11/2026). Usa
 * o bloco `.support` do protótipo — o mesmo da ajuda no acompanhamento —, sem classe nova.
 *
 * Só aparece com `aberta === false`: nulo é "não deu para confirmar", e aí o pedido decide.
 */
export function AvisoDeLojaFechada({ vitrine }: { vitrine: VitrineDaLoja | null }) {
  if (vitrine?.aberta !== false) return null;
  const quando =
    vitrine.proximaAbertura != null
      ? `Abre ${quandoAbre(vitrine.proximaAbertura)}. Você já pode ver o cardápio.`
      : 'A loja não está recebendo pedidos agora. Você pode ver o cardápio.';
  return (
    <div className="support" role="status" style={{ marginBottom: '16px' }}>
      <div className="support-icon">
        <span className="material-symbols-outlined">schedule</span>
      </div>
      <div>
        <strong>Loja fechada agora</strong>
        <p>{quando}</p>
      </div>
    </div>
  );
}
