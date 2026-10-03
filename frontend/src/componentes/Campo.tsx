import type { InputHTMLAttributes, ReactNode } from 'react';
import { useId } from 'react';

/**
 * O campo de formulário do sistema — um só, e todos os formulários o usam.
 *
 * O ofício vem do protótipo de 28/09, que fazia isso bem: rótulo acima, erro
 * por campo abaixo, e o erro anunciado. O que muda é que lá eram 803 linhas de
 * CSS escritas à mão e aqui é um componente.
 *
 * <h2>O `aria-describedby` e o `role="alert"` não são enfeite</h2>
 *
 * Sem o `describedby`, um leitor de tela anuncia "Telefone, campo de edição" e
 * nunca chega ao texto do erro, que visualmente está a três pixels de distância.
 * O `useId` existe para que dois campos na mesma página não colidam — o que
 * aconteceria com um id fixo, e é a razão de ele não ser prop.
 */
interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id' | 'aria-describedby'> {
  readonly rotulo: string;
  readonly erro?: string | undefined;
  readonly dica?: ReactNode;
}

export function Campo({ rotulo, erro, dica, ...resto }: Props) {
  const id = useId();
  const idDoErro = `${id}-erro`;
  const idDaDica = `${id}-dica`;

  const descricao = [erro !== undefined ? idDoErro : null, dica !== undefined ? idDaDica : null]
    .filter((valor): valor is string => valor !== null)
    .join(' ');

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium text-slate-700 dark:text-slate-200">
        {rotulo}
      </label>
      <input
        {...resto}
        id={id}
        aria-invalid={erro !== undefined}
        aria-describedby={descricao.length > 0 ? descricao : undefined}
        className={[
          'rounded-md border px-3 py-2 text-base outline-none transition',
          'bg-white text-slate-900 placeholder:text-slate-400',
          'dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500',
          'focus-visible:ring-2 focus-visible:ring-offset-1',
          erro !== undefined
            ? 'border-red-500 focus-visible:ring-red-500'
            : 'border-slate-300 focus-visible:ring-slate-500 dark:border-slate-700',
        ].join(' ')}
      />
      {dica !== undefined && (
        <p id={idDaDica} className="text-xs text-slate-500 dark:text-slate-400">
          {dica}
        </p>
      )}
      {erro !== undefined && (
        <p
          id={idDoErro}
          role="alert"
          className="text-xs font-medium text-red-600 dark:text-red-400"
        >
          {erro}
        </p>
      )}
    </div>
  );
}
