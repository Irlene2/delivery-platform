import type { ReactNode } from 'react';

/**
 * A caixa de mensagem do formulário — um lugar só para o erro que não é de campo.
 *
 * `role="alert"` para o leitor de tela anunciar sem que a pessoa precise
 * encontrar a caixa. `tom` decide a cor e nada mais: a cor nunca é a única
 * portadora do significado, porque o texto já diz.
 */
export function Aviso({ tom, children }: { tom: 'erro' | 'neutro'; children: ReactNode }) {
  return (
    <div
      role="alert"
      className={[
        'rounded-md border px-3 py-2 text-sm',
        tom === 'erro'
          ? 'border-red-300 bg-red-50 text-red-800 dark:border-red-900 dark:bg-red-950 dark:text-red-200'
          : 'border-slate-300 bg-slate-50 text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200',
      ].join(' ')}
    >
      {children}
    </div>
  );
}
