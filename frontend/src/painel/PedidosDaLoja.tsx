import { useCallback, useEffect, useRef, useState } from 'react';
import type {
  EntregadorDaEquipe,
  FormaDePagamento,
  PedidoDaLoja,
  StatusDoPedido,
} from '../api/pedidos';
import { emDemonstracao } from '../aki/demo';
import { dataEHora, dinheiro } from '../aki/formatos';
import { toast } from '../aki/toast';
import { lerPreco, mensagemDoErro } from './catalogo';
import type { FonteDePedidos } from '../pedido/fontes';
import {
  aceitaPagamento,
  cancelavel,
  classeDoStatus,
  encerrado,
  hojeLocal,
  linkDoWhatsApp,
  mensagemDoWhatsApp,
  proximoPasso,
  ROTULO_DA_FORMA,
  ROTULO_DA_MODALIDADE,
  ROTULO_DO_STATUS,
} from '../pedido/regras';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';
import { linkDoAcompanhamento, useFonteDePedidos } from './useFonteDePedidos';

/**
 * Os pedidos do dia (ADR-077): a fila que a loja opera — confirmar, preparar, mandar sair ou
 * avisar que está pronto, registrar o pagamento e concluir. E o atalho do WhatsApp com a
 * mensagem do status e o link do acompanhamento (`wa.me`, sem integração).
 *
 * Relê a cada {@link INTERVALO} ms. Pedido novo que aparece entre uma leitura e outra ganha
 * um aviso no canto da tela.
 */
const INTERVALO = 20_000;

const FILTROS: (StatusDoPedido | '')[] = [
  '',
  'RECEBIDO',
  'CONFIRMADO',
  'EM_PREPARO',
  'PRONTO',
  'SAIU_PARA_ENTREGA',
  'AGUARDANDO_CLIENTE',
  'ENTREGUE',
  'RETIRADO',
  'CANCELADO',
];

