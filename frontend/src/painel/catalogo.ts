import {
  alterarProduto,
  criarProduto,
  criarSecao,
  enviarFoto,
  fraseDaRecusa,
  publicarProduto,
} from '../api/cardapio';
import { chamar, urlDaApi } from '../api/cliente';
import {
  estadoConhecido,
  MarcacaoRecusada,
  marcarDisponibilidade,
  marcarDisponibilidadeDaOpcao,
  type EstadoDeDisponibilidade,
} from '../api/disponibilidade';
import type { components } from '../api/generated/catalog';
import { PRODUTOS_DE_EXEMPLO, TIPOS_DE_EXEMPLO } from '../aki/exemplos';
import { gravar, ler } from '../aki/guardado';
import { caminhoDasSecoes, TAMANHO_DO_CARDAPIO } from './cardapio';
import { partesDaFoto } from './foto';
import { caminhoDoProduto, regraDoGrupo } from './produtoInteiro';
import { caminhoDosProdutos } from './produtos';
import type { Permissao } from './tipos';

/**
 * O catálogo da loja como as telas do protótipo o usam — cadastro de produtos,
 * disponibilidade e cardápio —, venha ele do servidor ou da demonstração.
 *
 * ## O que o servidor sabe hoje
 *
 * Criar seção, criar produto (nasce rascunho), publicar, marcar disponibilidade
 * do produto e da opção. **Não há rota para alterar** nome, seção, descrição ou
 * preço de um produto existente, nem para criar grupos de opções: a tela trava
 * esses campos e diz por quê.
 *
 * Ingrediente não tem campo no contrato. Vai no fim da descrição, numa linha
 * `Ingredientes: a, b, c`, e volta separado ao abrir o produto. Foto e selo não
 * existem no servidor: valem só na demonstração.
 */

export type Situacao = 'RASCUNHO' | 'ATIVO' | 'INATIVO';
export type Controle = 'SEM_CONTROLE' | 'QUALITATIVO';

export interface TipoDeProduto {
  readonly id: string;
  readonly nome: string;
  readonly ordem: number;
  readonly icone: string;
}

export interface OpcaoDoItem {
  readonly id: string;
  readonly nome: string;
  readonly acrescimo: number;
  disponibilidade: EstadoDeDisponibilidade | null;
}

export interface GrupoDoItem {
  readonly id: string;
  readonly nome: string;
  readonly regra: string;
  readonly opcoes: OpcaoDoItem[];
}

export interface ItemDoCatalogo {
  readonly id: string;
  nome: string;
  tipoId: string;
  tipo: string;
  descricao: string;
  ingredientes: string[];
  preco: number;
  ordem: number;
  controle: Controle | null;
  situacao: Situacao;
  disponibilidade: EstadoDeDisponibilidade;
  vendavel: boolean;
  imagem: string;
  selo: string;
  grupos: GrupoDoItem[];
}

export interface DadosDoFormulario {
  readonly nome: string;
  readonly tipoId: string;
  readonly tipoNovo: string;
  readonly descricao: string;
  readonly ingredientes: readonly string[];
  readonly preco: number;
  readonly ordem: number;
  readonly controle: Controle;
  readonly situacao: Situacao;
  readonly disponibilidade: EstadoDeDisponibilidade;
  readonly imagem: string;
  readonly selo: string;
}

export interface Catalogo {
  readonly tipos: TipoDeProduto[];
  readonly itens: ItemDoCatalogo[];
}

export interface ResultadoDoSalvamento {
  readonly item: ItemDoCatalogo;
  readonly catalogo: Catalogo;
  /** O produto foi salvo, mas um passo depois dele (publicar, marcar) não. */
  readonly aviso: string | null;
}

