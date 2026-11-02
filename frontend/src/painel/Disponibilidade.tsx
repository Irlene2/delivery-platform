import { useState } from 'react';
import { ESTADOS, ROTULO, type EstadoDeDisponibilidade } from '../api/disponibilidade';
import { emDemonstracao } from '../aki/demo';
import { toast } from '../aki/toast';
import { IMAGEM_PADRAO, mensagemDoErro, type ItemDoCatalogo } from './catalogo';
import { CascaDaLoja } from './CascaDaLoja';
import { COR_DA_DISPONIBILIDADE } from './rotulos';
import { useLojaAtual } from './lojaAtual';
import { useCatalogo } from './useCatalogo';

/**
 * Estoque: o que tem e o que acabou hoje, produto a produto, no visual da lista
 * de produtos. O estoque deste sistema é **qualitativo** (premissa P6): ninguém
 * conta unidade — marca "acabando" ou "acabou hoje", e o produto volta sozinho
 * no próximo expediente.
 */
export function Disponibilidade() {
  const { fonte } = useLojaAtual();
  const { leitura, trocar } = useCatalogo(fonte);
  const [salvando, definirSalvando] = useState<string | null>(null);
  const [recusas, definirRecusas] = useState<Record<string, string>>({});
  // Com muitos produtos, achar um pela lista não dá (02/11/2026): busca e filtros.
  const [busca, definirBusca] = useState('');
  const [secao, definirSecao] = useState('');
  const [situacao, definirSituacao] = useState<'' | 'FALTANDO'>('');
  const podeMarcar = fonte.editaTudo || fonte.podeAlterar;

  async function marcar(item: ItemDoCatalogo, estado: EstadoDeDisponibilidade) {
    if (leitura.estado !== 'pronto') return;
    definirSalvando(item.id);
    definirRecusas((atual) => ({ ...atual, [item.id]: '' }));
    try {
      const atualizado = await fonte.marcar(item, estado);
      trocar({
        ...leitura.catalogo,
        itens: leitura.catalogo.itens.map((um) =>
          um.id === item.id ? { ...um, ...atualizado } : um,
        ),
      });
      toast(`${item.nome}: ${ROTULO[atualizado.disponibilidade]}`);
    } catch (erro) {
      definirRecusas((atual) => ({ ...atual, [item.id]: mensagemDoErro(erro) }));
    } finally {
      definirSalvando(null);
    }
  }

  const itens =
    leitura.estado === 'pronto'
      ? [...leitura.catalogo.itens].sort(
          (a, b) => a.tipo.localeCompare(b.tipo) || a.ordem - b.ordem,
        )
      : [];
  const acabando = itens.filter((item) => item.disponibilidade !== 'DISPONIVEL').length;
  const secoes = [...new Set(itens.map((item) => item.tipo))];
  const termo = busca.trim().toLowerCase();
  const filtrados = itens.filter(
    (item) =>
      (termo === '' || `${item.nome} ${item.tipo}`.toLowerCase().includes(termo)) &&
      (secao === '' || item.tipo === secao) &&
      (situacao === '' || item.disponibilidade !== 'DISPONIVEL'),
  );
  const filtrando = termo !== '' || secao !== '' || situacao !== '';

  return (
    <CascaDaLoja
      atual="estoque"
      titulo={podeMarcar ? 'Controle de estoque' : 'Consultar estoque'}
      subtitulo="O que tem e o que acabou hoje."
    >
      {leitura.estado === 'erro' && (
        <div className="prod-aviso" role="status">
          Não deu para carregar os produtos: {leitura.frase}
        </div>
      )}
      {emDemonstracao() && (
        <div className="prod-aviso" role="status">
          Modo demonstração: as marcações ficam só nesta aba.
        </div>
      )}
      <section className="panel prod-panel" id="estoque">
        <div className="table-title">
          <div>
            <h2>Disponibilidade hoje</h2>
            <p className="admin-muted">
              {leitura.estado === 'carregando'
                ? 'Carregando…'
                : `${itens.length} produto(s), ${acabando} acabando ou esgotado(s)` +
                  (filtrando ? ` · mostrando ${filtrados.length}` : '')}
            </p>
          </div>
          <span className="status orange">Volta sozinho no próximo expediente</span>
        </div>
        <div className="menu-toolbar">
          <label className="search">
            <span className="material-symbols-outlined">search</span>
            <input
              type="search"
              placeholder="Buscar produto pelo nome"
              aria-label="Buscar produto pelo nome"
              value={busca}
              onChange={(evento) => definirBusca(evento.target.value)}
            />
          </label>
          <select
            className="select"
            aria-label="Filtrar por categoria"
            value={secao}
            onChange={(evento) => definirSecao(evento.target.value)}
          >
            <option value="">Todas as categorias</option>
            {secoes.map((nome) => (
              <option key={nome} value={nome}>
                {nome}
              </option>
            ))}
          </select>
          <select
            className="select"
            aria-label="Filtrar por situação"
            value={situacao}
            onChange={(evento) => definirSituacao(evento.target.value as '' | 'FALTANDO')}
          >
            <option value="">Todas as situações</option>
            <option value="FALTANDO">Só acabando ou esgotados</option>
          </select>
        </div>
        <div className="table-scroll">
          <table className="orders-table prod-tabela">
            <thead>
              <tr>
                <th>Produto</th>
                <th>Tipo</th>
                <th>Agora</th>
                <th>{podeMarcar ? 'Marcar como' : 'Situação'}</th>
              </tr>
            </thead>
            <tbody>
              {filtrados.length === 0 ? (
                <tr>
                  <td colSpan={4} className="prod-vazio">
                    {leitura.estado === 'carregando'
                      ? 'Carregando os produtos…'
                      : itens.length === 0
                        ? 'Nenhum produto cadastrado ainda.'
                        : 'Nenhum produto com esse filtro.'}
                  </td>
                </tr>
              ) : (
                filtrados.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <div className="prod-nome">
                        <img
                          src={item.imagem || IMAGEM_PADRAO}
                          alt=""
                          loading="lazy"
                          decoding="async"
                        />
                        <div>
                          <strong>{item.nome}</strong>
                          {recusas[item.id] ? <small role="alert">{recusas[item.id]}</small> : null}
                        </div>
                      </div>
                    </td>
                    <td>{item.tipo}</td>
                    <td>
                      <span className={`status ${COR_DA_DISPONIBILIDADE[item.disponibilidade]}`}>
                        {ROTULO[item.disponibilidade]}
                      </span>
                    </td>
                    <td>
                      {item.controle === 'SEM_CONTROLE' ? (
                        <span className="admin-muted">Nunca acaba</span>
                      ) : podeMarcar ? (
                        <select
                          className="select"
                          aria-label={`Disponibilidade de ${item.nome}`}
                          value={item.disponibilidade}
                          disabled={salvando === item.id}
                          onChange={(evento) =>
                            void marcar(item, evento.target.value as EstadoDeDisponibilidade)
                          }
                        >
                          {ESTADOS.map((estado) => (
                            <option key={estado} value={estado}>
                              {ROTULO[estado]}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <span className="admin-muted">Só consulta</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Estoque
      </footer>
    </CascaDaLoja>
  );
}