export function PedidosDaLoja() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const fonte = useFonteDePedidos(estabelecimentoId);
  const demo = emDemonstracao();
  const [status, definirStatus] = useState<StatusDoPedido | ''>('');
  const [dia, definirDia] = useState(hojeLocal());
  const [pedidos, definirPedidos] = useState<PedidoDaLoja[] | null>(null);
  const [falha, definirFalha] = useState<string | null>(null);
  const conhecidos = useRef<Set<string> | null>(null);
  const [equipe, definirEquipe] = useState<EntregadorDaEquipe[]>([]);

  useEffect(() => {
    let atual = true;
    fonte
      .entregadores()
      .then((lidos) => {
        if (atual) definirEquipe(lidos);
      })
      .catch(() => undefined);
    return () => {
      atual = false;
    };
  }, [fonte]);

  const ler = useCallback(() => {
    fonte
      .listar({ ...(status === '' ? {} : { status }), ...(demo ? {} : { dia }) })
      .then((lidos) => {
        const antes = conhecidos.current;
        if (antes !== null) {
          const novos = lidos.filter((um) => um.status === 'RECEBIDO' && !antes.has(um.id));
          if (novos.length > 0) toast(`Novo pedido #${novos[0]?.numero ?? ''} chegou`);
        }
        conhecidos.current = new Set(lidos.map((um) => um.id));
        definirPedidos(lidos);
        definirFalha(null);
      })
      .catch((erro: unknown) => definirFalha(mensagemDoErro(erro)));
  }, [fonte, status, dia, demo]);

  useEffect(() => {
    ler();
    const relogio = window.setInterval(ler, INTERVALO);
    return () => window.clearInterval(relogio);
  }, [ler]);

  function trocar(atualizado: PedidoDaLoja) {
    definirPedidos(
      (lista) => lista?.map((um) => (um.id === atualizado.id ? atualizado : um)) ?? lista,
    );
  }

  const lista = pedidos ?? [];
  const contar = (...quais: StatusDoPedido[]) =>
    lista.filter((um) => quais.includes(um.status)).length;

  return (
    <CascaDaLoja
      atual="pedidos"
      titulo="Pedidos"
      subtitulo="Os pedidos que chegaram pelo link do cardápio."
    >
      <section className="stats">
        <div className="stat">
          <div className="stat-top">
            <span>Novos</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">notifications_active</span>
            </div>
          </div>
          <strong>{contar('RECEBIDO')}</strong>
          <small>Esperando confirmação</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Na cozinha</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">restaurant</span>
            </div>
          </div>
          <strong>{contar('CONFIRMADO', 'EM_PREPARO', 'PRONTO')}</strong>
          <small>Confirmados, em preparo e prontos</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Saindo</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">two_wheeler</span>
            </div>
          </div>
          <strong>{contar('SAIU_PARA_ENTREGA', 'AGUARDANDO_CLIENTE')}</strong>
          <small>Em entrega ou no balcão</small>
        </div>
        <div className="stat">
          <div className="stat-top">
            <span>Concluídos</span>
            <div className="stat-icon">
              <span className="material-symbols-outlined">task_alt</span>
            </div>
          </div>
          <strong>{contar('ENTREGUE', 'RETIRADO')}</strong>
          <small>
            {dinheiro(
              lista
                .filter((um) => um.status === 'ENTREGUE' || um.status === 'RETIRADO')
                .reduce((soma, um) => soma + um.total, 0),
            )}{' '}
            recebidos
          </small>
        </div>
      </section>

      <section className="panel orders-history" id="pedidos">
        <div className="table-title">
          <div>
            <h2>Fila de pedidos</h2>
            <p className="admin-muted">
              {demo
                ? 'Demonstração: os pedidos feitos em /loja/demo nesta aba.'
                : 'Atualiza sozinha a cada 20 segundos.'}
            </p>
          </div>
          <button type="button" className="btn btn-light" onClick={ler}>
            <span className="material-symbols-outlined">refresh</span>Atualizar
          </button>
        </div>
        <div className="menu-toolbar">
          <select
            className="select"
            aria-label="Filtrar por status"
            value={status}
            onChange={(evento) => definirStatus(evento.target.value as StatusDoPedido | '')}
          >
            {FILTROS.map((um) => (
              <option key={um} value={um}>
                {um === '' ? 'Todos os status' : ROTULO_DO_STATUS[um]}
              </option>
            ))}
          </select>
          {!demo && (
            <input
              className="select"
              type="date"
              aria-label="Dia"
              value={dia}
              onChange={(evento) => definirDia(evento.target.value || hojeLocal())}
            />
          )}
        </div>

        {falha !== null && (
          <p className="prod-aviso" role="alert">
            {falha}
          </p>
        )}
        {pedidos === null ? (
          falha === null && (
            <div className="ticket-empty">
              <strong>Carregando os pedidos…</strong>
            </div>
          )
        ) : lista.length === 0 ? (
          <div className="ticket-empty">
            <span className="material-symbols-outlined">receipt_long</span>
            <strong>Nenhum pedido {status === '' ? 'neste dia' : 'com este status'}</strong>
            <p>Mande o link do cardápio para os clientes — ele está em Configurações.</p>
          </div>
        ) : (
          <div className="orders-list">
            {lista.map((pedido) => (
              <CartaoDoPedido
                key={pedido.id}
                pedido={pedido}
                fonte={fonte}
                equipe={equipe}
                nomeDaLoja={loja?.nome ?? 'loja'}
                aoMudar={trocar}
              />
            ))}
          </div>
        )}
      </section>
    </CascaDaLoja>
  );
}

const AVULSO = '__avulso__';