export interface FonteDoCatalogo {
  /** Demonstração: tudo editável. Servidor: só o que tem rota. */
  readonly editaTudo: boolean;
  readonly podeCriar: boolean;
  readonly podeAlterar: boolean;
  carregar(): Promise<Catalogo>;
  abrir(item: ItemDoCatalogo): Promise<ItemDoCatalogo>;
  salvar(
    dados: DadosDoFormulario,
    original: ItemDoCatalogo | null,
    catalogo: Catalogo,
  ): Promise<ResultadoDoSalvamento>;
  marcar(item: ItemDoCatalogo, estado: EstadoDeDisponibilidade): Promise<ItemDoCatalogo>;
  marcarOpcao(
    item: ItemDoCatalogo,
    grupoId: string,
    opcaoId: string,
    estado: EstadoDeDisponibilidade,
  ): Promise<ItemDoCatalogo>;
}

export const NOVO_TIPO = '__novo__';
export const IMAGEM_PADRAO = '/assets/banner-hamburguer.jpg';
const MARCA_INGREDIENTES = 'Ingredientes:';

export function juntarDescricao(texto: string, ingredientes: readonly string[]): string {
  const partes: string[] = [];
  if (texto.trim() !== '') partes.push(texto.trim());
  if (ingredientes.length > 0) partes.push(`${MARCA_INGREDIENTES} ${ingredientes.join(', ')}`);
  return partes.join('\n\n');
}

export function separarDescricao(descricao: string | undefined): {
  texto: string;
  ingredientes: string[];
} {
  const texto = descricao ?? '';
  const achado = new RegExp(`(?:^|\\n)${MARCA_INGREDIENTES}\\s*(.*)\\s*$`).exec(texto);
  if (achado === null) return { texto: texto.trim(), ingredientes: [] };
  return {
    texto: texto.slice(0, achado.index).trim(),
    ingredientes: (achado[1] ?? '')
      .split(',')
      .map((parte) => parte.trim())
      .filter((parte) => parte !== ''),
  };
}

/** "25,90", "25.90", "R$ 1.234,56" → número; null se não for um valor. */
export function lerPreco(texto: string): number | null {
  let limpo = texto
    .trim()
    .replace(/^R\$\s*/i, '')
    .replace(/\s/g, '');
  if (limpo.includes(',')) limpo = limpo.replace(/\./g, '').replace(',', '.');
  return /^\d+(\.\d{1,2})?$/.test(limpo) ? Number(limpo) : null;
}

/** A frase de uma recusa, no padrão do projeto (ADR-055): o `detail` do servidor quando há. */
export function mensagemDoErro(erro: unknown): string {
  if (erro instanceof MarcacaoRecusada) return erro.recusa.detail ?? erro.recusa.mensagem;
  return (
    fraseDaRecusa(
      erro,
      { 403: 'Você não tem permissão para isso nesta loja.' },
      'Algo deu errado. Tente de novo.',
    ) ?? 'Sua sessão expirou. Entre de novo.'
  );
}

const esgotado = (estado: EstadoDeDisponibilidade | null): boolean =>
  estado === 'ESGOTADO_HOJE' || estado === 'ESGOTADO_INDETERMINADO';

// ============================== servidor ==============================

type CategoriaDoContrato = components['schemas']['CategoriaResponse'];
type PaginaDoContrato = components['schemas']['PaginaResponseProdutoResumoResponse'];
type ProdutoDoContrato = components['schemas']['ProdutoResponse'];

function gruposDoContrato(produto: ProdutoDoContrato): GrupoDoItem[] {
  return [...(produto.gruposDeOpcoes ?? [])]
    .sort((a, b) => (a.ordem ?? 0) - (b.ordem ?? 0))
    .map((grupo) => ({
      id: String(grupo.id ?? ''),
      nome: grupo.nome ?? '',
      regra: regraDoGrupo(grupo.minEscolhas ?? 0, grupo.maxEscolhas ?? 0),
      opcoes: [...(grupo.opcoes ?? [])]
        .sort((a, b) => (a.ordem ?? 0) - (b.ordem ?? 0))
        .map((opcao) => ({
          id: String(opcao.id ?? ''),
          nome: opcao.nome ?? '',
          acrescimo: opcao.acrescimo ?? 0,
          disponibilidade: estadoConhecido(opcao.disponibilidade),
        })),
    }));
}

