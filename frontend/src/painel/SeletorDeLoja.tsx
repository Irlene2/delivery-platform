import type { LojaDoUsuario } from './tipos';

/**
 * Qual loja o painel está mostrando.
 *
 * ## Três telas diferentes, e a do meio é a que importa
 *
 * - **nenhuma loja** — não é lista vazia: é uma tela. É o estado de todo mundo
 *   no instante seguinte ao cadastro, e a primeira coisa que essa pessoa vê no
 *   produto. Dizer "nenhum resultado" aqui seria descrever um erro onde há um
 *   começo;
 * - **uma loja** — não há o que selecionar. Um `<select>` de um item é ruído, e
 *   é o caso da maioria absoluta dos comerciantes deste produto;
 * - **duas ou mais** — aí sim, um seletor.
 *
 * ## Por que `<select>` e não uma lista de botões
 *
 * Porque o teclado e o leitor de tela já sabem o que fazer com ele, e porque no
 * telefone o sistema operacional desenha a roda nativa. Uma lista de botões
 * exigiria reimplementar as setas, o foco e o `aria-expanded` — e o resultado
 * seria pior do que o que o navegador entrega de graça.
 */
export function SeletorDeLoja({
  lojas,
  selecionada,
  aoSelecionar,
}: {
  lojas: LojaDoUsuario[];
  selecionada: string | null;
  aoSelecionar: (estabelecimentoId: string) => void;
}) {
  if (lojas.length === 0) {
    return (
      <section className="rounded-lg border border-dashed border-slate-300 p-6 text-slate-600 dark:border-slate-700 dark:text-slate-300">
        <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">
          Você ainda não faz parte de nenhuma loja
        </h2>
        <p className="mt-2 text-sm">
          Quem administra a loja precisa te convidar. O convite chega com um código, e é com ele que
          você entra na equipe.
        </p>
      </section>
    );
  }

  const unica = lojas.length === 1 ? lojas[0] : undefined;
  if (unica !== undefined) {
    return (
      <div>
        <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-100">{unica.nome}</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">{rotuloDoPapel(unica.papel)}</p>
      </div>
    );
  }

  return (
    <div>
      <label
        htmlFor="seletor-de-loja"
        className="block text-sm font-medium text-slate-700 dark:text-slate-300"
      >
        Loja
      </label>
      <select
        id="seletor-de-loja"
        value={selecionada ?? ''}
        onChange={(evento) => aoSelecionar(evento.target.value)}
        className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100"
      >
        {lojas.map((loja) => (
          <option key={loja.estabelecimentoId} value={loja.estabelecimentoId}>
            {loja.nome}
          </option>
        ))}
      </select>
    </div>
  );
}

function rotuloDoPapel(papel: LojaDoUsuario['papel']): string {
  return papel === 'ADMINISTRADOR' ? 'Você administra esta loja' : 'Você faz parte desta equipe';
}