function CartaoDoPedido({
  pedido,
  fonte,
  equipe,
  nomeDaLoja,
  aoMudar,
}: {
  pedido: PedidoDaLoja;
  fonte: FonteDePedidos;
  equipe: readonly EntregadorDaEquipe[];
  nomeDaLoja: string;
  aoMudar: (pedido: PedidoDaLoja) => void;
}) {
  const [ocupado, definirOcupado] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);
  // Da equipe pelo usuarioId; "avulso" abre o campo do nome.
  const [escolhido, definirEscolhido] = useState(
    pedido.entregadorId ?? (pedido.entregador !== null || equipe.length === 0 ? AVULSO : ''),
  );
  const [entregador, definirEntregador] = useState(
    pedido.entregadorId === null ? (pedido.entregador ?? '') : '',
  );
  const [cancelando, definirCancelando] = useState(false);
  const [motivo, definirMotivo] = useState('');
  const [forma, definirForma] = useState<FormaDePagamento>(pedido.pagamentoDeclarado.forma);
  const [recebido, definirRecebido] = useState(
    pedido.pagamentoDeclarado.trocoPara === null ? '' : String(pedido.pagamentoDeclarado.trocoPara),
  );
  const [pixConferido, definirPixConferido] = useState(false);

  const passo = proximoPasso(pedido);
  const entrega = pedido.modalidade === 'ENTREGA';
  const precisaEntregador =
    entrega && !encerrado(pedido.status) && pedido.status !== 'SAIU_PARA_ENTREGA';
  const precisaPagamento = aceitaPagamento(pedido.status);

  async function fazer(ato: () => Promise<PedidoDaLoja>, aviso: string) {
    definirOcupado(true);
    definirRecusa(null);
    try {
      const atualizado = await ato();
      aoMudar(atualizado);
      toast(aviso);
      return true;
    } catch (erro) {
      definirRecusa(mensagemDoErro(erro));
      return false;
    } finally {
      definirOcupado(false);
    }
  }

  function registrar() {
    let valorRecebido: number | undefined;
    if (forma === 'DINHEIRO') {
      const lido = lerPreco(recebido);
      if (lido === null) {
        definirRecusa('Informe quanto foi recebido em dinheiro, como 110,00.');
        return;
      }
      valorRecebido = lido;
    }
    void fazer(
      () =>
        fonte.pagamento(pedido.id, {
          formaEfetiva: forma,
          ...(valorRecebido === undefined ? {} : { valorRecebido }),
          ...(forma === 'PIX' ? { pixConferido } : {}),
        }),
      `Pagamento do pedido #${pedido.numero} registrado`,
    );
  }

  const whatsapp = linkDoWhatsApp(
    pedido.cliente.telefone,
    mensagemDoWhatsApp(pedido, nomeDaLoja, linkDoAcompanhamento(pedido.codigoDeAcompanhamento)),
  );

  return (
    <article className="order-history-card" data-pedido={pedido.numero}>
      <div className="order-history-main">
        <div className="order-number">
          <span className="material-symbols-outlined">
            {entrega ? 'two_wheeler' : 'storefront'}
          </span>
          <div>
            <strong>
              #{pedido.numero} · {pedido.cliente.nome}
            </strong>
            <small>
              {dataEHora(pedido.criadoEm)} · {ROTULO_DA_MODALIDADE[pedido.modalidade]} ·{' '}
              {pedido.cliente.telefone}
            </small>
          </div>
        </div>
        <span className={`ticket-status ${classeDoStatus(pedido.status)}`}>
          {ROTULO_DO_STATUS[pedido.status]}
        </span>
      </div>

      <div className="order-history-items">
        {pedido.itens.map((item, i) => (
          <span key={`${item.produtoId}-${i}`}>
            {item.quantidade}x {item.nome}
            {item.observacao !== null && item.observacao !== '' ? ` (${item.observacao})` : ''}
          </span>
        ))}
      </div>

      {pedido.endereco !== null && (
        <p className="pedido-linha">
          <span className="material-symbols-outlined">location_on</span>
          {pedido.endereco.logradouro}, {pedido.endereco.bairro}
          {pedido.endereco.complemento !== null ? ` — ${pedido.endereco.complemento}` : ''}
        </p>
      )}
      <p className="pedido-linha">
        <span className="material-symbols-outlined">payments</span>
        Paga com {ROTULO_DA_FORMA[pedido.pagamentoDeclarado.forma]}
        {pedido.pagamentoDeclarado.trocoPara !== null
          ? ` · troco para ${dinheiro(pedido.pagamentoDeclarado.trocoPara)}`
          : ''}
        {pedido.pagamentoRegistrado !== null && (
          <b className="pedido-pago">
            {' '}
            · recebido em {ROTULO_DA_FORMA[pedido.pagamentoRegistrado.formaEfetiva]}
            {pedido.pagamentoRegistrado.trocoDevolvido !== null &&
            pedido.pagamentoRegistrado.trocoDevolvido > 0
              ? `, troco de ${dinheiro(pedido.pagamentoRegistrado.trocoDevolvido)}`
              : ''}
          </b>
        )}
      </p>
      {pedido.entregador !== null && (
        <p className="pedido-linha">
          <span className="material-symbols-outlined">sports_motorsports</span>
          Entregador: {pedido.entregador}
        </p>
      )}
      {pedido.motivoDoCancelamento !== null && (
        <p className="pedido-linha">
          <span className="material-symbols-outlined">cancel</span>
          Cancelado: {pedido.motivoDoCancelamento}
        </p>
      )}

      {!encerrado(pedido.status) && (
        <div className="pedido-acoes">
          {precisaEntregador && (
            <div className="pedido-campo">
              <select
                className="select"
                aria-label={`Entregador do pedido ${pedido.numero}`}
                value={escolhido}
                onChange={(evento) => definirEscolhido(evento.target.value)}
              >
                <option value="">Escolha o entregador</option>
                {equipe.map((um) => (
                  <option key={um.usuarioId} value={um.usuarioId}>
                    {um.nome}
                  </option>
                ))}
                <option value={AVULSO}>Outro, sem conta (digitar o nome)</option>
              </select>
              {escolhido === AVULSO && (
                <input
                  className="select"
                  aria-label={`Nome do entregador do pedido ${pedido.numero}`}
                  placeholder="Nome do entregador"
                  maxLength={80}
                  value={entregador}
                  onChange={(evento) => definirEntregador(evento.target.value)}
                />
              )}
              <button
                type="button"
                className="btn btn-light"
                disabled={
                  ocupado ||
                  escolhido === '' ||
                  (escolhido === AVULSO && entregador.trim() === '') ||
                  (escolhido !== AVULSO && escolhido === pedido.entregadorId)
                }
                onClick={() =>
                  void fazer(
                    () =>
                      fonte.entregador(
                        pedido.id,
                        escolhido === AVULSO
                          ? { nome: entregador.trim() }
                          : { usuarioId: escolhido },
                      ),
                    `Entregador do pedido #${pedido.numero} definido`,
                  )
                }
              >
                <span className="material-symbols-outlined">sports_motorsports</span>Salvar
                entregador
              </button>
            </div>
          )}

          {precisaPagamento && (
            <div className="pedido-campo">
              <select
                className="select"
                aria-label={`Como o pedido ${pedido.numero} foi pago`}
                value={forma}
                onChange={(evento) => definirForma(evento.target.value as FormaDePagamento)}
              >
                {(['PIX', 'CARTAO', 'DINHEIRO'] as const).map((uma) => (
                  <option key={uma} value={uma}>
                    Recebido em {ROTULO_DA_FORMA[uma]}
                  </option>
                ))}
              </select>
              {forma === 'DINHEIRO' && (
                <input
                  className="select"
                  inputMode="decimal"
                  aria-label="Valor recebido"
                  placeholder={`Recebido (total ${dinheiro(pedido.total)})`}
                  value={recebido}
                  onChange={(evento) => definirRecebido(evento.target.value)}
                />
              )}
              {forma === 'PIX' && (
                <label className="pedido-confere">
                  <input
                    type="checkbox"
                    checked={pixConferido}
                    onChange={(evento) => definirPixConferido(evento.target.checked)}
                  />
                  Conferi o Pix no extrato
                </label>
              )}
              <button
                type="button"
                className="btn btn-light"
                disabled={ocupado}
                onClick={registrar}
              >
                <span className="material-symbols-outlined">price_check</span>
                {pedido.pagamentoRegistrado === null ? 'Registrar pagamento' : 'Corrigir pagamento'}
              </button>
            </div>
          )}

          <div className="pedido-campo">
            {passo !== null && (
              <button
                type="button"
                className="btn btn-primary"
                disabled={ocupado}
                onClick={() =>
                  void fazer(
                    () => fonte.avancar(pedido.id, passo.status),
                    `Pedido #${pedido.numero}: ${ROTULO_DO_STATUS[passo.status]}`,
                  )
                }
              >
                <span className="material-symbols-outlined">{passo.icone}</span>
                {passo.rotulo}
              </button>
            )}
            <a className="btn btn-light" href={whatsapp} target="_blank" rel="noreferrer">
              <span className="material-symbols-outlined">chat</span>Avisar no WhatsApp
            </a>
            {cancelavel(pedido.status) && !cancelando && (
              <button
                type="button"
                className="btn btn-danger-soft"
                disabled={ocupado}
                onClick={() => definirCancelando(true)}
              >
                <span className="material-symbols-outlined">cancel</span>Cancelar
              </button>
            )}
          </div>

          {cancelando && (
            <div className="prod-confirmar" role="alertdialog" aria-label="Cancelar o pedido">
              <input
                className="select"
                aria-label="Motivo do cancelamento"
                placeholder="Motivo (o cliente verá no WhatsApp)"
                maxLength={200}
                value={motivo}
                onChange={(evento) => definirMotivo(evento.target.value)}
              />
              <button
                type="button"
                className="btn btn-danger-soft"
                disabled={ocupado}
                onClick={() =>
                  void fazer(
                    () => fonte.avancar(pedido.id, 'CANCELADO', motivo),
                    `Pedido #${pedido.numero} cancelado`,
                  ).then((deuCerto) => {
                    if (deuCerto) definirCancelando(false);
                  })
                }
              >
                Confirmar cancelamento
              </button>
              <button
                type="button"
                className="btn btn-light"
                onClick={() => definirCancelando(false)}
              >
                Voltar
              </button>
            </div>
          )}
        </div>
      )}

      {recusa !== null && (
        <p className="prod-msg erro" role="alert">
          {recusa}
        </p>
      )}

      <div className="order-history-bottom">
        <strong>{dinheiro(pedido.total)}</strong>
        {encerrado(pedido.status) && (
          <a className="ticket-btn" href={whatsapp} target="_blank" rel="noreferrer">
            <span className="material-symbols-outlined">chat</span> WhatsApp
          </a>
        )}
      </div>
    </article>
  );
}
