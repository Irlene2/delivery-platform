import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router';
import { gravar, ler } from '../aki/guardado';
import { toast } from '../aki/toast';
import { dataEHora } from '../aki/formatos';
import { caminhoDaLoja } from './caminhos';
import { naoVistas, notificacoesDosPedidos } from './notificacoes';
import { useFonteDePedidos } from './useFonteDePedidos';
import { usePedidosDoDia } from './usePedidosDoDia';

/**
 * O sino do topo (02/11/2026): os pedidos novos e as mudanças que não foram feitas no painel,
 * com o contador do que ainda não foi visto. Relê os pedidos do dia com o mesmo intervalo do
 * dashboard; abrir a lista marca tudo como visto.
 *
 * Na primeira leitura da aba, o que já existe entra como visto: o sino avisa do que acontece
 * daqui em diante, e não do dia inteiro de uma vez. A fila de Pedidos mostra o resto.
 */
export function SinoDeNotificacoes({ estabelecimentoId }: { estabelecimentoId: string }) {
  const fonte = useFonteDePedidos(estabelecimentoId);
  const { pedidos } = usePedidosDoDia(fonte);
  const chave = `aki.notificacoes.${estabelecimentoId}`;
  const [vistas, definirVistas] = useState<Set<string> | null>(() => {
    const guardadas = ler<string[] | null>(chave, null);
    return guardadas === null ? null : new Set(guardadas);
  });
  const [aberto, definirAberto] = useState(false);
  const caixa = useRef<HTMLDivElement>(null);

  const avisos = useMemo(() => notificacoesDosPedidos(pedidos ?? []), [pedidos]);
  const novas = useMemo(
    () => (vistas === null ? [] : naoVistas(avisos, vistas)),
    [avisos, vistas],
  );

  // A primeira leitura vira a linha de base; as seguintes avisam com um toast.
  const anteriores = useRef<number | null>(null);
  useEffect(() => {
    if (pedidos === null) return;
    if (vistas === null) {
      const base = new Set(avisos.map((aviso) => aviso.chave));
      gravar(chave, [...base]);
      definirVistas(base);
      return;
    }
    if (anteriores.current !== null && novas.length > anteriores.current) {
      const ultima = novas[0];
      if (ultima !== undefined) toast(ultima.titulo);
    }
    anteriores.current = novas.length;
  }, [pedidos, avisos, vistas, novas, chave]);

  // Fecha ao clicar fora.
  useEffect(() => {
    if (!aberto) return;
    const fora = (evento: MouseEvent) => {
      if (caixa.current !== null && !caixa.current.contains(evento.target as Node)) {
        definirAberto(false);
      }
    };
    document.addEventListener('mousedown', fora);
    return () => document.removeEventListener('mousedown', fora);
  }, [aberto]);

  function abrir() {
    const abrindo = !aberto;
    definirAberto(abrindo);
    if (abrindo && novas.length > 0) {
      const todas = new Set([...(vistas ?? []), ...avisos.map((aviso) => aviso.chave)]);
      gravar(chave, [...todas]);
      definirVistas(todas);
      anteriores.current = 0;
    }
  }

  const naoLidas = new Set(novas.map((aviso) => aviso.chave));
  const pedidosDaLoja = `${caminhoDaLoja(estabelecimentoId)}/pedidos`;

  return (
    <div className="notificacoes" ref={caixa}>
      <button
        className="icon-btn notification"
        type="button"
        aria-label={
          novas.length > 0 ? `Notificações: ${novas.length} novas` : 'Notificações, nenhuma nova'
        }
        aria-expanded={aberto}
        onClick={abrir}
      >
        <span className="material-symbols-outlined">notifications</span>
        {novas.length > 0 && <b>{novas.length > 9 ? '9+' : novas.length}</b>}
      </button>
      {aberto && (
        <div className="notificacoes-lista" role="dialog" aria-label="Notificações">
          <div className="notificacoes-topo">
            <strong>Notificações</strong>
            <Link to={pedidosDaLoja} onClick={() => definirAberto(false)}>
              Ver pedidos
            </Link>
          </div>
          {avisos.length === 0 ? (
            <p className="notificacoes-vazio">Nenhum pedido hoje ainda.</p>
          ) : (
            <ul>
              {avisos.slice(0, 20).map((aviso) => (
                <li key={aviso.chave} className={naoLidas.has(aviso.chave) ? 'nova' : undefined}>
                  <Link to={pedidosDaLoja} onClick={() => definirAberto(false)}>
                    <span className="material-symbols-outlined">{aviso.icone}</span>
                    <span>
                      <b>{aviso.titulo}</b>
                      <small>
                        {aviso.detalhe} · {dataEHora(aviso.quando)}
                      </small>
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
