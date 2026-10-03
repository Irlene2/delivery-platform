import type { components } from '../api/generated/merchant';

/**
 * Os tipos do painel saem do **contrato gerado**, e não de uma cópia escrita à
 * mão.
 *
 * <p>É a metade do front que espelha o `ContratoOpenApiIT` do outro lado do fio
 * (ADR-039): lá um teste recusa o contrato que divergir do código; aqui o
 * `tipos:conferir` do CI recusa o tipo que divergir do contrato. Uma cópia
 * manual quebraria os dois de uma vez, em silêncio.
 *
 * ## O catálogo mora em outro arquivo
 *
 * Os tipos do `ProdutoResumoResponse` e da página dele são lidos só em
 * `produtos.ts`, que é a ponte com o contrato do catálogo.
 *
 * Os quatro campos da `LojaDoUsuario` chegam **opcionais** no tipo gerado,
 * porque o springdoc não deduz `required` de um `record` — está anotado na
 * página do marco 2. Por isso o `Exigido<T>` abaixo: ele é uma afirmação
 * localizada, num lugar só, em vez de `?.` espalhado por quatro componentes.
 */

/** Remove a opcionalidade que o gerador acrescentou por falta de `required`. */
type Exigido<T> = { [K in keyof T]-?: NonNullable<T[K]> };

export type LojaDoUsuario = Exigido<components['schemas']['LojaDoUsuario']>;

export type Permissao = LojaDoUsuario['permissoes'][number];

export type Papel = LojaDoUsuario['papel'];
