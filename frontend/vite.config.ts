// vitest/config, e não vite: a chave `test` não existe no defineConfig do vite,
// e com o import errado o typecheck reprova numa mensagem que fala de
// propriedade desconhecida em vez de falar do import.
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

/**
 * Porta 5173 fixa e `strictPort`, e não é preferência.
 *
 * O CORS deste sistema mora no gateway, numa lista de origens
 * (`CORS_ALLOWED_ORIGINS`), e o padrão do gateway é exatamente
 * `http://localhost:5173`. Se o Vite mudar de porta sozinho porque a 5173 está
 * ocupada, o navegador passa a bloquear toda chamada e o erro que aparece é
 * "Failed to fetch" — que não diz nada sobre porta nem sobre CORS. Melhor
 * falhar ao subir.
 *
 * NÃO há proxy de desenvolvimento, de propósito. Um proxy no Vite faria as
 * chamadas saírem da mesma origem e o CORS deixaria de ser exercitado — e aí a
 * primeira vez que alguém descobriria a configuração errada seria em produção.
 * O `CadeiaDeFiltrosIT` do gateway prova o preflight; este front o exercita de
 * verdade.
 */
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/teste/preparo.ts'],
    globals: true,
    // A exclusão não é decoração: o tsconfig tem `composite: true`, e enquanto
    // ele emitia .js para .tsbuild/ o Vitest coletava a saída compilada junto
    // com a fonte — cada teste rodava DUAS vezes e o total aparecia dobrado.
    // O `emitDeclarationOnly` resolveu a causa; isto é a rede, para o dia em
    // que alguém puser emit de volta sem notar o dobro.
    exclude: ['node_modules/**', 'dist/**', '.tsbuild/**', '.tsbuild-node/**'],
  },
});
