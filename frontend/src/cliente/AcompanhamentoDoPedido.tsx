import { Fragment, useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import type { PedidoPublico } from '../api/pedidos';
import { dataEHora, dinheiro } from '../aki/formatos';
import { usePedidosLembrados } from '../aki/pedidos';
import { toast } from '../aki/toast';
import { IMAGEM_PADRAO, mensagemDoErro } from '../painel/catalogo';
import { acompanhar } from '../pedido/fontes';
import {
  classeDoStatus,
  encerrado,
  etapasDoAcompanhamento,
  ROTULO_DA_FORMA,
  ROTULO_DA_MODALIDADE,
  ROTULO_DO_STATUS,
} from '../pedido/regras';
import { CascaDaVitrine } from './CascaDaVitrine';

/**
 * `/pedido/:codigo` — o cliente acompanha o pedido pelo código que recebeu (ADR-077). Público:
 * o código é um UUID aleatório, e a resposta não traz telefone nem endereço.
 *
 * Relê a cada {@link INTERVALO} ms enquanto o pedido não encerra. Sem WebSocket nesta entrega:
 * um pedido muda de status poucas vezes, e a leitura é barata.
 */
const INTERVALO = 15_000;

export function AcompanhamentoDoPedido() {
  const { codigo = '' } = useParams();
  const lembrados = usePedidosLembrados();
  const [pedido, definirPedido] = useState<PedidoPublico | null>(null);
  const [falha, definirFalha] = useState<string | null>(null);

  const ler = useCallback(() => {
    acompanhar(codigo)
      .then((lido) => {
        definirPedido(lido);
        definirFalha(null);
      })
      .catch((erro: unknown) => definirFalha(mensagemDoErro(erro)));
  }, [codigo]);

  useEffect(() => {
    ler();
  }, [ler]);

  const aberto = pedido !== null && !encerrado(pedido.status);
  useEffect(() => {
    if (!aberto) return;
    const relogio = window.setInterval(ler, INTERVALO);
    return () => window.clearInterval(relogio);
  }, [aberto, ler]);

  const loja =
    pedido?.estabelecimentoId ??
    lembrados.find((um) => um.codigo === codigo)?.estabelecimentoId ??
    '';

  async function copiarLink() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      toast('Link do acompanhamento copiado');
    } catch {
      toast('Não deu para copiar. Guarde o endereço desta página.');
    }
  }

  return (
    <CascaDaVitrine
      estabelecimentoId={loja}
      atual="pedidos"
      titulo="Acompanhar pedido"
      subtitulo="O andamento do seu pedido, atualizado sozinho."
    >
      {pedido === null ? (
        <section className="panel orders-history">
          <div className="ticket-empty" role={falha === null ? 'status' : 'alert'}>
            <span className="material-symbols-outlined">
              {falha === null ? 'hourglass_top' : 'search_off'}
            </span>
            <strong>
              {falha === null ? 'Carregando o pedido…' : 'Não encontramos este pedido'}
            </strong>
            {falha !== null && <p>{falha}</p>}
          </div>
        </section>
      ) : (
        <>
          <section className="orders-hero">
            <div>
              <span className="eyebrow">PEDIDO #{pedido.numero}</span>
              <h2>
                {pedido.status === 'CANCELADO'
                  ? 'Seu pedido foi cancelado.'
                  : encerrado(pedido.status)
                    ? 'Pedido finalizado. Bom apetite!'
                    : 'A loja já está com seu pedido.'}
              </h2>
              <p>
                {ROTULO_DA_MODALIDADE[pedido.modalidade]} · feito em {dataEHora(pedido.criadoEm)}
              </p>
            </div>
            <button type="button" className="btn btn-primary" onClick={() => void copiarLink()}>
              <span className="material-symbols-outlined">link</span>Copiar link do pedido
            </button>
          </section>

          <section className="panel tracking" id="pedido">
            <div className="section-title">
              <div className="title-icon">
                <span className="material-symbols-outlined">route</span>
              </div>
              <div>
                <small>ACOMPANHAMENTO</small>
                <h2>Acompanhe seu pedido</h2>
                <p>Esta página se atualiza sozinha enquanto o pedido anda.</p>
              </div>
            </div>
            <div className="tracking-order-head">
              <div>
                <span>Pedido atual</span>
                <strong data-current-order="">#{pedido.numero}</strong>
              </div>
              {aberto && (
                <div className="tracking-live">
                  <i /> Atualização automática
                </div>
              )}
            </div>
            {pedido.status === 'CANCELADO' ? (
              <div className="ticket-empty" role="alert">
                <span className="material-symbols-outlined">cancel</span>
                <strong>Pedido cancelado pela loja</strong>
                <p>Se tiver dúvida, fale com a loja pelo WhatsApp.</p>
              </div>
            ) : (
              <div className="timeline">
                {etapasDoAcompanhamento(pedido).map((etapa, i) => (
                  <Fragment key={etapa.rotulo}>
                    {i > 0 && (
                      <div className={`timeline-line${etapa.estado !== '' ? ' done' : ''}`} />
                    )}
                    <div
                      className={`timeline-step${etapa.estado === '' ? '' : ` ${etapa.estado}`}`}
                    >
                      <div className="timeline-icon">
                        <span className="material-symbols-outlined">{etapa.icone}</span>
                      </div>
                      <b>{etapa.rotulo}</b>
                      <p>{etapa.texto}</p>
                    </div>
                  </Fragment>
                ))}
              </div>
            )}
          </section>

          <section className="panel order-details">
            <div className="order-head">
              <div>
                <strong>Pedido #{pedido.numero}</strong>
                <small>{dataEHora(pedido.criadoEm)}</small>
              </div>
              <span className={`ticket-status ${classeDoStatus(pedido.status)}`}>
                {ROTULO_DO_STATUS[pedido.status]}
              </span>
            </div>
            {pedido.itens.map((item, i) => (
              <div className="order-product" key={`${item.produtoId}-${i}`}>
                <img src={IMAGEM_PADRAO} alt="" />
                <div>
                  <strong>
                    {item.quantidade}x {item.nome}
                  </strong>
                  <small>
                    {dinheiro(item.precoUnitario)} cada
                    {item.observacao !== null && item.observacao !== ''
                      ? ` · ${item.observacao}`
                      : ''}
                  </small>
                </div>
                <span>{dinheiro(item.subtotal)}</span>
              </div>
            ))}
            <div className="totals">
              <p>
                <span>Subtotal</span>
                <span>{dinheiro(pedido.subtotal)}</span>
              </p>
              <p>
                <span>{pedido.modalidade === 'ENTREGA' ? 'Taxa de entrega' : 'Retirada'}</span>
                <span>{dinheiro(pedido.taxaDeEntrega)}</span>
              </p>
              <p>
                <span>Pagamento</span>
                <span>
                  {ROTULO_DA_FORMA[pedido.pagamento.forma]}
                  {pedido.pagamento.trocoPara !== null
                    ? ` · troco para ${dinheiro(pedido.pagamento.trocoPara)}`
                    : ''}
                </span>
              </p>
              <h2>
                Total <b>{dinheiro(pedido.total)}</b>
              </h2>
            </div>
          </section>

          {loja !== '' && (
            <section className="panel tracking compact-tracking">
              <div className="section-title">
                <div className="title-icon">
                  <span className="material-symbols-outlined">restaurant_menu</span>
                </div>
                <div>
                  <small>NOVO PEDIDO</small>
                  <h2>Bateu fome de novo?</h2>
                  <p>Volte ao cardápio da loja.</p>
                </div>
              </div>
              <Link className="btn btn-primary" to={`/loja/${loja}`}>
                <span className="material-symbols-outlined">add_shopping_cart</span>Ver cardápio
              </Link>
            </section>
          )}
        </>
      )}
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Seus pedidos, sempre por perto.
      </footer>
    </CascaDaVitrine>
  );
}
