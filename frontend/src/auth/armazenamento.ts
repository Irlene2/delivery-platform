/**
 * Onde o token fica, e por quê — a ADR-047 em código.
 *
 * `sessionStorage`, e a decisão foi entre três opções ruins:
 *
 * - **memória só**: o mais seguro contra XSS, e recarregar a página desloga. O
 *   painel de um comerciante é aberto e recarregado o dia todo.
 * - **localStorage**: sobrevive a fechar o navegador — o que aqui só significa
 *   guardar um token morto, porque ele expira em 30 minutos e **não há
 *   refresh**. Paga o risco clássico de XSS por nada.
 * - **sessionStorage**: sobrevive ao F5, morre ao fechar a aba, não vaza para
 *   outra aba.
 *
 * O que realmente defende contra XSS não é a escolha de armazenamento: é não
 * injetar HTML de origem alheia e ter CSP. O protótipo que chegou em 28/09 era
 * feito de `innerHTML` com interpolação — é exatamente o que este front não
 * faz, e é por isso que o React é a defesa e o `sessionStorage` é só onde a
 * coisa mora.
 *
 * ## Tudo aqui tolera `sessionStorage` indisponível
 *
 * Em aba privada, com dados de site bloqueados ou dentro de um iframe de outra
 * origem, o acesso **estoura** em vez de devolver vazio. Um `try` em cada
 * ponto, e o front funciona sem guardar nada: o comerciante reloga a cada
 * recarga, o que é ruim e não é quebrado.
 *
 * ## O token é opaco para o cliente
 *
 * Nada aqui decodifica o JWT. A validade vem do `expiresIn` que o login
 * devolve, não de um `exp` lido do token — porque ler claim no cliente é o
 * primeiro passo para **confiar** em claim no cliente, e a permissão deste
 * sistema não está no token (`CLAUDE.md`: *"Permissão é do vínculo usuário ×
 * estabelecimento e é resolvida por requisição"*).
 */

const CHAVE = 'delivery.sessao';

export interface Sessao {
  /** O valor bruto do token. Nunca é inspecionado, só reenviado. */
  readonly token: string;
  /** Instante em que o token deixa de valer, em milissegundos do epoch. */
  readonly expiraEm: number;
}

/** Guarda a sessão. Em navegador que recusa armazenar, não faz nada. */
export function guardar(sessao: Sessao): void {
  try {
    sessionStorage.setItem(CHAVE, JSON.stringify(sessao));
  } catch {
    // Aba privada, dados de site bloqueados, cota. A sessão continua em
    // memória nesta aba; recarregar vai exigir novo login.
  }
}

/**
 * A sessão guardada, se houver e se ainda valer.
 *
 * Token expirado é apagado aqui e devolvido como ausente: manter um token morto
 * significaria a primeira requisição da página levar 401, e a tela de sessão
 * expirada aparecer depois de um instante de painel vazio.
 */
export function recuperar(): Sessao | null {
  let cru: string | null = null;
  try {
    cru = sessionStorage.getItem(CHAVE);
  } catch {
    return null;
  }
  if (cru === null) return null;

  let candidato: unknown;
  try {
    candidato = JSON.parse(cru);
  } catch {
    esquecer();
    return null;
  }

  if (!eSessao(candidato)) {
    // Formato que este código não escreveu — versão antiga, outra aplicação na
    // mesma origem, alguém mexendo à mão. Apagar é mais seguro do que adivinhar.
    esquecer();
    return null;
  }
  if (candidato.expiraEm <= Date.now()) {
    esquecer();
    return null;
  }
  return candidato;
}

export function esquecer(): void {
  try {
    sessionStorage.removeItem(CHAVE);
  } catch {
    // Nada a fazer: se não dá para remover, também não deu para guardar.
  }
}

/**
 * Validação de forma, e ela não é cerimônia.
 *
 * O que está no `sessionStorage` pode ter sido escrito por uma versão anterior
 * desta aplicação, ou à mão pelo console. Sem esta conferência, `sessao.token`
 * poderia ser `undefined` e o cliente HTTP mandaria o cabeçalho
 * `Authorization: Bearer undefined` — que o servidor recusa com 401 e ninguém
 * entende por quê.
 */
function eSessao(valor: unknown): valor is Sessao {
  if (typeof valor !== 'object' || valor === null) return false;
  const objeto = valor as Record<string, unknown>;
  return (
    typeof objeto.token === 'string' &&
    objeto.token.length > 0 &&
    typeof objeto.expiraEm === 'number' &&
    Number.isFinite(objeto.expiraEm)
  );
}
