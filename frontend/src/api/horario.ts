import { chamar } from './cliente';
import type { components } from './generated/merchant';

/**
 * O horário semanal da loja (ADR-073): ler e substituir, as duas no mesmo caminho e sob a
 * mesma permissão, `CONFIGURAR_LOJA`. Os tipos são os **gerados** do contrato do
 * `merchant`; a recusa chega como `ErroDaApi`, e quem a transforma em frase é o
 * `fraseDaRecusa`.
 *
 * As duas rotas respondem com a semana **como ficou gravada**, normalizada pelo domínio, e
 * a loja que nunca abre por horário vem como **mapa vazio com 200** — não 404.
 */
export type Horario = components['schemas']['HorarioResponse'];
export type NovoHorario = components['schemas']['HorarioRequest'];

export function caminhoDoHorario(estabelecimentoId: string): string {
  return `/api/v1/merchants/${estabelecimentoId}/settings/horario`;
}

export function definirHorario(
  estabelecimentoId: string,
  corpo: NovoHorario,
  token: string,
): Promise<Horario> {
  return chamar<Horario>(caminhoDoHorario(estabelecimentoId), { metodo: 'PUT', corpo, token });
}
