import type { EstadoDeDisponibilidade } from '../api/disponibilidade';
import type { Situacao } from './catalogo';

/**
 * As cores dos selos de situação e disponibilidade, no padrão `.status` do protótipo.
 * Arquivo próprio porque componente não exporta constante (react-refresh).
 */
export const COR_DA_DISPONIBILIDADE: Record<EstadoDeDisponibilidade, string> = {
  DISPONIVEL: 'green',
  ACABANDO: 'orange',
  ESGOTADO_HOJE: 'vermelho',
  ESGOTADO_INDETERMINADO: 'vermelho',
};
export const ROTULO_DA_SITUACAO: Record<Situacao, [string, string]> = {
  ATIVO: ['Publicado', 'green'],
  RASCUNHO: ['Rascunho', 'cinza'],
  INATIVO: ['Fora do cardápio', 'cinza'],
};
