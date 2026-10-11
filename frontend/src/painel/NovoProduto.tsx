import { useState, type FormEvent } from 'react';

import { criarProduto, fraseDaRecusa, type NovoProduto as CorpoDoProduto } from '../api/cardapio';
import { useRecurso } from '../api/useRecurso';
import { useSessao } from '../auth/useSessao';
import { Aviso } from '../componentes/Aviso';
import { Botao } from '../componentes/Botao';
import { Campo } from '../componentes/Campo';
import {
  caminhoDasSecoes,
  proximaOrdem,
  secaoParaTela,
  TAMANHO_DO_CARDAPIO,
  type CategoriaDoContrato,
} from './cardapio';
import { caminhoDosProdutos, paraTela, type PaginaDoContrato } from './produtos';

type ModoDeControle = CorpoDoProduto['modoDeControle'];

/** Os modos vêm do contrato; o rótulo é daqui. O `Record` exige do typecheck todos eles. */
const MODOS: Record<ModoDeControle, string> = {
  QUALITATIVO: 'Pode acabar no dia — uma pizza, um sabor',
  SEM_CONTROLE: 'Não acaba — um refrigerante em lata',
};

/**
 * O produto novo — `/painel/lojas/:estabelecimentoId/produtos/novo` (ADR-067 §3): cinco
 * campos têm URL, porque um formulário assim alguém quer retomar ou recarregar.
 *
 * <h2>Duas rotas para montar um formulário</h2>
 *
 * A **seção** é um seletor alimentado pela lista de seções — a primeira vez que uma tela
 * monta formulário com duas rotas do mesmo serviço (ADR-066). E a lista de produtos entra
 * para uma conta só: a `ordem`, **o maior da seção escolhida, mais um**, derivada e nunca
 * mostrada. *É dívida nossa* (ADR-067 §4): a borda exige um campo que nenhuma pessoa
 * fornece.
 *
 * <h2>A loja sem seção não é um beco</h2>
 *
 * Sem seção não há produto (ADR-066), e a tela diz isso e leva de volta ao cardápio, onde
 * a seção nasce — em vez de mostrar um seletor vazio.
 *
 * <h2>A recusa</h2>
 *
 * O `detail` do servidor vai à tela como veio, e **sai quando qualquer campo muda**, ou
 * no envio seguinte (ADR-067 §5) — o achado da passagem humana de 10/10.
 */
export function NovoProduto({
  estabelecimentoId,
  aoCriar,
  aoVoltar,
}: {
  estabelecimentoId: string;
  aoCriar: () => void;
  aoVoltar: () => void;
}) {
  const secoes = useRecurso<CategoriaDoContrato[]>(caminhoDasSecoes(estabelecimentoId));
  const produtos = useRecurso<PaginaDoContrato>(
    caminhoDosProdutos(estabelecimentoId, TAMANHO_DO_CARDAPIO),
  );

  if (secoes.estado === 'carregando' || produtos.estado === 'carregando') {
    return <p className="text-sm text-slate-500 dark:text-slate-400">Carregando…</p>;
  }
  if (secoes.estado === 'erro' || produtos.estado === 'erro') {
    return (
      <p role="alert" className="text-sm text-amber-700 dark:text-amber-400">
        Não deu para carregar as seções do cardápio agora.
      </p>
    );
  }

  const lista = (secoes.dados ?? []).map(secaoParaTela);
  if (lista.length === 0) {
    return (
      <section className="flex flex-col gap-4">
        <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-50">Novo produto</h2>
        <Aviso tom="neutro">
          Todo produto mora numa seção do cardápio, e esta loja não tem nenhuma. Crie a primeira no
          cardápio — Bebidas, Pizzas — e volte aqui.
        </Aviso>
        <div>
          <Botao type="button" variante="discreto" onClick={aoVoltar}>
            Voltar ao cardápio
          </Botao>
        </div>
      </section>
    );
  }

  return (
    <Formulario
      estabelecimentoId={estabelecimentoId}
      secoes={lista}
      produtos={paraTela(produtos.dados).produtos}
      aoCriar={aoCriar}
      aoVoltar={aoVoltar}
    />
  );
}