export function fonteDoServidor(
  estabelecimentoId: string,
  token: string,
  permissoes: readonly Permissao[],
): FonteDoCatalogo {
  const nomeDoTipo = (tipos: readonly TipoDeProduto[], id: string) =>
    tipos.find((tipo) => tipo.id === id)?.nome ?? 'Sem tipo';

  function doContrato(produto: ProdutoDoContrato, tipos: readonly TipoDeProduto[]): ItemDoCatalogo {
    const { texto, ingredientes } = separarDescricao(produto.descricao);
    const tipoId = String(produto.categoriaId ?? '');
    return {
      id: String(produto.id ?? ''),
      nome: produto.nome ?? '',
      tipoId,
      tipo: nomeDoTipo(tipos, tipoId),
      descricao: texto,
      ingredientes,
      preco: produto.precoBase ?? 0,
      ordem: produto.ordem ?? 0,
      controle: produto.modoDeControle ?? null,
      situacao: produto.estadoDePublicacao ?? 'RASCUNHO',
      disponibilidade: estadoConhecido(produto.disponibilidade) ?? 'DISPONIVEL',
      vendavel: produto.vendavel === true,
      imagem: '',
      selo: '',
      grupos: gruposDoContrato(produto),
    };
  }

  const fonte: FonteDoCatalogo = {
    editaTudo: false,
    podeCriar: permissoes.includes('CRIAR_PRODUTO'),
    podeAlterar: permissoes.includes('ALTERAR_PRODUTO'),

    async carregar() {
      const [secoes, pagina] = await Promise.all([
        chamar<CategoriaDoContrato[]>(caminhoDasSecoes(estabelecimentoId), { token }),
        chamar<PaginaDoContrato>(caminhoDosProdutos(estabelecimentoId, TAMANHO_DO_CARDAPIO), {
          token,
        }),
      ]);
      const tipos: TipoDeProduto[] = (secoes ?? [])
        .filter((secao) => secao.ativa !== false)
        .sort((a, b) => (a.ordem ?? 0) - (b.ordem ?? 0))
        .map((secao) => ({
          id: String(secao.id ?? ''),
          nome: secao.nome ?? '',
          ordem: secao.ordem ?? 0,
          icone: 'restaurant_menu',
        }));
      // A listagem não traz descrição nem grupos: eles chegam ao abrir o produto.
      const itens = (pagina.conteudo ?? []).map((resumo) => doContrato(resumo, tipos));
      return { tipos, itens };
    },

    async abrir(item) {
      const produto = await chamar<ProdutoDoContrato>(
        caminhoDoProduto(estabelecimentoId, item.id),
        { token },
      );
      return { ...doContrato(produto, []), tipo: item.tipo };
    },

    async salvar(dados, original, catalogo) {
      let tipos = catalogo.tipos;
      let itens = catalogo.itens;
      let item = original;

      // A seção nova nasce antes do produto — na criação e na correção do cadastro.
      let categoriaId = dados.tipoId;
      if (categoriaId === NOVO_TIPO) {
        const nova = await criarSecao(
          estabelecimentoId,
          { nome: dados.tipoNovo, ordem: tipos.length },
          token,
        );
        categoriaId = String(nova.id ?? '');
        tipos = [
          ...tipos,
          {
            id: categoriaId,
            nome: nova.nome ?? dados.tipoNovo,
            ordem: nova.ordem ?? 0,
            icone: 'restaurant_menu',
          },
        ];
      }
      const descricao = juntarDescricao(dados.descricao, dados.ingredientes);

      if (item === null) {
        const criado = await criarProduto(
          estabelecimentoId,
          {
            categoriaId,
            nome: dados.nome,
            ...(descricao === '' ? {} : { descricao }),
            precoBase: dados.preco,
            modoDeControle: dados.controle,
            ordem: dados.ordem,
          },
          token,
        );
        item = doContrato(criado, tipos);
        itens = [...itens, item];
      } else {
        const mudou =
          categoriaId !== item.tipoId ||
          dados.nome !== item.nome ||
          dados.preco !== item.preco ||
          descricao !== juntarDescricao(item.descricao, item.ingredientes);
        if (mudou) {
          // Correção do cadastro (ADR-077, emenda de 01/11/2026). A recusa aqui é do produto
          // inteiro: ela sobe, e a tela mostra a frase sem perder o que foi digitado.
          const alterado = await alterarProduto(
            estabelecimentoId,
            item.id,
            { categoriaId, nome: dados.nome, descricao, precoBase: dados.preco },
            token,
          );
          item = { ...doContrato(alterado, tipos), grupos: item.grupos };
        }
      }

      // Publicar, marcar e a foto têm rota própria. Se um falhar, o produto já existe:
      // avisamos sem perder nada, e a tela reabre como edição para não duplicar.
      let atual: ItemDoCatalogo = item;
      let aviso: string | null = null;
      try {
        const foto = partesDaFoto(dados.imagem);
        if (foto !== null) {
          const ilustrado = await enviarFoto(estabelecimentoId, atual.id, foto, token);
          atual = { ...atual, imagem: urlDaApi(ilustrado.imagem) };
        }
        if (dados.situacao === 'ATIVO' && atual.situacao !== 'ATIVO') {
          const publicado = await publicarProduto(estabelecimentoId, atual.id, token);
          atual = {
            ...atual,
            situacao: publicado.estadoDePublicacao ?? 'ATIVO',
            vendavel: publicado.vendavel === true,
          };
        }
        if (atual.controle === 'QUALITATIVO' && dados.disponibilidade !== atual.disponibilidade) {
          atual = await fonte.marcar(atual, dados.disponibilidade);
        }
      } catch (erro) {
        aviso = `O produto foi salvo, mas uma parte não: ${mensagemDoErro(erro)}`;
      }
      const salvo = atual;
      itens = itens.map((um) =>
        um.id === salvo.id
          ? { ...um, ...salvo, grupos: um.grupos.length > 0 ? um.grupos : salvo.grupos }
          : um,
      );
      return { item: salvo, catalogo: { tipos, itens }, aviso };
    },

    async marcar(item, estado) {
      const resposta = await marcarDisponibilidade(estabelecimentoId, item.id, estado, token);
      return {
        ...item,
        disponibilidade: estadoConhecido(resposta.disponibilidade) ?? estado,
        vendavel: resposta.vendavel === true,
      };
    },

    async marcarOpcao(item, grupoId, opcaoId, estado) {
      const resposta = await marcarDisponibilidadeDaOpcao(
        estabelecimentoId,
        item.id,
        grupoId,
        opcaoId,
        estado,
        token,
      );
      return {
        ...item,
        vendavel: resposta.vendavel === true,
        grupos: item.grupos.map((grupo) =>
          grupo.id !== grupoId
            ? grupo
            : {
                ...grupo,
                opcoes: grupo.opcoes.map((opcao) =>
                  opcao.id === opcaoId ? { ...opcao, disponibilidade: estado } : opcao,
                ),
              },
        ),
      };
    },
  };
  return fonte;
}

