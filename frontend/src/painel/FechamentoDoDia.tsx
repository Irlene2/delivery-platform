import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react';
import { emDemonstracao } from '../aki/demo';
import { dinheiro } from '../aki/formatos';
import { toast } from '../aki/toast';
import type { FechamentoGravado } from '../api/pedidos';
import { fechamentoEmCsv, periodoEmCsv, somaDoPeriodo } from '../pedido/caixa';
import { acertosDosEntregadores, resumoDoDia } from '../pedido/fechamento';
import { hojeLocal, ROTULO_DA_FORMA } from '../pedido/regras';
import { lerPreco, mensagemDoErro } from './catalogo';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';
import { useFonteDePedidos, useFonteDoCaixa } from './useFonteDePedidos';
import { usePedidosDoDia } from './usePedidosDoDia';

/** Uma linha do acerto, venha ela da soma ao vivo ou do fechamento gravado. */
interface LinhaDoAcerto {
  readonly chave: string;
  readonly nome: string;
  readonly comConta: boolean;
  readonly entregas: number;
  readonly emRota: number;
  readonly repasse: number;
  readonly dinheiroRecebido: number;
  readonly cartaoRecebido: number;
  readonly acerto: number;
}

const dataBr = (dia: string) => dia.split('-').reverse().join('/');

function baixarCsv(fechamento: FechamentoGravado) {
  baixar(fechamentoEmCsv(fechamento), `fechamento-${fechamento.dia}.csv`);
}

