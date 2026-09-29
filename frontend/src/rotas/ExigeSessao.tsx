import { Navigate, useLocation } from 'react-router';
import type { ReactNode } from 'react';
import { useSessao } from '../auth/useSessao';

/**
 * O guarda de rota — e ele é conveniência, não segurança.
 *
 * O que protege de verdade é o servidor: toda rota deste sistema exige token, e
 * a permissão é resolvida no `merchant` a cada requisição (ADR-011). Este
 * componente existe para a pessoa não ver um painel vazio piscando antes do
 * 401; remover este arquivo não abriria dado nenhum.
 *
 * <p>Vale dizer porque o protótipo de 28/09 fazia o contrário: ele escolhia o
 * perfil num `<select>` e redirecionava para a página correspondente. Ali o
 * cliente decidia o acesso. Aqui o cliente só decide o que desenhar enquanto
 * espera.
 *
 * <p>`replace` e o `state`: sem `replace`, o botão voltar leva de volta à rota
 * protegida e o guarda redireciona outra vez, prendendo a navegação num laço.
 * O `state` guarda onde a pessoa queria ir, para o login devolvê-la lá.
 */
export function ExigeSessao({ children }: { children: ReactNode }) {
  const { autenticado } = useSessao();
  const local = useLocation();

  if (!autenticado) {
    return <Navigate to="/entrar" replace state={{ de: local.pathname }} />;
  }
  return <>{children}</>;
}
