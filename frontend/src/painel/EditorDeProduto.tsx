import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import { ESTADOS, ROTULO, type EstadoDeDisponibilidade } from '../api/disponibilidade';
import { dinheiro } from '../aki/formatos';
import { toast } from '../aki/toast';
import {
  IMAGEM_PADRAO,
  lerPreco,
  mensagemDoErro,
  NOVO_TIPO,
  type Controle,
  type DadosDoFormulario,
  type ItemDoCatalogo,
  type Situacao,
} from './catalogo';
import { CascaDaLoja } from './CascaDaLoja';
import { caminhoDaSecao } from './caminhos';
import { reduzirFoto } from './foto';
import { useLojaAtual } from './lojaAtual';
import { useCatalogo } from './useCatalogo';

/**
 * Novo produto e Editar produto — o formulário do cadastro de produtos do
 * protótipo, com a prévia de como o cliente vê.
 *
 * Com o servidor, num produto que já existe, nome, tipo, descrição,
 * ingredientes e preço ficam travados: ainda não há rota para alterá-los. Dá
 * para publicar, marcar a disponibilidade e marcar cada opção.
 */
const SELOS = ['Mais pedido', 'Novo', 'Favorito', 'Oferta', 'Economize', 'Natural'];

interface Campos {
  nome: string;
  tipoId: string;
  tipoNovo: string;
  descricao: string;
  ingredientes: string[];
  precoTexto: string;
  ordem: string;
  controle: Controle;
  situacao: Situacao;
  disponibilidade: EstadoDeDisponibilidade;
  imagem: string;
  selo: string;
}

type Erros = Partial<Record<'nome' | 'tipoId' | 'tipoNovo' | 'preco', string>>;

const precoNoCampo = (valor: number | undefined) =>
  valor === undefined ? '' : valor.toFixed(2).replace('.', ',');

function camposDe(item: ItemDoCatalogo | null, posicao: number): Campos {
  return {
    nome: item?.nome ?? '',
    tipoId: item?.tipoId ?? '',
    tipoNovo: '',
    descricao: item?.descricao ?? '',
    ingredientes: [...(item?.ingredientes ?? [])],
    precoTexto: precoNoCampo(item?.preco),
    ordem: String(item?.ordem ?? posicao),
    controle: item?.controle ?? 'QUALITATIVO',
    situacao: item?.situacao ?? 'RASCUNHO',
    disponibilidade: item?.disponibilidade ?? 'DISPONIVEL',
    imagem: item?.imagem ?? '',
    selo: item?.selo ?? '',
  };
}