function baixar(conteudo: string, nome: string) {
  // O BOM faz a planilha abrir os acentos certos.
  const arquivo = new Blob(['﻿', conteudo], {
    type: 'text/csv;charset=utf-8',
  });
  const url = URL.createObjectURL(arquivo);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  link.click();
  // Revogar na hora pode cortar o download em alguns navegadores.
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

/**
 * O fechamento do dia: o caixa por forma de pagamento e o acerto de cada entregador.
 *
 * **Antes de fechar, é a soma ao vivo dos pedidos; depois, é o que o servidor gravou** (ADR-077,
 * emenda de 01/11/2026). Fechar exige que nenhum pedido do dia esteja em andamento; o servidor
 * apura os totais a partir dos pedidos — a tela só manda o dia e, se quiser, o dinheiro contado
 * no caixa. Fechado, o dia não muda mais, e o histórico guarda cada fechamento.
 *
 * O repasse é a taxa de cada entrega feita (decisão de 31/10/2026). O dinheiro que o
 * entregador recebeu na porta é da loja; o acerto é a diferença.
 */
export function FechamentoDoDia() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const fonte = useFonteDePedidos(estabelecimentoId);
  const caixa = useFonteDoCaixa(estabelecimentoId);
  const podeFechar = emDemonstracao() || (loja?.permissoes.includes('GERENCIAR_JORNADA') ?? false);
  const podeVerHistorico = emDemonstracao() || (loja?.permissoes.includes('VER_VENDAS') ?? false);

  const [dia, definirDia] = useState(hojeLocal());
  const { pedidos, falha, recarregar } = usePedidosDoDia(fonte, dia);
  const lista = useMemo(() => pedidos ?? [], [pedidos]);
  const resumo = useMemo(() => resumoDoDia(lista), [lista]);
  const acertosAoVivo = useMemo(() => acertosDosEntregadores(lista), [lista]);

  const [gravado, definirGravado] = useState<FechamentoGravado | null>(null);
  const [historico, definirHistorico] = useState<FechamentoGravado[]>([]);
  // As vendas de um período (02/11/2026): do primeiro dia do mês até hoje, para começar.
  const [de, definirDe] = useState(() => `${hojeLocal().slice(0, 8)}01`);
  const [ate, definirAte] = useState(hojeLocal());
  const [recusaDoPeriodo, definirRecusaDoPeriodo] = useState<string | null>(null);
  const soma = useMemo(() => somaDoPeriodo(historico), [historico]);
  const [contado, definirContado] = useState('');
  const [observacao, definirObservacao] = useState('');
  const [fechando, definirFechando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);

  const carregarCaixa = useCallback(() => {
    let atual = true;
    if (podeVerHistorico) {
      caixa
        .doDia(dia)
        .then((lido) => {
          if (atual) definirGravado(lido);
        })
        .catch(() => {
          if (atual) definirGravado(null);
        });
      caixa
        .periodo(de, ate)
        .then((lidos) => {
          if (atual) {
            definirHistorico(lidos);
            definirRecusaDoPeriodo(null);
          }
        })
        .catch((erro: unknown) => {
          if (atual) definirRecusaDoPeriodo(mensagemDoErro(erro));
        });
    }
    return () => {
      atual = false;
    };
  }, [caixa, dia, de, ate, podeVerHistorico]);

  useEffect(() => carregarCaixa(), [carregarCaixa]);

  const abertos = lista.filter(
    (um) => !['ENTREGUE', 'RETIRADO', 'CANCELADO'].includes(um.status),
  ).length;

  // Fechado, vale o que foi gravado; aberto, a soma ao vivo.
  const quadro =
    gravado === null
      ? {
          vendas: resumo.vendas,
          concluidos: resumo.concluidos,
          cancelados: resumo.cancelados,
          taxas: resumo.taxas,
          ticketMedio: resumo.ticketMedio,
          porForma: resumo.porForma,
        }
      : {
          vendas: gravado.vendas,
          concluidos: gravado.concluidos,
          cancelados: gravado.cancelados,
          taxas: gravado.taxas,
          ticketMedio:
            gravado.concluidos === 0
              ? 0
              : Math.round((gravado.vendas / gravado.concluidos) * 100) / 100,
          porForma: { PIX: gravado.pix, CARTAO: gravado.cartao, DINHEIRO: gravado.dinheiro },
        };
  const acertos: LinhaDoAcerto[] =
    gravado === null
      ? acertosAoVivo
      : gravado.acertos.map((um, i) => ({
          chave: um.entregadorId ?? `avulso-${i}`,
          nome: um.nome,
          comConta: um.entregadorId !== null,
          entregas: um.entregas,
          emRota: 0,
          repasse: um.repasse,
          dinheiroRecebido: um.dinheiro,
          cartaoRecebido: um.cartao,
          acerto: um.acerto,
        }));
  const totalRepasse =
    gravado === null ? acertos.reduce((soma, um) => soma + um.repasse, 0) : gravado.repasseTotal;

  async function fechar(evento: FormEvent) {
    evento.preventDefault();
    definirRecusa(null);
    const valor = contado.trim() === '' ? null : lerPreco(contado);
    if (contado.trim() !== '' && valor === null) {
      definirRecusa('O dinheiro contado precisa ser um valor, como 320,00.');
      return;
    }
    definirFechando(true);
    try {
      const fechado = await caixa.fechar({
        dia,
        ...(valor === null ? {} : { dinheiroContado: valor }),
        ...(observacao.trim() === '' ? {} : { observacao: observacao.trim() }),
      });
      definirGravado(fechado);
      definirHistorico((antes) => [fechado, ...antes.filter((um) => um.dia !== fechado.dia)]);
      toast(`Caixa de ${dataBr(fechado.dia)} fechado`);
    } catch (erro) {
      definirRecusa(mensagemDoErro(erro));
    } finally {
      definirFechando(false);
    }
  }

  return (
    <CascaDaLoja
      atual="fechamento"
      titulo="Fechamento do dia"
      subtitulo={`O caixa e o acerto dos entregadores da ${loja?.nome ?? 'loja'}.`}
    >
      <section className="panel prod-panel">
        <div className="table-title">
          <div>
            <h2>Caixa de {dataBr(dia)}</h2>
            <p className="admin-muted">
              {gravado === null
                ? 'Soma ao vivo dos pedidos do dia. Feche o caixa no fim do expediente.'
                : `Fechado em ${new Date(gravado.fechadoEm).toLocaleString('pt-BR')}. Estes são os números gravados.`}
            </p>
          </div>
          <div className="pedido-campo">
            {!emDemonstracao() && (
              <input
                className="select"
                type="date"
                aria-label="Dia"
                value={dia}
                onChange={(evento) => definirDia(evento.target.value || hojeLocal())}
              />
            )}
            <button
              type="button"
              className="btn btn-light"
              onClick={() => {
                recarregar();
                carregarCaixa();
              }}
            >
              <span className="material-symbols-outlined">refresh</span>Atualizar
            </button>
            {gravado !== null && (
              <button type="button" className="btn btn-light" onClick={() => baixarCsv(gravado)}>
                <span className="material-symbols-outlined">download</span>Exportar CSV
              </button>
            )}
            <button type="button" className="btn btn-primary" onClick={() => window.print()}>
              <span className="material-symbols-outlined">print</span>Imprimir
            </button>
          </div>
        </div>
        {falha !== null && (
          <p className="prod-aviso" role="alert">
            {falha}
          </p>
        )}
        {gravado !== null && (
          <p className="prod-nota" role="status">
            <b>Caixa fechado.</b> Os pedidos deste dia não mudam mais.
            {gravado.diferencaDeCaixa !== null &&
              ` Conferência: contado ${dinheiro(gravado.dinheiroContado ?? 0)}, ${
                gravado.diferencaDeCaixa === 0
                  ? 'sem diferença'
                  : gravado.diferencaDeCaixa > 0
                    ? `sobra de ${dinheiro(gravado.diferencaDeCaixa)}`
                    : `falta de ${dinheiro(-gravado.diferencaDeCaixa)}`
              }.`}
            {gravado.observacao !== null && ` Observação: ${gravado.observacao}`}
          </p>
        )}
        {gravado === null && abertos > 0 && (
          <p className="prod-nota" role="status">
            Ainda há {abertos} {abertos === 1 ? 'pedido em andamento' : 'pedidos em andamento'}:
            conclua ou cancele antes de fechar o caixa.
          </p>
        )}

        <section className="stats">
          <div className="stat">
            <div className="stat-top">
              <span>Vendas</span>
              <div className="stat-icon">
                <span className="material-symbols-outlined">payments</span>
              </div>
            </div>
            <strong>{dinheiro(quadro.vendas)}</strong>
            <small>{quadro.concluidos} pedidos concluídos</small>
          </div>
          {(['PIX', 'CARTAO', 'DINHEIRO'] as const).map((forma) => (
            <div className="stat" key={forma}>
              <div className="stat-top">
                <span>{ROTULO_DA_FORMA[forma]}</span>
                <div className="stat-icon">
                  <span className="material-symbols-outlined">
                    {forma === 'PIX'
                      ? 'qr_code_2'
                      : forma === 'CARTAO'
                        ? 'credit_card'
                        : 'payments'}
                  </span>
                </div>
              </div>
              <strong>{dinheiro(quadro.porForma[forma])}</strong>
              <small>
                {forma === 'DINHEIRO' ? 'No caixa e com os entregadores' : 'Na conta da loja'}
              </small>
            </div>
          ))}
        </section>
        <p className="admin-muted">
          {quadro.cancelados} cancelados · {dinheiro(quadro.taxas)} em taxas de entrega · ticket
          médio {dinheiro(quadro.ticketMedio)}
        </p>

        {gravado === null && podeFechar && (
          <form className="prod-form" noValidate onSubmit={(evento) => void fechar(evento)}>
            <fieldset className="prod-grupo" disabled={fechando}>
              <legend>Fechar o caixa</legend>
              <div className="prod-linha">
                <label className="prod-campo">
                  Dinheiro contado no caixa (opcional)
                  <input
                    inputMode="decimal"
                    placeholder="Ex.: 320,00"
                    value={contado}
                    onChange={(evento) => definirContado(evento.target.value)}
                  />
                </label>
                <label className="prod-campo">
                  Observação (opcional)
                  <input
                    maxLength={500}
                    value={observacao}
                    onChange={(evento) => definirObservacao(evento.target.value)}
                  />
                </label>
              </div>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={abertos > 0 || fechando}
                aria-busy={fechando}
              >
                <span className="material-symbols-outlined">lock</span>
                {fechando ? 'Fechando…' : `Fechar o caixa de ${dataBr(dia)}`}
              </button>
              <small className="prod-dica">
                Depois de fechado, os pedidos deste dia não mudam mais e a loja não recebe pedido
                novo neste dia.
              </small>
            </fieldset>
            {recusa !== null && (
              <p className="prod-msg erro" role="alert">
                {recusa}
              </p>
            )}
          </form>
        )}
      </section>

      <section className="panel table-panel" id="entregadores">
        <div className="table-title">
          <div>
            <h2>Acerto dos entregadores</h2>
            <p className="admin-muted">
              Repasse = a taxa de cada entrega feita. O dinheiro recebido na porta é da loja.
            </p>
          </div>
          <span className="status orange">Repasse total {dinheiro(totalRepasse)}</span>
        </div>
        <div className="table-scroll">
          <table className="orders-table">
            <thead>
              <tr>
                <th>Entregador</th>
                <th>Entregas</th>
                <th>Repasse</th>
                <th>Dinheiro recebido</th>
                <th>Cartão recebido</th>
                <th>Acerto</th>
              </tr>
            </thead>
            <tbody>
              {acertos.length === 0 ? (
                <tr>
                  <td colSpan={6}>Nenhuma entrega com entregador neste dia.</td>
                </tr>
              ) : (
                acertos.map((um) => (
                  <tr key={um.chave}>
                    <td>
                      <strong>{um.nome}</strong>
                      <br />
                      <small>{um.comConta ? 'Da equipe' : 'Avulso'}</small>
                      {um.emRota > 0 && (
                        <>
                          {' '}
                          <span className="status blue">{um.emRota} em rota</span>
                        </>
                      )}
                    </td>
                    <td>{um.entregas}</td>
                    <td>{dinheiro(um.repasse)}</td>
                    <td>{dinheiro(um.dinheiroRecebido)}</td>
                    <td>{dinheiro(um.cartaoRecebido)}</td>
                    <td>
                      <strong>
                        {um.acerto > 0
                          ? `Devolve ${dinheiro(um.acerto)} à loja`
                          : um.acerto < 0
                            ? `Recebe ${dinheiro(-um.acerto)} da loja`
                            : 'Zerado'}
                      </strong>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {podeVerHistorico && (
        <section className="panel table-panel" id="historico">
          <div className="table-title">
            <div>
              <h2>Vendas por período</h2>
              <p className="admin-muted">
                Os caixas fechados entre as datas, do mais recente ao mais antigo. Dia que não foi
                fechado não entra na soma.
              </p>
            </div>
          </div>
          <div className="menu-toolbar">
            <label className="pedido-confere">
              De
              <input
                className="select"
                type="date"
                value={de}
                max={ate}
                onChange={(evento) => definirDe(evento.target.value)}
              />
            </label>
            <label className="pedido-confere">
              Até
              <input
                className="select"
                type="date"
                value={ate}
                min={de}
                max={hojeLocal()}
                onChange={(evento) => definirAte(evento.target.value)}
              />
            </label>
            <button
              type="button"
              className="btn btn-light"
              disabled={historico.length === 0}
              onClick={() => baixar(periodoEmCsv(historico), `vendas-${de}-a-${ate}.csv`)}
            >
              <span className="material-symbols-outlined">download</span>CSV do período
            </button>
          </div>
          {recusaDoPeriodo !== null && (
            <p className="prod-msg erro" role="alert">
              {recusaDoPeriodo}
            </p>
          )}
          {historico.length > 0 && (
            <div className="stats" style={{ marginBottom: '15px' }}>
              <div className="panel stat">
                <div className="stat-top">Vendas no período</div>
                <strong>{dinheiro(soma.vendas)}</strong>
                <small>
                  {soma.dias} dia(s) fechado(s) · {soma.concluidos} pedido(s)
                </small>
              </div>
              <div className="panel stat">
                <div className="stat-top">Dinheiro</div>
                <strong>{dinheiro(soma.dinheiro)}</strong>
              </div>
              <div className="panel stat">
                <div className="stat-top">Cartão e Pix</div>
                <strong>{dinheiro(soma.cartao + soma.pix)}</strong>
                <small>
                  Cartão {dinheiro(soma.cartao)} · Pix {dinheiro(soma.pix)}
                </small>
              </div>
              <div className="panel stat">
                <div className="stat-top">Repasse aos entregadores</div>
                <strong>{dinheiro(soma.repasse)}</strong>
              </div>
            </div>
          )}
          <div className="table-scroll">
            <table className="orders-table">
              <thead>
                <tr>
                  <th>Dia</th>
                  <th>Pedidos</th>
                  <th>Vendas</th>
                  <th>Repasse</th>
                  <th>Conferência</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {historico.length === 0 ? (
                  <tr>
                    <td colSpan={6}>Nenhum caixa fechado neste período.</td>
                  </tr>
                ) : (
                  historico.map((um) => (
                    <tr key={um.id}>
                      <td>
                        <strong>{dataBr(um.dia)}</strong>
                      </td>
                      <td>{um.concluidos}</td>
                      <td>{dinheiro(um.vendas)}</td>
                      <td>{dinheiro(um.repasseTotal)}</td>
                      <td>
                        {um.diferencaDeCaixa === null
                          ? 'Sem contagem'
                          : um.diferencaDeCaixa === 0
                            ? 'Sem diferença'
                            : um.diferencaDeCaixa > 0
                              ? `Sobra ${dinheiro(um.diferencaDeCaixa)}`
                              : `Falta ${dinheiro(-um.diferencaDeCaixa)}`}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-light"
                          onClick={() => baixarCsv(um)}
                        >
                          <span className="material-symbols-outlined">download</span>CSV
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}
      <footer className="footer">
        <b>Aki Delivery</b> © 2026 — Fechamento do dia
      </footer>
    </CascaDaLoja>
  );
}
