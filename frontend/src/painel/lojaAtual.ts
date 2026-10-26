import { useMemo } from 'react';
import { useParams } from 'react-router';
import { useSessao } from '../auth/useSessao';
import { emDemonstracao } from '../aki/demo';
import { fonteDaDemonstracao, fonteDoServidor, type FonteDoCatalogo } from './catalogo';
import { useContextoDoPainel } from './contextoDoPainel';
import type { LojaDoUsuario } from './tipos';

/** A loja da URL (ADR-061), as lojas da pessoa e de onde vem o catálogo dela. */
export function useLojaAtual(): {
  estabelecimentoId: string;
  loja: LojaDoUsuario | undefined;
  lojas: LojaDoUsuario[];
  fonte: FonteDoCatalogo;
} {
  const { estabelecimentoId = '' } = useParams();
  const { lojas } = useContextoDoPainel();
  const token = useSessao().sessao?.token ?? '';
  const loja = lojas.find((uma) => uma.estabelecimentoId === estabelecimentoId);
  const permissoes = loja?.permissoes;
  const fonte = useMemo(
    () =>
      emDemonstracao()
        ? fonteDaDemonstracao()
        : fonteDoServidor(estabelecimentoId, token, permissoes ?? []),
    [estabelecimentoId, token, permissoes],
  );
  return { estabelecimentoId, loja, lojas, fonte };
}
