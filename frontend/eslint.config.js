import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

/**
 * Flat config: o ESLint 9 não lê `.eslintrc` por padrão. Quem procurar o arquivo
 * antigo e não achar vai pensar que falta configuração.
 *
 * A regra que importa aqui é a última: `no-restricted-globals` sobre
 * `localStorage`. A ADR-047 escolheu `sessionStorage`, e uma decisão de
 * segurança que depende de todo mundo lembrar não é decisão, é esperança — o
 * mesmo argumento com que a ADR-001 virou task de build em vez de comentário.
 */
export default tseslint.config(
  { ignores: ['dist', 'src/api/generated', '.tsbuild', '.tsbuild-node'] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommendedTypeChecked],
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
      parserOptions: {
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],

      // A ADR-047 decidiu sessionStorage. localStorage sobrevive a fechar o
      // navegador, o que aqui só guarda um token morto — e é o alvo clássico de
      // XSS. O único lugar que fala com armazenamento é src/auth/armazenamento.ts.
      'no-restricted-globals': [
        'error',
        {
          name: 'localStorage',
          message:
            'ADR-047: a sessão mora em sessionStorage, e só src/auth/armazenamento.ts a toca.',
        },
      ],

      // O token é opaco para o cliente. Decodificar JWT é o primeiro passo para
      // confiar em claim no cliente, e a permissão deste sistema não está no
      // token (CLAUDE.md).
      'no-restricted-syntax': [
        'error',
        {
          selector: "CallExpression[callee.name='atob']",
          message:
            'Não decodifique o token. A validade vem do expiresIn do login; a permissão vem do merchant.',
        },
      ],
    },
  },
);