function Formulario({
  estabelecimentoId,
  secoes,
  produtos,
  aoCriar,
  aoVoltar,
}: {
  estabelecimentoId: string;
  secoes: { id: string; nome: string }[];
  produtos: { categoriaId: string | null; ordem: number }[];
  aoCriar: () => void;
  aoVoltar: () => void;
}) {
  const token = useSessao().sessao?.token;
  const [categoriaId, definirCategoria] = useState(secoes[0]?.id ?? '');
  const [nome, definirNome] = useState('');
  const [preco, definirPreco] = useState('');
  const [descricao, definirDescricao] = useState('');
  const [modo, definirModo] = useState<ModoDeControle>('QUALITATIVO');
  const [enviando, definirEnviando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (token === undefined) return;

    const corpo: CorpoDoProduto = {
      categoriaId,
      nome,
      // Texto do formulário para o número do contrato. Não é conta: é leitura. Vazio
      // não é zero — vira NaN, que o JSON escreve como null, e o @NotNull recusa.
      precoBase: preco.trim() === '' ? Number.NaN : Number(preco.replace(',', '.')),
      modoDeControle: modo,
      ordem: proximaOrdem(produtos.filter((produto) => produto.categoriaId === categoriaId)),
      ...(descricao.trim() === '' ? {} : { descricao }),
    };

    definirEnviando(true);
    definirRecusa(null);
    try {
      await criarProduto(estabelecimentoId, corpo, token);
      aoCriar();
    } catch (falha) {
      const frase = fraseDaRecusa(
        falha,
        {
          400: 'Confira os campos: seção, nome, preço e se o produto pode acabar.',
          403: 'Você não tem permissão para criar produto nesta loja.',
        },
        'Não foi possível criar o produto agora.',
      );
      if (frase !== null) definirRecusa(frase);
      definirEnviando(false);
    }
  }

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-50">Novo produto</h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          O produto nasce em rascunho. Ele vai ao cardápio quando você o publicar.
        </p>
      </header>

      <form
        onSubmit={(evento) => void enviar(evento)}
        onChange={() => definirRecusa(null)}
        className="flex flex-col gap-4"
        noValidate
      >
        <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">
          Seção
          <select
            value={categoriaId}
            onChange={(e) => definirCategoria(e.target.value)}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-base text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          >
            {secoes.map((secao) => (
              <option key={secao.id} value={secao.id}>
                {secao.nome}
              </option>
            ))}
          </select>
        </label>
        <Campo
          rotulo="Nome"
          value={nome}
          onChange={(e) => definirNome(e.target.value)}
          aria-required
        />
        <Campo
          rotulo="Preço"
          inputMode="decimal"
          value={preco}
          onChange={(e) => definirPreco(e.target.value)}
          dica="Em reais, como 6,00."
          aria-required
        />
        <Campo
          rotulo="Descrição (opcional)"
          value={descricao}
          onChange={(e) => definirDescricao(e.target.value)}
        />
        <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">
          Ele pode acabar?
          <select
            value={modo}
            onChange={(e) => definirModo(e.target.value as ModoDeControle)}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-base text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          >
            {(Object.keys(MODOS) as ModoDeControle[]).map((um) => (
              <option key={um} value={um}>
                {MODOS[um]}
              </option>
            ))}
          </select>
        </label>

        {recusa !== null && <Aviso tom="erro">{recusa}</Aviso>}

        <div className="flex gap-3">
          <Botao type="submit" ocupado={enviando}>
            Criar produto
          </Botao>
          <Botao type="button" variante="discreto" onClick={aoVoltar}>
            Voltar ao cardápio
          </Botao>
        </div>
      </form>
    </section>
  );
}