export function EditorDeProduto() {
  const { produtoId } = useParams();
  const { estabelecimentoId, fonte } = useLojaAtual();
  const { leitura, trocar } = useCatalogo(fonte);
  const navegar = useNavigate();
  const local = useLocation();
  const avisoVindo = (local.state as { aviso?: string } | null)?.aviso ?? null;
  const caminhoDaLista = caminhoDaSecao(estabelecimentoId, 'cardapio');

  const [item, definirItem] = useState<ItemDoCatalogo | null>(null);
  const [aberto, definirAberto] = useState(produtoId === undefined);
  const [falhaAoAbrir, definirFalhaAoAbrir] = useState<string | null>(null);
  const [campos, definirCampos] = useState<Campos>(() => camposDe(null, 0));
  const [foto, definirFoto] = useState('');
  const [ingrediente, definirIngrediente] = useState('');
  const [erros, definirErros] = useState<Erros>({});
  const [mensagem, definirMensagem] = useState<{ texto: string; tom: 'ok' | 'erro' } | null>(
    avisoVindo === null ? null : { texto: avisoVindo, tom: 'erro' },
  );
  const [salvando, definirSalvando] = useState(false);
  const [confirmarSaida, definirConfirmarSaida] = useState(false);
  const [estadoDasOpcoes, definirEstadoDasOpcoes] = useState<Record<string, string>>({});
  const campoNome = useRef<HTMLInputElement>(null);
  const campoIngrediente = useRef<HTMLInputElement>(null);

  const catalogo = leitura.estado === 'pronto' ? leitura.catalogo : null;

  // Abre o produto (ou começa um novo) quando o catálogo chega.
  useEffect(() => {
    if (catalogo === null) return;
    if (produtoId === undefined) {
      const novos = camposDe(null, 0);
      definirItem(null);
      definirCampos(novos);
      definirFoto(JSON.stringify(novos));
      definirAberto(true);
      return;
    }
    const daLista = catalogo.itens.find((um) => um.id === produtoId);
    if (daLista === undefined) {
      definirFalhaAoAbrir('Este produto não está mais nesta loja.');
      return;
    }
    let atual = true;
    fonte
      .abrir(daLista)
      .then((completo) => {
        if (!atual) return;
        const preenchidos = camposDe(completo, completo.ordem);
        definirItem(completo);
        definirCampos(preenchidos);
        definirFoto(JSON.stringify(preenchidos));
        definirAberto(true);
      })
      .catch((erro: unknown) => {
        if (atual) definirFalhaAoAbrir(mensagemDoErro(erro));
      });
    return () => {
      atual = false;
    };
    // O catálogo muda de identidade a cada salvamento; reabrir só quando o produto muda.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [produtoId, catalogo === null, fonte]);

  // ---------- travas ----------
  const doServidor = !fonte.editaTudo;
  const semPermissao = doServidor && (item === null ? !fonte.podeCriar : !fonte.podeAlterar);
  // A posição e o "pode acabar" são do cadastro inicial: a correção do cadastro (ADR-077,
  // emenda de 01/11/2026) muda seção, nome, descrição, ingredientes e preço.
  const travarDados = doServidor && item !== null;
  const travarTudo = semPermissao;
  const nota = semPermissao
    ? item === null
      ? 'Você não tem permissão para criar produtos nesta loja.'
      : 'Você pode ver este produto, mas não tem a permissão para alterá-lo nesta loja.'
    : null;
  const [preparandoFoto, definirPreparandoFoto] = useState(false);

  async function escolherFoto(arquivo: File | undefined) {
    if (arquivo === undefined) return;
    definirPreparandoFoto(true);
    try {
      alterar('imagem', await reduzirFoto(arquivo));
    } catch (erro) {
      definirMensagem({
        tom: 'erro',
        texto: erro instanceof Error ? erro.message : 'Não foi possível usar esta foto.',
      });
    } finally {
      definirPreparandoFoto(false);
    }
  }
  const situacaoFixa = doServidor && item?.situacao === 'ATIVO';

  const alterar = <K extends keyof Campos>(campo: K, valor: Campos[K]) => {
    definirCampos((atual) => ({ ...atual, [campo]: valor }));
    definirConfirmarSaida(false);
  };

  function adicionarIngrediente() {
    const novos = ingrediente
      .split(',')
      .map((parte) => parte.trim())
      .filter((parte) => parte !== '');
    const lista = [...campos.ingredientes];
    for (const novo of novos) {
      if (!lista.some((um) => um.toLowerCase() === novo.toLowerCase())) lista.push(novo);
    }
    alterar('ingredientes', lista);
    definirIngrediente('');
    campoIngrediente.current?.focus();
  }

  const preco = lerPreco(campos.precoTexto);
  const sujo = foto !== '' && JSON.stringify(campos) !== foto;

  function validar(): boolean {
    const novos: Erros = {};
    if (campos.nome.trim() === '') novos.nome = 'Dê um nome ao produto.';
    if (campos.tipoId === '') novos.tipoId = 'Escolha o tipo do produto.';
    if (campos.tipoId === NOVO_TIPO && campos.tipoNovo.trim() === '')
      novos.tipoNovo = 'Escreva o nome do novo tipo.';
    if (preco === null) novos.preco = 'Use um valor como 25,90.';
    definirErros(novos);
    const primeiro = Object.keys(novos)[0];
    if (primeiro !== undefined) {
      const ids: Record<string, string> = {
        nome: 'fNome',
        tipoId: 'fTipo',
        tipoNovo: 'fTipoNovo',
        preco: 'fPreco',
      };
      document.getElementById(ids[primeiro] ?? '')?.focus();
    }
    return primeiro === undefined;
  }

  async function salvar(evento: FormEvent) {
    evento.preventDefault();
    if (salvando || catalogo === null) return; // evita salvar duas vezes
    definirMensagem(null);
    if (!validar() || preco === null) return;

    const dados: DadosDoFormulario = {
      nome: campos.nome.trim(),
      tipoId: campos.tipoId,
      tipoNovo: campos.tipoNovo.trim(),
      descricao: campos.descricao,
      ingredientes: campos.ingredientes,
      preco,
      ordem: Math.max(0, Number.parseInt(campos.ordem, 10) || 0),
      controle: campos.controle,
      situacao: campos.situacao,
      disponibilidade: campos.disponibilidade,
      imagem: campos.imagem.trim(),
      selo: campos.selo,
    };

    definirSalvando(true);
    try {
      const resultado = await fonte.salvar(dados, item, catalogo);
      trocar(resultado.catalogo);
      if (resultado.aviso !== null) {
        // O produto existe: reabre como edição, para não criar outro ao tentar de novo.
        void navegar(`${caminhoDaLista}/${encodeURIComponent(resultado.item.id)}`, {
          replace: true,
          state: { aviso: resultado.aviso },
        });
        definirItem(resultado.item);
        definirMensagem({ texto: resultado.aviso, tom: 'erro' });
        return;
      }
      toast(`${resultado.item.nome} salvo`);
      void navegar(caminhoDaLista);
    } catch (erro) {
      definirMensagem({ texto: mensagemDoErro(erro), tom: 'erro' });
    } finally {
      definirSalvando(false);
    }
  }

  function pedirParaSair() {
    if (sujo) {
      definirConfirmarSaida(true);
      return;
    }
    void navegar(caminhoDaLista);
  }

  // Esc também sai, como no protótipo.
  useEffect(() => {
    const tecla = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') {
        if (sujo) definirConfirmarSaida(true);
        else void navegar(caminhoDaLista);
      }
    };
    document.addEventListener('keydown', tecla);
    return () => document.removeEventListener('keydown', tecla);
  }, [sujo, navegar, caminhoDaLista]);

  useEffect(() => {
    if (aberto && !travarDados && !travarTudo) campoNome.current?.focus();
  }, [aberto, travarDados, travarTudo]);

  async function marcarOpcao(grupoId: string, opcaoId: string, estado: EstadoDeDisponibilidade) {
    if (item === null) return;
    const chave = `${grupoId}/${opcaoId}`;
    definirEstadoDasOpcoes((atual) => ({ ...atual, [chave]: 'Salvando…' }));
    try {
      const atualizado = await fonte.marcarOpcao(item, grupoId, opcaoId, estado);
      definirItem(atualizado);
      definirEstadoDasOpcoes((atual) => ({ ...atual, [chave]: 'Salvo.' }));
    } catch (erro) {
      definirEstadoDasOpcoes((atual) => ({ ...atual, [chave]: mensagemDoErro(erro) }));
    }
  }

  // ---------- prévia ----------
  const nomeDoTipo = useMemo(() => {
    if (campos.tipoId === NOVO_TIPO) return campos.tipoNovo;
    return catalogo?.tipos.find((tipo) => tipo.id === campos.tipoId)?.nome ?? '';
  }, [campos.tipoId, campos.tipoNovo, catalogo]);
  let seloDaPrevia = fonte.editaTudo ? campos.selo : '';
  if (campos.controle === 'QUALITATIVO' && campos.disponibilidade !== 'DISPONIVEL') {
    seloDaPrevia = ROTULO[campos.disponibilidade];
  }
  const imagemDaPrevia = campos.imagem.trim() || IMAGEM_PADRAO;

  const titulo = item === null ? 'Novo produto' : `Editar: ${item.nome}`;

  return (
    <CascaDaLoja
      atual="catalogo"
      titulo="Cadastro de produtos"
      subtitulo="Crie e edite o que a loja vende."
    >
      {falhaAoAbrir !== null && (
        <div className="prod-aviso" role="status">
          Não deu para abrir o produto: {falhaAoAbrir}
        </div>
      )}
      {leitura.estado === 'erro' && (
        <div className="prod-aviso" role="status">
          Não deu para carregar os produtos: {leitura.frase}
        </div>
      )}

      <section className="panel prod-panel" id="vistaEditor">
        <div className="table-title">
          <div>
            <h2 id="editorTitulo">{aberto ? titulo : 'Abrindo o produto…'}</h2>
            <p className="admin-muted" id="editorSub">
              {item === null
                ? 'Preencha o que o cliente vai ver no cardápio.'
                : 'Mude o que precisar e salve.'}
            </p>
          </div>
          <button
            type="button"
            className="btn btn-light prod-sair"
            id="btnSairTopo"
            onClick={pedirParaSair}
          >
            <span className="material-symbols-outlined">arrow_back</span>Sair e voltar à lista
          </button>
        </div>

        {nota !== null && (
          <div id="editorTrava" className="prod-nota">
            {nota}
          </div>
        )}

        {aberto && catalogo !== null && (
          <div className="prod-editor">
            <form
              id="prodForm"
              className="prod-form"
              noValidate
              onSubmit={(evento) => void salvar(evento)}
            >
              <fieldset className="prod-grupo" disabled={travarTudo}>
                <legend>O produto</legend>
                <label className="prod-campo">
                  Nome do produto *
                  <input
                    id="fNome"
                    ref={campoNome}
                    maxLength={120}
                    autoComplete="off"
                    placeholder="Ex.: X-Bacon da casa"
                    className={erros.nome !== undefined ? 'campo-invalido' : undefined}
                    value={campos.nome}
                    onChange={(evento) => alterar('nome', evento.target.value)}
                  />
                  <small className="prod-erro">{erros.nome}</small>
                </label>

                <label className="prod-campo">
                  Tipo de produto *
                  <select
                    id="fTipo"
                    className={erros.tipoId !== undefined ? 'campo-invalido' : undefined}
                    value={campos.tipoId}
                    onChange={(evento) => alterar('tipoId', evento.target.value)}
                  >
                    <option value="">Escolha o tipo</option>
                    {catalogo.tipos.map((tipo) => (
                      <option key={tipo.id} value={tipo.id}>
                        {tipo.nome}
                      </option>
                    ))}
                    {(fonte.editaTudo || fonte.podeCriar) && (
                      <option value={NOVO_TIPO}>+ Criar novo tipo…</option>
                    )}
                  </select>
                  <small className="prod-erro">{erros.tipoId}</small>
                </label>
                {campos.tipoId === NOVO_TIPO && (
                  <label className="prod-campo" id="fTipoNovoWrap">
                    Nome do novo tipo *
                    <input
                      id="fTipoNovo"
                      maxLength={60}
                      autoComplete="off"
                      placeholder="Ex.: Pizzas, Açaí, Marmitas"
                      className={erros.tipoNovo !== undefined ? 'campo-invalido' : undefined}
                      value={campos.tipoNovo}
                      onChange={(evento) => alterar('tipoNovo', evento.target.value)}
                    />
                    <small className="prod-erro">{erros.tipoNovo}</small>
                  </label>
                )}

                <label className="prod-campo">
                  Descrição
                  <textarea
                    id="fDescricao"
                    rows={3}
                    maxLength={500}
                    placeholder="Como o cliente lê o produto no cardápio"
                    value={campos.descricao}
                    onChange={(evento) => alterar('descricao', evento.target.value)}
                  />
                </label>

                <div className="prod-campo">
                  <span id="ingRotulo">Ingredientes</span>
                  <div className="prod-ing-entrada">
                    <input
                      id="fIngrediente"
                      ref={campoIngrediente}
                      maxLength={40}
                      autoComplete="off"
                      placeholder="Digite e aperte Enter"
                      aria-labelledby="ingRotulo"
                      value={ingrediente}
                      onChange={(evento) => definirIngrediente(evento.target.value)}
                      onKeyDown={(evento) => {
                        if (evento.key === 'Enter') {
                          evento.preventDefault();
                          adicionarIngrediente();
                        }
                      }}
                    />
                    <button
                      type="button"
                      className="btn btn-light"
                      id="btnIngrediente"
                      onClick={adicionarIngrediente}
                    >
                      <span className="material-symbols-outlined">add</span>Adicionar
                    </button>
                  </div>
                  <ul
                    className="prod-chips"
                    id="listaIngredientes"
                    aria-label="Ingredientes do produto"
                  >
                    {campos.ingredientes.map((um, i) => (
                      <li key={um}>
                        {um}
                        {!travarDados && !travarTudo && (
                          <button
                            type="button"
                            aria-label={`Tirar ${um}`}
                            onClick={() =>
                              alterar(
                                'ingredientes',
                                campos.ingredientes.filter((_, j) => j !== i),
                              )
                            }
                          >
                            <span className="material-symbols-outlined">close</span>
                          </button>
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              </fieldset>

              <fieldset className="prod-grupo" disabled={travarTudo}>
                <legend>Preço e controle</legend>
                <div className="prod-linha">
                  <label className="prod-campo">
                    Preço (R$) *
                    <input
                      id="fPreco"
                      inputMode="decimal"
                      autoComplete="off"
                      placeholder="0,00"
                      className={erros.preco !== undefined ? 'campo-invalido' : undefined}
                      value={campos.precoTexto}
                      onChange={(evento) => alterar('precoTexto', evento.target.value)}
                    />
                    <small className="prod-erro">{erros.preco}</small>
                  </label>
                  <label className="prod-campo">
                    Posição no cardápio
                    <input
                      id="fOrdem"
                      type="number"
                      min={0}
                      step={1}
                      disabled={travarDados}
                      value={campos.ordem}
                      onChange={(evento) => alterar('ordem', evento.target.value)}
                    />
                  </label>
                </div>
                <div className="prod-campo" role="radiogroup" aria-labelledby="ctrlRotulo">
                  <span id="ctrlRotulo">Este produto pode acabar no dia?</span>
                  <label className="prod-opcao">
                    <input
                      type="radio"
                      name="fControle"
                      value="QUALITATIVO"
                      disabled={travarDados}
                      checked={campos.controle === 'QUALITATIVO'}
                      onChange={() => alterar('controle', 'QUALITATIVO')}
                    />{' '}
                    <span>
                      <b>Pode acabar</b>
                      <small>
                        Você marca "acabando" ou "acabou hoje"; ele volta sozinho no próximo
                        expediente.
                      </small>
                    </span>
                  </label>
                  <label className="prod-opcao">
                    <input
                      type="radio"
                      name="fControle"
                      value="SEM_CONTROLE"
                      disabled={travarDados}
                      checked={campos.controle === 'SEM_CONTROLE'}
                      onChange={() => alterar('controle', 'SEM_CONTROLE')}
                    />{' '}
                    <span>
                      <b>Nunca acaba</b>
                      <small>Bebida em lata, taxa, couvert.</small>
                    </span>
                  </label>
                </div>
              </fieldset>

              <fieldset className="prod-grupo" id="grupoSituacao" disabled={travarTudo}>
                <legend>No cardápio</legend>
                <div className="prod-linha">
                  <label className="prod-campo">
                    Situação
                    <select
                      id="fSituacao"
                      value={campos.situacao}
                      disabled={situacaoFixa}
                      onChange={(evento) => alterar('situacao', evento.target.value as Situacao)}
                    >
                      {campos.situacao === 'INATIVO' && (
                        <option value="INATIVO">Inativo (não aparece)</option>
                      )}
                      <option value="RASCUNHO">Rascunho (não aparece)</option>
                      <option value="ATIVO">Publicado (aparece no cardápio)</option>
                    </select>
                  </label>
                  {campos.controle === 'QUALITATIVO' && (
                    <label className="prod-campo" id="fDispWrap">
                      Disponibilidade hoje
                      <select
                        id="fDisponibilidade"
                        value={campos.disponibilidade}
                        onChange={(evento) =>
                          alterar('disponibilidade', evento.target.value as EstadoDeDisponibilidade)
                        }
                      >
                        {ESTADOS.map((estado) => (
                          <option key={estado} value={estado}>
                            {ROTULO[estado]}
                          </option>
                        ))}
                      </select>
                    </label>
                  )}
                </div>
              </fieldset>

              {item !== null && item.grupos.length > 0 && (
                <fieldset className="prod-grupo" id="grupoOpcoes" disabled={travarTudo}>
                  <legend>Opções do produto</legend>
                  <p className="prod-dica">
                    Marque a opção que acabou — ela some da escolha do cliente sem tirar o produto.
                  </p>
                  {item.grupos.map((grupo) => (
                    <div key={grupo.id} className="prod-campo">
                      <span>
                        {grupo.nome} <small className="prod-dica">· {grupo.regra}</small>
                      </span>
                      {grupo.opcoes.map((opcao) => {
                        const chave = `${grupo.id}/${opcao.id}`;
                        return (
                          <div
                            key={opcao.id}
                            className="prod-linha"
                            style={{ alignItems: 'center' }}
                          >
                            <span style={{ fontWeight: 500 }}>
                              {opcao.nome}
                              {opcao.acrescimo !== 0 &&
                                ` (${opcao.acrescimo > 0 ? '+' : ''}${dinheiro(opcao.acrescimo)})`}
                            </span>
                            <div>
                              <select
                                aria-label={`Disponibilidade de ${opcao.nome}`}
                                value={opcao.disponibilidade ?? 'DISPONIVEL'}
                                disabled={!fonte.editaTudo && !fonte.podeAlterar}
                                onChange={(evento) =>
                                  void marcarOpcao(
                                    grupo.id,
                                    opcao.id,
                                    evento.target.value as EstadoDeDisponibilidade,
                                  )
                                }
                              >
                                {ESTADOS.map((estado) => (
                                  <option key={estado} value={estado}>
                                    {ROTULO[estado]}
                                  </option>
                                ))}
                              </select>
                              {estadoDasOpcoes[chave] !== undefined && (
                                <small className="prod-dica" role="status">
                                  {estadoDasOpcoes[chave]}
                                </small>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ))}
                </fieldset>
              )}

              <fieldset className="prod-grupo" id="grupoVitrine" disabled={travarTudo}>
                <legend>Vitrine</legend>
                <label className="prod-campo">
                  Foto do produto
                  <input
                    id="fImagem"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={(evento) => void escolherFoto(evento.target.files?.[0])}
                  />
                  <small className="prod-dica">
                    {preparandoFoto
                      ? 'Preparando a foto…'
                      : 'JPG, PNG ou WebP. A foto é reduzida antes de enviar.'}
                  </small>
                </label>
                {!fonte.editaTudo && (
                  <p className="prod-dica" id="vitrineNota">
                    O selo vale só na demonstração.
                  </p>
                )}
                <label className="prod-campo">
                  Selo
                  <select
                    id="fSelo"
                    disabled={!fonte.editaTudo}
                    value={campos.selo}
                    onChange={(evento) => alterar('selo', evento.target.value)}
                  >
                    <option value="">Sem selo</option>
                    {SELOS.map((selo) => (
                      <option key={selo}>{selo}</option>
                    ))}
                  </select>
                </label>
              </fieldset>

              <div className="prod-acoes">
                {!travarTudo && (
                  <button
                    type="submit"
                    className="btn btn-primary"
                    id="btnSalvar"
                    aria-busy={salvando}
                  >
                    <span className="material-symbols-outlined">save</span>
                    <span id="btnSalvarTexto">
                      {salvando
                        ? 'Salvando…'
                        : item === null
                          ? 'Salvar produto'
                          : 'Salvar alterações'}
                    </span>
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-light"
                  id="btnSair"
                  onClick={pedirParaSair}
                >
                  <span className="material-symbols-outlined">close</span>Sair sem salvar
                </button>
              </div>
              {confirmarSaida && (
                <div
                  className="prod-confirmar"
                  id="confirmarSaida"
                  role="alertdialog"
                  aria-labelledby="confirmarTexto"
                >
                  <span id="confirmarTexto">Há alterações que ainda não foram salvas.</span>
                  <button
                    type="button"
                    className="btn btn-danger-soft"
                    id="btnDescartar"
                    onClick={() => void navegar(caminhoDaLista)}
                  >
                    Descartar e sair
                  </button>
                  <button
                    type="button"
                    className="btn btn-light"
                    id="btnContinuar"
                    onClick={() => definirConfirmarSaida(false)}
                  >
                    Continuar editando
                  </button>
                </div>
              )}
              {mensagem !== null && (
                <p className={`prod-msg ${mensagem.tom}`} id="formMsg" role="alert">
                  {mensagem.texto}
                </p>
              )}
            </form>

            <aside className="prod-previa" aria-label="Prévia no cardápio">
              <small className="prod-previa-rotulo">Como o cliente vê</small>
              <article className="product-card prod-previa-card">
                {seloDaPrevia !== '' && (
                  <span className="badge" id="pvSelo">
                    {seloDaPrevia}
                  </span>
                )}
                <img
                  id="pvImagem"
                  src={imagemDaPrevia}
                  alt=""
                  decoding="async"
                  onError={(evento) => {
                    if (!evento.currentTarget.src.endsWith(IMAGEM_PADRAO))
                      evento.currentTarget.src = IMAGEM_PADRAO;
                  }}
                />
                <div className="product-body">
                  <span className="product-category" id="pvTipo">
                    {nomeDoTipo || 'Tipo'}
                  </span>
                  <h3 id="pvNome">{campos.nome || 'Nome do produto'}</h3>
                  <p id="pvDescricao">{campos.descricao.trim()}</p>
                  <p className="prod-previa-ing" id="pvIngredientes">
                    {campos.ingredientes.length > 0
                      ? `Ingredientes: ${campos.ingredientes.join(', ')}`
                      : ''}
                  </p>
                  <div className="product-bottom">
                    <span className="price" id="pvPreco">
                      {preco === null ? 'R$ —' : dinheiro(preco)}
                    </span>
                  </div>
                </div>
              </article>
            </aside>
          </div>
        )}
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Cadastro de produtos
      </footer>
    </CascaDaLoja>
  );
}
