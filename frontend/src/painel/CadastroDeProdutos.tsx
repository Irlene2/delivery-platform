import { useState } from 'react';
import { Link } from 'react-router';
import { ROTULO, type EstadoDeDisponibilidade } from '../api/disponibilidade';
import { emDemonstracao } from '../aki/demo';
import { dinheiro } from '../aki/formatos';
import { IMAGEM_PADRAO, type Situacao } from './catalogo';
import { CascaDaLoja } from './CascaDaLoja';
import { caminhoDaSecao, caminhoDoNovoProduto } from './caminhos';
import { useLojaAtual } from './lojaAtual';
import { useCatalogo } from './useCatalogo';

/**
 * Cadastro de produtos (`aki_delivery/pages/produtos.html`): a lista da loja,
 * com busca, filtro por tipo e os botões Novo produto e Editar.
 */
export const COR_DA_DISPONIBILIDADE: Record<EstadoDeDisponibilidade, string> = {
  DISPONIVEL: 'green',
  ACABANDO: 'orange',
  ESGOTADO_HOJE: 'vermelho',
  ESGOTADO_INDETERMINADO: 'vermelho',
};
export const ROTULO_DA_SITUACAO: Record<Situacao, [string, string]> = {
  ATIVO: ['Publicado', 'green'],
  RASCUNHO: ['Rascunho', 'cinza'],
  INATIVO: ['Inativo', 'cinza'],
};

export function CadastroDeProdutos() {
  const { estabelecimentoId, fonte } = useLojaAtual();
  const { leitura } = useCatalogo(fonte);
  const [busca, definirBusca] = useState('');
  const [tipo, definirTipo] = useState('');

  const catalogo = leitura.estado === 'pronto' ? leitura.catalogo : { tipos: [], itens: [] };
  const termo = busca.trim().toLowerCase();
  const lista = catalogo.itens
    .filter((item) => tipo === '' || item.tipoId === tipo)
    .filter(
      (item) =>
        termo === '' ||
        `${item.nome} ${item.ingredientes.join(' ')} ${item.tipo}`.toLowerCase().includes(termo),
    )
    .sort((a, b) => a.tipo.localeCompare(b.tipo) || a.ordem - b.ordem);
  const publicados = catalogo.itens.filter((item) => item.situacao === 'ATIVO').length;
  const rotuloDoBotao = fonte.editaTudo || fonte.podeAlterar ? 'Editar' : 'Ver';
  const caminhoDaLista = caminhoDaSecao(estabelecimentoId, 'cardapio');

  return (
    <CascaDaLoja
      atual="catalogo"
      titulo="Cadastro de produtos"
      subtitulo="Crie e edite o que a loja vende."
    >
      {leitura.estado === 'erro' && (
        <div className="prod-aviso" role="status">
          Não deu para carregar os produtos: {leitura.frase}
        </div>
      )}
      {emDemonstracao() && (
        <div className="prod-aviso" role="status">
          Modo demonstração: o que você salvar fica só nesta aba e aparece no cardápio de
          demonstração.
        </div>
      )}

      <section className="panel prod-panel" id="vistaLista">
        <div className="table-title">
          <div>
            <h2>Produtos da loja</h2>
            <p className="admin-muted" id="prodResumo">
              {leitura.estado === 'carregando'
                ? 'Carregando…'
                : `${catalogo.itens.length} produto(s), ${publicados} publicado(s)${emDemonstracao() ? ' · demonstração' : ''}`}
            </p>
          </div>
          {(fonte.editaTudo || fonte.podeCriar) && (
            <Link
              className="btn btn-primary prod-novo"
              id="btnNovo"
              to={caminhoDoNovoProduto(estabelecimentoId)}
            >
              <span className="material-symbols-outlined">add</span>Novo produto
            </Link>
          )}
        </div>
        <div className="prod-toolbar">
          <label className="search">
            <span className="material-symbols-outlined">search</span>
            <input
              id="prodBusca"
              type="search"
              placeholder="Buscar pelo nome ou ingrediente"
              aria-label="Buscar produto"
              value={busca}
              onChange={(evento) => definirBusca(evento.target.value)}
            />
          </label>
          <select
            className="select"
            id="prodFiltroTipo"
            aria-label="Filtrar por tipo"
            value={tipo}
            onChange={(evento) => definirTipo(evento.target.value)}
          >
            <option value="">Todos os tipos</option>
            {catalogo.tipos.map((um) => (
              <option key={um.id} value={um.id}>
                {um.nome}
              </option>
            ))}
          </select>
        </div>
        <div className="table-scroll">
          <table className="orders-table prod-tabela">
            <thead>
              <tr>
                <th>Produto</th>
                <th>Tipo</th>
                <th>Preço</th>
                <th>Situação</th>
                <th>Disponibilidade</th>
                <th>
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody id="prodLinhas">
              {lista.length === 0 ? (
                <tr>
                  <td colSpan={6} className="prod-vazio">
                    {leitura.estado === 'carregando'
                      ? 'Carregando os produtos…'
                      : catalogo.itens.length > 0
                        ? 'Nenhum produto com esse filtro.'
                        : 'Nenhum produto cadastrado ainda. Clique em "Novo produto".'}
                  </td>
                </tr>
              ) : (
                lista.map((item) => {
                  const [rotulo, cor] = ROTULO_DA_SITUACAO[item.situacao];
                  return (
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
                            {item.ingredientes.length > 0 && (
                              <small>{item.ingredientes.join(', ')}</small>
                            )}
                          </div>
                        </div>
                      </td>
                      <td>{item.tipo}</td>
                      <td>{dinheiro(item.preco)}</td>
                      <td>
                        <span className={`status ${cor}`}>{rotulo}</span>
                      </td>
                      <td>
                        <span className={`status ${COR_DA_DISPONIBILIDADE[item.disponibilidade]}`}>
                          {ROTULO[item.disponibilidade]}
                        </span>
                      </td>
                      <td>
                        <Link
                          className="btn btn-light prod-editar"
                          data-editar={item.id}
                          to={`${caminhoDaLista}/${encodeURIComponent(item.id)}`}
                        >
                          <span className="material-symbols-outlined">edit</span>
                          {rotuloDoBotao}
                        </Link>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Cadastro de produtos
      </footer>
    </CascaDaLoja>
  );
}
