import { useSessao } from '../auth/useSessao';
import { Botao } from '../componentes/Botao';

/**
 * A primeira tela autenticada — e ela está vazia porque o sistema não sabe
 * responder a pergunta seguinte.
 *
 * <h2>Por que não há nada aqui, e por que isso é o entregável</h2>
 *
 * Depois do login existe um token e mais nada. **Não há rota que diga quais
 * estabelecimentos o portador tem** — medido em 29/09: o gateway roteia
 * `/api/v1/me/**` para o `identity`, que não tem controlador nenhum sob `/me`,
 * e o `MembroRepositorio` do `merchant` só tem buscas que já exigem a loja.
 * Todas as rotas de negócio começam com `{estabelecimentoId}`.
 *
 * <p>Então esta tela não pode listar lojas, não pode montar menu — o menu
 * depende de saber o que a pessoa pode fazer em cada loja, e essa resposta vive
 * numa rota `/internal/` que o gateway não expõe — e não pode nem dizer o nome
 * de quem entrou, porque não existe rota que devolva o usuário do token e este
 * front **não decodifica o JWT**.
 *
 * <p>Ela mostra o que é verdade: a sessão existe, e quanto falta para expirar.
 * <b>Uma tela honestamente vazia é melhor do que uma tela com dado inventado</b>
 * — o protótipo de 28/09 tinha um painel com gráfico de vendas por horário
 * alimentado por quatro pedidos falsos que ele mesmo gravava no navegador, e
 * ninguém desconfiaria dele.
 *
 * <h2>Os trinta minutos aparecem, e é decisão de produto</h2>
 *
 * Não há refresh token (ADR-037: *"o access token é a sessão"*). Esconder a
 * expiração não a evita — só faz a pessoa descobrir no meio de um formulário.
 * Mostrar é o mínimo enquanto a decisão de produto não muda.
 */
export function PainelPage() {
  const { sessao, sair } = useSessao();
  if (sessao === null) return null;

  const minutos = Math.max(0, Math.round((sessao.expiraEm - Date.now()) / 60_000));

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-2xl flex-col gap-6 px-4 py-10">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
            Painel
          </h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Sessão ativa · expira em aproximadamente {minutos} min
          </p>
        </div>
        <Botao variante="discreto" onClick={sair}>
          Sair
        </Botao>
      </header>

      <section
        aria-labelledby="sem-loja"
        className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-5 py-8 text-center dark:border-slate-700 dark:bg-slate-900"
      >
        <h2 id="sem-loja" className="text-base font-semibold text-slate-900 dark:text-slate-100">
          Nenhuma loja para mostrar ainda
        </h2>
        <p className="mx-auto mt-2 max-w-md text-sm text-slate-600 dark:text-slate-400">
          Você entrou. O sistema ainda não tem como dizer de quais lojas você faz parte — essa
          consulta é a próxima a ser construída no servidor.
        </p>
      </section>
    </main>
  );
}