// ============================== demonstração ==============================

const CHAVE_DEMO = 'aki.catalogo';

function catalogoDeExemplo(): Catalogo {
  const tipos: TipoDeProduto[] = TIPOS_DE_EXEMPLO.map(([nome, icone], ordem) => ({
    id: nome,
    nome,
    ordem,
    icone,
  }));
  const itens: ItemDoCatalogo[] = PRODUTOS_DE_EXEMPLO.map((produto, ordem) => ({
    id: produto.id,
    nome: produto.nome,
    tipoId: produto.tipo,
    tipo: produto.tipo,
    descricao: produto.descricao,
    ingredientes: [],
    preco: produto.preco,
    ordem,
    controle: 'QUALITATIVO',
    situacao: 'ATIVO',
    disponibilidade: 'DISPONIVEL',
    vendavel: true,
    imagem: produto.imagem,
    selo: produto.selo,
    // Um exemplo de grupos de opções, para a tela de opções ter o que mostrar.
    grupos:
      produto.id === '1'
        ? [
            {
              id: 'g-ponto',
              nome: 'Ponto da carne',
              regra: regraDoGrupo(1, 1),
              opcoes: [
                { id: 'o-ponto', nome: 'Ao ponto', acrescimo: 0, disponibilidade: 'DISPONIVEL' },
                { id: 'o-bem', nome: 'Bem passado', acrescimo: 0, disponibilidade: 'DISPONIVEL' },
              ],
            },
            {
              id: 'g-adicionais',
              nome: 'Adicionais',
              regra: regraDoGrupo(0, 2),
              opcoes: [
                { id: 'o-bacon', nome: 'Bacon', acrescimo: 6, disponibilidade: 'DISPONIVEL' },
                { id: 'o-cheddar', nome: 'Cheddar', acrescimo: 4, disponibilidade: 'ACABANDO' },
              ],
            },
          ]
        : [],
  }));
  return { tipos, itens };
}

