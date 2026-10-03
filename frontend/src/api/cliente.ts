/**
 * O cliente HTTP encapsulado que a ADR-016 pede pelo nome.
 *
 * Um lugar só onde se monta URL, se põe o `Authorization`, se lê `ProblemDetail`
 * e se decide o que fazer com 401 e 403. Espalhar `fetch` pelas telas é como
 * cada tela passa a tratar erro de um jeito diferente — e, neste sistema, a
 * recusa é desenhada com cuidado e precisa ser lida do mesmo jeito em todo
 * lugar.
 *
 * ## Os dois formatos de recusa, medidos e não supostos
 *
 * | situação | corpo |
 * | --- | --- |
 * | login recusado (identity) | `ProblemDetail` com `detail: "telefone ou senha inválidos"` |
 * | token ausente ou inválido | **vazio**, com `WWW-Authenticate: Bearer ...` |
 * | sem vínculo / sem permissão / produtor fora (merchant, catalog) | `ProblemDetail` com `detail: "sem acesso a este estabelecimento"` |
 * | corpo malformado | 400 padrão do Spring |
 *
 * **Por isso o cliente se orienta pelo status, e o corpo é um extra.** Um front
 * que dependesse de `problema.detail` para saber o que aconteceu quebraria no
 * 401 de token, onde não há corpo nenhum. E não se pode ler o
 * `WWW-Authenticate` do navegador: o CORS do gateway não declara
 * `exposedHeaders`, então o cabeçalho existe na resposta e é invisível para o
 * JavaScript.
 *
 * ## O 403 é deliberadamente ambíguo, e o front não tenta desambiguar
 *
 * No `catalog`, o mesmo 403 com o mesmo corpo sai para quatro situações — sem
 * vínculo, sem permissão, `merchant` com erro e `merchant` calado. É M7: pela
 * borda, ninguém distingue "esta loja não é sua" de "o sistema está com
 * problema". Inventar uma mensagem que escolha uma das quatro seria mentir com
 * mais confiança do que o servidor.
 */

import { esquecer } from '../auth/armazenamento';

/** De onde a API responde. Vem do ambiente, e não tem valor embutido no código. */
const BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/** Assinado quando qualquer requisição recebe 401. A raiz da aplicação escuta. */
type OuvinteDeSessaoPerdida = () => void;
let aoPerderSessao: OuvinteDeSessaoPerdida | null = null;

export function quandoASessaoCair(ouvinte: OuvinteDeSessaoPerdida): void {
  aoPerderSessao = ouvinte;
}

/**
 * O que o servidor respondeu quando não foi 2xx.
 *
 * `detalhe` é opcional porque **existe resposta de erro sem corpo** — o 401 do
 * Resource Server é assim. Quem renderiza precisa ter um texto próprio para
 * esse caso, e é por isso que o campo é opcional em vez de vir com um valor
 * inventado aqui.
 */
export class ErroDaApi extends Error {
  constructor(
    readonly status: number,
    readonly detalhe?: string,
  ) {
    super(detalhe ?? `a API respondeu ${status}`);
    this.name = 'ErroDaApi';
  }
}

/** Não houve resposta: rede caída, DNS, CORS recusado, servidor no chão. */
export class SemResposta extends Error {
  constructor(causa: unknown) {
    super('não foi possível falar com o servidor');
    this.name = 'SemResposta';
    this.cause = causa;
  }
}

interface Opcoes {
  readonly metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  readonly corpo?: unknown;
  /** O token do portador. Ausente quando a rota é pública (login, cadastro). */
  readonly token?: string | undefined;
}

/**
 * Uma requisição.
 *
 * O tipo do retorno é do chamador, e vem dos tipos **gerados** do contrato
 * (`src/api/generated/`). Este arquivo não conhece nenhum formato de corpo — é o
 * que o mantém pequeno e é o que faz um contrato mudado quebrar o `typecheck`
 * na tela, onde a mudança importa, em vez de aqui.
 */
export async function chamar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json' };
  if (opcoes.corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  if (opcoes.token !== undefined) cabecalhos['Authorization'] = `Bearer ${opcoes.token}`;

  let resposta: Response;
  try {
    resposta = await fetch(`${BASE}${caminho}`, {
      method: opcoes.metodo ?? 'GET',
      headers: cabecalhos,
      body: opcoes.corpo === undefined ? null : JSON.stringify(opcoes.corpo),
      // Sem cookie, nunca: a credencial deste sistema é um cabeçalho
      // (ADR-044). E o CORS do gateway declara allowCredentials: false —
      // mandar credencial faria o navegador recusar a resposta inteira.
      credentials: 'omit',
    });
  } catch (causa) {
    // Aqui cai o CORS recusado também, e é o erro mais confuso do primeiro dia:
    // o navegador não conta ao JavaScript que foi CORS. Se aparecer com o
    // servidor de pé, confira CORS_ALLOWED_ORIGINS no gateway.
    throw new SemResposta(causa);
  }

  if (resposta.status === 401) {
    // Token ausente, expirado, de outro emissor ou de outra audiência. A sessão
    // acabou, e apagá-la aqui é o que garante que a próxima navegação não tente
    // de novo com o mesmo token morto.
    esquecer();
    aoPerderSessao?.();
    throw new ErroDaApi(401, await lerDetalhe(resposta));
  }

  if (!resposta.ok) {
    throw new ErroDaApi(resposta.status, await lerDetalhe(resposta));
  }

  if (resposta.status === 204 || resposta.headers.get('Content-Length') === '0') {
    // O 202 do verification-code não tem corpo. Sem esta saída, o JSON.parse de
    // string vazia estouraria num caminho que é sucesso.
    return undefined as T;
  }

  const texto = await resposta.text();
  if (texto.length === 0) return undefined as T;
  return JSON.parse(texto) as T;
}

/**
 * O `detail` do `ProblemDetail`, quando houver.
 *
 * Nunca estoura: um corpo ausente, vazio ou que não é JSON é caso normal aqui,
 * e um erro ao ler o erro esconderia o status, que é a informação que importa.
 */
async function lerDetalhe(resposta: Response): Promise<string | undefined> {
  try {
    const texto = await resposta.text();
    if (texto.length === 0) return undefined;
    const corpo: unknown = JSON.parse(texto);
    if (typeof corpo === 'object' && corpo !== null) {
      const detalhe = (corpo as Record<string, unknown>).detail;
      if (typeof detalhe === 'string' && detalhe.length > 0) return detalhe;
    }
    return undefined;
  } catch {
    return undefined;
  }
}
