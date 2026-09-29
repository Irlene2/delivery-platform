/**
 * As três rotas de `/api/v1/auth/**`, cada uma com o formato que o contrato
 * congelado declara — e nada além do que ele declara.
 *
 * Os tipos vêm de `generated/identity.ts`, gerado de
 * `contracts/openapi/identity-service.json` (ADR-039). Se o back mudar um nome
 * de campo e regravar o contrato, o `npm run tipos` regrava daqui, e o
 * `typecheck` reprova **neste arquivo** — que é o lugar certo para descobrir.
 *
 * ## Dois passos, e respostas diferentes em cada um
 *
 * | rota | corpo que vai | o que volta |
 * | --- | --- | --- |
 * | `POST /api/v1/auth/verification-code` | `{ telefone }` | **202, sem corpo** |
 * | `POST /api/v1/auth/signup` | `{ telefone, codigo, nome, senha }` | **201 `{ id }`** |
 * | `POST /api/v1/auth/login` | `{ telefone, senha }` | **200 `{ accessToken, tokenType, expiresIn }`** |
 *
 * **O 202 é do código, não do cadastro**, e ele vem vazio de propósito —
 * inclusive para telefone que já tem conta, caso em que nenhum código é criado.
 * A tela não pode dizer "enviamos um código" com certeza; ela diz o que é
 * verdade, que é "se este telefone puder receber um código, ele foi pedido".
 *
 * ## O que este módulo NÃO tem
 *
 * Renovação. **Não existe refresh token neste sistema** — a ADR-037 diz com
 * essas palavras que *"o access token é a sessão"* e que o refresh está
 * *"adiado com registro"*. Trinta minutos, e depois login outra vez. Qualquer
 * função aqui chamada `renovar` seria uma promessa sem rota atrás.
 *
 * E não tem "quem sou eu". Não há rota que devolva o usuário do token: o
 * `/api/v1/me/**` do gateway aponta para o `identity`, que não tem controlador
 * nenhum sob `/me`. O login devolve três campos, e nenhum é o nome de quem
 * entrou.
 */

import type { components } from './generated/identity';
import { chamar } from './cliente';

export type PedidoDeCodigo = components['schemas']['VerificationCodeRequest'];
export type PedidoDeCadastro = components['schemas']['SignupRequest'];
export type RespostaDeCadastro = components['schemas']['SignupResponse'];
export type PedidoDeLogin = components['schemas']['LoginRequest'];
export type RespostaDeLogin = components['schemas']['LoginResponse'];

/** 202 e nada mais — nem para telefone novo, nem para telefone que já existe. */
export async function pedirCodigo(telefone: string): Promise<void> {
  await chamar<void>('/api/v1/auth/verification-code', {
    metodo: 'POST',
    corpo: { telefone } satisfies PedidoDeCodigo,
  });
}

/** 201 com o identificador do usuário criado. Não devolve token: o login é o passo seguinte. */
export function cadastrar(dados: PedidoDeCadastro): Promise<RespostaDeCadastro> {
  return chamar<RespostaDeCadastro>('/api/v1/auth/signup', {
    metodo: 'POST',
    corpo: dados,
  });
}

export function entrar(telefone: string, senha: string): Promise<RespostaDeLogin> {
  return chamar<RespostaDeLogin>('/api/v1/auth/login', {
    metodo: 'POST',
    corpo: { telefone, senha } satisfies PedidoDeLogin,
  });
}
