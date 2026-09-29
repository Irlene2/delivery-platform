/// <reference types="vite/client" />

/**
 * O tipo do `import.meta.env` usado pelo cliente HTTP.
 *
 * Sem esta declaração, `import.meta.env.VITE_API_BASE_URL` é `any` e um erro de
 * digitação no nome da variável passa pelo `typecheck` e vira `undefined` em
 * tempo de execução — com a base caindo no padrão e ninguém sabendo por quê.
 */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
