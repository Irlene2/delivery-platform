import type { ButtonHTMLAttributes } from 'react';

/**
 * O botão, com estado de ocupado.
 *
 * `disabled` enquanto ocupado, e o `aria-busy` junto: sem o `disabled`, dois
 * cliques mandam duas requisições de cadastro e a segunda falha com "código
 * inválido" — porque a primeira consumiu o código. É um erro que parece do
 * usuário e é do front.
 */
interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  readonly ocupado?: boolean;
  readonly variante?: 'primario' | 'discreto';
}

export function Botao({ ocupado = false, variante = 'primario', children, ...resto }: Props) {
  return (
    <button
      {...resto}
      disabled={resto.disabled === true || ocupado}
      aria-busy={ocupado}
      className={[
        'inline-flex items-center justify-center gap-2 rounded-md px-4 py-2',
        'text-sm font-semibold transition',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2',
        'disabled:cursor-not-allowed disabled:opacity-60',
        variante === 'primario'
          ? 'bg-slate-900 text-white hover:bg-slate-800 focus-visible:ring-slate-900 dark:bg-slate-100 dark:text-slate-900 dark:hover:bg-white'
          : 'bg-transparent text-slate-700 underline underline-offset-4 hover:text-slate-900 focus-visible:ring-slate-500 dark:text-slate-300 dark:hover:text-white',
      ].join(' ')}
    >
      {children}
    </button>
  );
}
