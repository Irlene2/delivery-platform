import { createContext } from 'react';
import type { Sessao } from './armazenamento';

/**
 * O contexto mora em arquivo próprio, e não junto do provider.
 *
 * O `react-refresh` só recarrega um módulo em desenvolvimento se ele exportar
 * **apenas** componentes. Um arquivo que exporta o provider e mais um objeto de
 * contexto perde o refresh e passa a exigir recarga da página inteira a cada
 * alteração — o que, num formulário meio preenchido, é lento o bastante para
 * mudar como se trabalha.
 */
export interface EstadoDaSessao {
  readonly sessao: Sessao | null;
  readonly autenticado: boolean;
  readonly entrar: (telefone: string, senha: string) => Promise<void>;
  readonly sair: () => void;
}

export const ContextoDaSessao = createContext<EstadoDaSessao | null>(null);
