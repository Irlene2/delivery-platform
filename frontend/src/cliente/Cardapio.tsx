import { useEffect, useMemo, useState } from 'react';
import { ESTADOS, ROTULO, type EstadoDeDisponibilidade } from '../api/disponibilidade';
import { dinheiro } from '../aki/formatos';
import { adicionarAoCarrinho } from '../aki/pedidos';
import { toast } from '../aki/toast';
import {
  IMAGEM_PADRAO,
  mensagemDoErro,
  type FonteDoCatalogo,
  type ItemDoCatalogo,
} from '../painel/catalogo';
import { useCatalogo } from '../painel/useCatalogo';
import { SecaoDoCarrinho } from './Carrinho';

/**
 * O cardápio do protótipo (`cardapio.html`): faixa do topo, categorias, busca,
 * os cartões de produto, a janela do produto e o carrinho.
 *
 * O cliente vê **só o que foi publicado**. Com o servidor, a descrição chega ao
 * abrir o produto, e quem tem `ALTERAR_PRODUTO` marca a disponibilidade ali.
 */
const ICONE_PADRAO = 'restaurant_menu';

export function ConteudoDoCardapio({
  fonte,
  linkDoCardapio,
  linkDosPedidos,
}: {
  fonte: FonteDoCatalogo;
  linkDoCardapio: string;
  linkDosPedidos: string;
}) {
  const { leitura, trocar } = useCatalogo(fonte);
  const [categoria, definirCategoria] = useState('Todos');
  const [busca, definirBusca] = useState('');
  const [aberto, definirAberto] = useState<ItemDoCatalogo | null>(null);
  const [quantidade, definirQuantidade] = useState(1);
  const [estadoNoModal, definirEstadoNoModal] = useState<EstadoDeDisponibilidade>('DISPONIVEL');
  const [mensagemNoModal, definirMensagemNoModal] = useState('');
  const [salvandoNoModal, definirSalvandoNoModal] = useState(false);

  const publicados = useMemo(
    () =>
      leitura.estado === 'pronto'
        ? leitura.catalogo.itens
            .filter((item) => item.situacao === 'ATIVO')
            .sort((a, b) => a.ordem - b.ordem)
        : [],
    [leitura],
  );
  const categorias: [string, string][] = useMemo(() => {
    if (leitura.estado !== 'pronto') return [['Todos', ICONE_PADRAO]];
    const usadas = leitura.catalogo.tipos.filter((tipo) =>
      publicados.some((item) => item.tipoId === tipo.id),
    );
    return [
      ['Todos', ICONE_PADRAO],
      ...usadas.map((tipo): [string, string] => [tipo.nome, tipo.icone]),
    ];
  }, [leitura, publicados]);

  const termo = busca.trim().toLowerCase();
  const filtrados = publicados.filter((item) => {
    const daCategoria = categoria === 'Todos' || item.tipo === categoria;
    return (
      daCategoria && `${item.nome} ${item.descricao} ${item.tipo}`.toLowerCase().includes(termo)
    );
  });

  function selo(item: ItemDoCatalogo): string {
    if (!item.vendavel) return 'Indisponível';
    if (item.disponibilidade === 'ACABANDO') return 'Acabando';
    return item.selo;
  }

  function adicionar(item: ItemDoCatalogo, vezes = 1) {
    if (!item.vendavel) {
      toast(`${item.nome} está indisponível agora`);
      return;
    }
    adicionarAoCarrinho(
      { id: item.id, name: item.nome, price: item.preco, image: item.imagem || IMAGEM_PADRAO },
      vezes,
    );
    toast(`${item.nome} foi adicionado ao carrinho`);
  }

  function abrir(item: ItemDoCatalogo) {
    definirAberto(item);
    definirQuantidade(1);
    definirEstadoNoModal(item.disponibilidade);
    definirMensagemNoModal('');
    if (!fonte.editaTudo) {
      // A listagem do servidor não traz a descrição; o produto inteiro traz.
      fonte
        .abrir(item)
        .then((completo) =>
          definirAberto((atual) =>
            atual?.id === item.id
              ? { ...atual, descricao: completo.descricao, ingredientes: completo.ingredientes }
              : atual,
          ),
        )
        .catch(() => undefined);
    }
  }

  function fechar() {
    definirAberto(null);
  }

  useEffect(() => {
    document.body.classList.toggle('modal-open', aberto !== null);
    if (aberto === null) return;
    const tecla = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') definirAberto(null);
    };
    document.addEventListener('keydown', tecla);
    return () => {
      document.removeEventListener('keydown', tecla);
      document.body.classList.remove('modal-open');
    };
  }, [aberto]);

  async function salvarDisponibilidade() {
    if (aberto === null || leitura.estado !== 'pronto') return;
    definirSalvandoNoModal(true);
    definirMensagemNoModal('');
    try {
      const atualizado = await fonte.marcar(aberto, estadoNoModal);
      trocar({
        ...leitura.catalogo,
        itens: leitura.catalogo.itens.map((um) =>
          um.id === atualizado.id ? { ...um, ...atualizado } : um,
        ),
      });
      definirAberto({ ...aberto, ...atualizado });
      toast(`${aberto.nome}: ${ROTULO[atualizado.disponibilidade]}`);
      definirMensagemNoModal('Salvo.');
    } catch (erro) {
      definirMensagemNoModal(mensagemDoErro(erro));
    } finally {
      definirSalvandoNoModal(false);
    }
  }

  const descricaoNoModal =
    aberto === null
      ? ''
      : `${aberto.descricao}${aberto.ingredientes.length > 0 ? ` Ingredientes: ${aberto.ingredientes.join(', ')}.` : ''}`.trim();

  return (
    <>
      <section className="menu-hero professional-banner">
        <img
          src="https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=1400&q=75"
          alt="Sanduíche artesanal com batatas"
        />
        <div className="menu-hero-content">
          <span className="eyebrow">AKI LANCHES • DELIVERY</span>
          <h2>
            O sabor que você
            <br />
            <strong>estava esperando.</strong>
          </h2>
          <p>Hambúrgueres, combos, porções, hot dogs, bebidas e sobremesas.</p>
          <a className="btn btn-primary" href="#menuContent">
            <span className="material-symbols-outlined">restaurant_menu</span>Explorar cardápio
          </a>
        </div>
      </section>

      <section className="menu-intro">
        <div>
          <span className="eyebrow orange-text">CARDÁPIO ONLINE</span>
          <h2>Escolha seu próximo favorito</h2>
          <p>Fotos, descrição, preço e adição rápida ao carrinho em poucos cliques.</p>
        </div>
        <div className="menu-highlights">
          <span>
            <span className="material-symbols-outlined">local_fire_department</span> Mais pedidos
          </span>
          <span>
            <span className="material-symbols-outlined">schedule</span> Preparo rápido
          </span>
          <span>
            <span className="material-symbols-outlined">verified</span> Pedido seguro
          </span>
        </div>
      </section>

      <section id="categorias" className="category-section">
        <div className="section-title">
          <div className="title-icon">
            <span className="material-symbols-outlined">category</span>
          </div>
          <div>
            <small>EXPLORE</small>
            <h2>Categorias</h2>
            <p>Encontre rapidamente o que deseja.</p>
          </div>
        </div>
        <div className="category-pills" id="categoryPills">
          {categorias.map(([nome, icone]) => (
            <button
              key={nome}
              type="button"
              className={`category-pill${categoria === nome ? ' active' : ''}`}
              data-category={nome}
              onClick={() => definirCategoria(nome)}
            >
              <span className="material-symbols-outlined">{icone}</span>
              {nome}
            </button>
          ))}
        </div>
      </section>

      <div className="menu-toolbar">
        <div className="search">
          <span className="material-symbols-outlined">search</span>
          <input
            id="searchInput"
            placeholder="Buscar hambúrguer, combo, bebida..."
            value={busca}
            onChange={(evento) => definirBusca(evento.target.value)}
          />
        </div>
        <select
          className="select"
          id="categoryFilter"
          aria-label="Filtrar categoria"
          value={categoria}
          onChange={(evento) => definirCategoria(evento.target.value)}
        >
          {categorias.map(([nome]) => (
            <option key={nome} value={nome}>
              {nome}
            </option>
          ))}
        </select>
      </div>

      <section id="menuContent" className="products" aria-live="polite">
        {leitura.estado === 'carregando' ? (
          <div className="empty-menu">
            <strong>Carregando o cardápio da loja…</strong>
          </div>
        ) : leitura.estado === 'erro' ? (
          <div className="empty-menu">
            <span className="material-symbols-outlined">cloud_off</span>
            <strong>Não deu para carregar o cardápio</strong>
            <p>{leitura.frase}</p>
          </div>
        ) : publicados.length === 0 ? (
          <div className="empty-menu">
            <span className="material-symbols-outlined">restaurant_menu</span>
            <strong>Nenhum produto publicado ainda</strong>
            <p>Publique produtos pelo cadastro de produtos para eles aparecerem aqui.</p>
          </div>
        ) : filtrados.length === 0 ? (
          <div className="empty-menu">
            <span className="material-symbols-outlined">search_off</span>
            <strong>Nenhum produto encontrado</strong>
            <p>Tente outro nome ou categoria.</p>
          </div>
        ) : (
          filtrados.map((item) => {
            const rotulo = selo(item);
            return (
              <article
                key={item.id}
                className="product-card"
                data-product-id={item.id}
                tabIndex={0}
                role="button"
                aria-label={`Ver detalhes de ${item.nome}`}
                onClick={() => abrir(item)}
                onKeyDown={(evento) => {
                  if (evento.key === 'Enter' || evento.key === ' ') {
                    evento.preventDefault();
                    abrir(item);
                  }
                }}
              >
                {rotulo !== '' && <span className="badge">{rotulo}</span>}
                <img
                  src={item.imagem || IMAGEM_PADRAO}
                  alt={item.nome}
                  loading="lazy"
                  decoding="async"
                  onError={(evento) => {
                    evento.currentTarget.src = IMAGEM_PADRAO;
                  }}
                />
                <div className="product-body">
                  <span className="product-category">{item.tipo}</span>
                  <h3>{item.nome}</h3>
                  <p>{item.descricao}</p>
                  <div className="product-bottom">
                    <span className="price">{dinheiro(item.preco)}</span>
                    <button
                      className="add"
                      type="button"
                      aria-label={`Adicionar ${item.nome}`}
                      disabled={!item.vendavel}
                      onClick={(evento) => {
                        evento.stopPropagation();
                        adicionar(item);
                      }}
                    >
                      <span className="material-symbols-outlined">add</span>
                    </button>
                  </div>
                </div>
              </article>
            );
          })
        )}
      </section>

      <SecaoDoCarrinho
        variante="cardapio"
        linkDoCardapio={linkDoCardapio}
        linkDosPedidos={linkDosPedidos}
      />

      <div
        className="product-modal"
        id="productModal"
        hidden={aberto === null}
        aria-hidden={aberto === null}
      >
        <div className="product-modal-backdrop" data-close-modal="" onClick={fechar} />
        {aberto !== null && (
          <div
            className="product-modal-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="modalProductName"
          >
            <button
              className="modal-close"
              type="button"
              data-close-modal=""
              aria-label="Fechar detalhes"
              onClick={fechar}
            >
              <span className="material-symbols-outlined">close</span>
            </button>
            <div className="modal-product-image-wrap">
              <img id="modalProductImage" src={aberto.imagem || IMAGEM_PADRAO} alt={aberto.nome} />
              {selo(aberto) !== '' && (
                <span id="modalProductTag" className="badge">
                  {selo(aberto)}
                </span>
              )}
            </div>
            <div className="modal-product-content">
              <span id="modalProductCategory" className="product-category">
                {aberto.tipo}
              </span>
              <h2 id="modalProductName">{aberto.nome}</h2>
              <div id="modalProductPrice" className="modal-product-price">
                {dinheiro(aberto.preco)}
              </div>
              <p id="modalProductDescription">{descricaoNoModal}</p>
              <div className="modal-highlights">
                <span>
                  <span className="material-symbols-outlined">verified</span> Produto selecionado
                </span>
                <span>
                  <span className="material-symbols-outlined">shopping_cart</span> Adição rápida ao
                  carrinho
                </span>
              </div>
              <div className="modal-product-actions">
                <div className="modal-qty">
                  <button
                    type="button"
                    id="modalQtyMinus"
                    onClick={() => definirQuantidade((q) => Math.max(1, q - 1))}
                  >
                    −
                  </button>
                  <strong id="modalQty">{quantidade}</strong>
                  <button
                    type="button"
                    id="modalQtyPlus"
                    onClick={() => definirQuantidade((q) => q + 1)}
                  >
                    +
                  </button>
                </div>
                <button
                  type="button"
                  className="btn btn-primary modal-add-btn"
                  id="modalAddButton"
                  onClick={() => {
                    adicionar(aberto, quantidade);
                    fechar();
                  }}
                >
                  <span className="material-symbols-outlined">shopping_cart</span>Adicionar ao
                  carrinho
                </button>
              </div>

              {!fonte.editaTudo && fonte.podeAlterar && aberto.controle !== 'SEM_CONTROLE' && (
                <div id="akiDisponibilidade" className="payment-checkout">
                  <div className="summary-heading">
                    <span>Disponibilidade hoje</span>
                    <span className="material-symbols-outlined section-icon">inventory_2</span>
                  </div>
                  <label className="payment-select-label" htmlFor="akiEstado">
                    Marque como este produto está agora
                  </label>
                  <select
                    id="akiEstado"
                    className="payment-select"
                    value={estadoNoModal}
                    onChange={(evento) =>
                      definirEstadoNoModal(evento.target.value as EstadoDeDisponibilidade)
                    }
                  >
                    {ESTADOS.map((estado) => (
                      <option key={estado} value={estado}>
                        {ROTULO[estado]}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="btn btn-light full"
                    id="akiSalvarEstado"
                    style={{ marginTop: '10px' }}
                    disabled={salvandoNoModal}
                    onClick={() => void salvarDisponibilidade()}
                  >
                    Salvar disponibilidade
                  </button>
                  <small
                    id="akiEstadoMsg"
                    role="alert"
                    style={{ display: 'block', marginTop: '8px' }}
                  >
                    {mensagemNoModal}
                  </small>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </>
  );
}
