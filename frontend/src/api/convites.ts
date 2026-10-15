import { chamar } from './cliente';
import type { components } from './generated/merchant';

/**
 * O convite (ADR-069, ADR-070): emitir, ver e aceitar. Os tipos são os **gerados** do
 * contrato do `merchant`.
 *
 * **O token só passa por aqui de passagem.** Ele chega no 201 de `convidar`, e quem o
 * guarda é o estado da tela que o mostra — nunca `sessionStorage`, nunca estado global,
 * nunca log. Depois de a pessoa sair da tela, ele acabou (ADR-069 §1).
 */
export type NovoConvite = components['schemas']['ConviteRequest'];
export type ConviteEmitido = components['schemas']['ConviteEmitidoResponse'];
export type ConvitePublico = components['schemas']['ConvitePublicoResponse'];
export type Aceite = components['schemas']['AceiteResponse'];
export type PermissaoDoConvite = NovoConvite['permissoes'][number];

export function convidar(
  estabelecimentoId: string,
  corpo: NovoConvite,
  token: string,
): Promise<ConviteEmitido> {
  return chamar<ConviteEmitido>(`/api/v1/merchants/${estabelecimentoId}/team/convites`, {
    metodo: 'POST',
    corpo,
    token,
  });
}

/** Público: **sem** token de sessão, de propósito — quem abre o link pode não ter conta. */
export function verConvite(tokenDoConvite: string): Promise<ConvitePublico> {
  return chamar<ConvitePublico>(`/api/v1/convites/${encodeURIComponent(tokenDoConvite)}`);
}

export function aceitarConvite(tokenDoConvite: string, sessao: string): Promise<Aceite> {
  return chamar<Aceite>(`/api/v1/convites/${encodeURIComponent(tokenDoConvite)}/aceite`, {
    metodo: 'POST',
    token: sessao,
  });
}

/**
 * O endereço da página do convite — **montado aqui**, com a origem desta aplicação: o
 * `merchant` devolve o token e não conhece o endereço das telas (ADR-070 §2).
 */
export function caminhoDoConvite(tokenDoConvite: string): string {
  return `/convite/${encodeURIComponent(tokenDoConvite)}`;
}

export function linkDoConvite(origem: string, tokenDoConvite: string): string {
  return `${origem}${caminhoDoConvite(tokenDoConvite)}`;
}