export function fonteDaDemonstracao(): FonteDoCatalogo {
  const guardar = (catalogo: Catalogo) => gravar(CHAVE_DEMO, catalogo);
  const atual = (): Catalogo => {
    const salvo = ler<Catalogo | null>(CHAVE_DEMO, null);
    if (salvo !== null) return salvo;
    const novo = catalogoDeExemplo();
    guardar(novo);
    return novo;
  };
  const trocar = (item: ItemDoCatalogo): ItemDoCatalogo => {
    const catalogo = atual();
    guardar({ ...catalogo, itens: catalogo.itens.map((um) => (um.id === item.id ? item : um)) });
    return item;
  };

  return {
    editaTudo: true,
    podeCriar: true,
    podeAlterar: true,

    carregar: () => Promise.resolve(atual()),

    abrir: (item) => Promise.resolve(atual().itens.find((um) => um.id === item.id) ?? item),

    salvar(dados, original) {
      const catalogo = atual();
      let tipos = catalogo.tipos;
      let tipoId = dados.tipoId;
      if (tipoId === NOVO_TIPO) {
        const existente = tipos.find(
          (tipo) => tipo.nome.toLowerCase() === dados.tipoNovo.toLowerCase(),
        );
        tipoId = existente?.id ?? dados.tipoNovo;
        if (existente === undefined) {
          tipos = [
            ...tipos,
            { id: tipoId, nome: dados.tipoNovo, ordem: tipos.length, icone: 'restaurant_menu' },
          ];
        }
      }
      const disponibilidade =
        dados.controle === 'SEM_CONTROLE' ? 'DISPONIVEL' : dados.disponibilidade;
      const item: ItemDoCatalogo = {
        id: original?.id ?? `demo-${Date.now().toString(36)}`,
        nome: dados.nome,
        tipoId,
        tipo: tipos.find((tipo) => tipo.id === tipoId)?.nome ?? tipoId,
        descricao: dados.descricao,
        ingredientes: [...dados.ingredientes],
        preco: dados.preco,
        ordem: dados.ordem,
        controle: dados.controle,
        situacao: dados.situacao,
        disponibilidade,
        vendavel: !esgotado(disponibilidade),
        imagem: dados.imagem,
        selo: dados.selo,
        grupos: original?.grupos ?? [],
      };
      const itens = original
        ? catalogo.itens.map((um) => (um.id === item.id ? item : um))
        : [...catalogo.itens, item];
      const novo = { tipos, itens };
      guardar(novo);
      return Promise.resolve({ item, catalogo: novo, aviso: null });
    },

    marcar: (item, estado) =>
      Promise.resolve(trocar({ ...item, disponibilidade: estado, vendavel: !esgotado(estado) })),

    marcarOpcao: (item, grupoId, opcaoId, estado) =>
      Promise.resolve(
        trocar({
          ...item,
          grupos: item.grupos.map((grupo) =>
            grupo.id !== grupoId
              ? grupo
              : {
                  ...grupo,
                  opcoes: grupo.opcoes.map((opcao) =>
                    opcao.id === opcaoId ? { ...opcao, disponibilidade: estado } : opcao,
                  ),
                },
          ),
        }),
      ),
  };
}
