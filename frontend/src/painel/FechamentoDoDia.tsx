import { useMemo, useState } from 'react';
import { emDemonstracao } from '../aki/demo';
import { dinheiro } from '../aki/formatos';
import { acertosDosEntregadores, resumoDoDia } from '../pedido/fechamento';
import { hojeLocal, ROTULO_DA_FORMA } from '../pedido/regras';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';
import { useFonteDePedidos } from './useFonteDePedidos';
import { usePedidosDoDia } from './usePedidosDoDia';

/**
 * O fechamento do dia: o caixa por forma de pagamento e o acerto de cada entregador.
 *
 * **É um relatório, e não um registro.** Ele soma os pedidos do dia como o servidor os
 * devolve; fechar o caixa com data, conferência e assinatura é o marco 6 (settlement). Para
 * 15/11, a loja confere aqui e imprime.
 *
 * O repasse é a taxa de cada entrega feita (decisão de 31/10/2026). O dinheiro que o
 * entregador recebeu na porta é da loja; o acerto é a diferença.
 */
export function FechamentoDoDia() {
  const { estabelecimentoId, loja } = useLojaAtual();
  const fonte = useFonteDePedidos(estabelecimentoId);
  const [dia, definirDia] = useState(hojeLocal());
  const { pedidos, falha, recarregar } = usePedidosDoDia(fonte, dia);
  const lista = useMemo(() => pedidos ?? [], [pedidos]);
  const resumo = useMemo(() => resumoDoDia(lista), [lista]);
  const acertos = useMemo(() => acertosDosEntregadores(lista), [lista]);
  const abertos = lista.filter(
    (um) => !['ENTREGUE', 'RETIRADO', 'CANCELADO'].includes(um.status),
  ).length;
  const semRegistro = lista.filter(
    (um) =>
      (um.status === 'ENTREGUE' || um.status === 'RETIRADO') && um.pagamentoRegistrado === null,
  ).length;
  const totalRepasse = acertos.reduce((soma, um) => soma + um.repasse, 0);

  return (
    <CascaDaLoja
      atual="fechamento"
      titulo="Fechamento do dia"
      subtitulo={`O caixa e o acerto dos entregadores da ${loja?.nome ?? 'loja'}.`}
    >
      <section className="panel prod-panel">
        <div className="table-title">
          <div>
            <h2>Caixa de {dia.split('-').reverse().join('/')}</h2>
            <p className="admin-muted">
              Relatório do dia, somado dos pedidos. Confira e imprima no fim do expediente.
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
            <button type="button" className="btn btn-light" onClick={recarregar}>
              <span className="material-symbols-outlined">refresh</span>Atualizar
            </button>
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
        {abertos > 0 && (
          <p className="prod-nota" role="status">
            Ainda há {abertos} {abertos === 1 ? 'pedido em andamento' : 'pedidos em andamento'}: o
            fechamento só soma o que foi concluído.
          </p>
        )}
        {semRegistro > 0 && (
          <p className="prod-nota" role="status">
            {semRegistro} {semRegistro === 1 ? 'pedido concluído' : 'pedidos concluídos'} sem o
            pagamento registrado — entram pela forma que o cliente declarou.
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
            <strong>{dinheiro(resumo.vendas)}</strong>
            <small>{resumo.concluidos} pedidos concluídos</small>
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
              <strong>{dinheiro(resumo.porForma[forma])}</strong>
              <small>
                {forma === 'DINHEIRO' ? 'No caixa e com os entregadores' : 'Na conta da loja'}
              </small>
            </div>
          ))}
        </section>
        <p className="admin-muted">
          {resumo.cancelados} cancelados · {dinheiro(resumo.taxas)} em taxas de entrega · ticket
          médio {dinheiro(resumo.ticketMedio)}
        </p>
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
      <footer className="footer">
        <b>Aki Delivery</b> © 2026 — Fechamento do dia
      </footer>
    </CascaDaLoja>
  );
}
