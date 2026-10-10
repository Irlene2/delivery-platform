import { Navigate, Outlet, useLocation, useNavigate, useParams } from 'react-router';

import { ProdutoAberto } from './ProdutoAberto';
import { ListaDeProdutos } from './ListaDeProdutos';
import { caminhoDaLoja, caminhoDaSecao, secaoDoCaminho } from './caminhos';
import { useContextoDoPainel } from './contextoDoPainel';
import { MenuDoPainel } from './MenuDoPainel';
import { secoesDe, type Secao } from './secoes';
import { SeletorDeLoja } from './SeletorDeLoja';

/**
 * Uma loja do painel — `/painel/lojas/:estabelecimentoId` (ADR-061).
 *
 * <h2>O id vem da URL, e o front não confere vínculo</h2>
 *
 * O menu é desenhado das permissões que o `GET /api/v1/me/estabelecimentos` devolveu
 * para esta loja. Uma loja que não está na lista não tem permissão nenhuma no menu — e
 * a tela embaixo pede os dados assim mesmo: quem decide é o servidor, que responde
 * 403 idêntico a "não existe" (M7). Conferir vínculo aqui seria derivar a invariante 9
 * no navegador.
 */
export function LojaDoPainel() {
  const { estabelecimentoId = '' } = useParams();
  const contexto = useContextoDoPainel();
  const { lojas } = contexto;
  const navegar = useNavigate();
  const local = useLocation();

  const loja = lojas.find((uma) => uma.estabelecimentoId === estabelecimentoId);
  const permissoes = loja?.permissoes ?? [];

  return (
    <>
      <SeletorDeLoja
        lojas={lojas}
        selecionada={estabelecimentoId}
        aoSelecionar={(outra) => void navegar(caminhoDaLoja(outra))}
      />
      <MenuDoPainel
        permissoes={permissoes}
        atual={secaoDoCaminho(local.pathname)}
        aoEscolher={(secao) => void navegar(caminhoDaSecao(estabelecimentoId, secao))}
      />
      {/* O contexto de um Outlet vale só para o nível de baixo: repassado aqui, ele chega
          às seções — sem isto a SecaoInicial recebia undefined (medido na W-E). */}
      <Outlet context={contexto} />
    </>
  );
}

/**
 * `/painel/lojas/:id` sem seção: a primeira que as permissões abrem, como era quando a
 * seção morava em estado. Sem nenhuma, fica só o menu dizendo isso.
 */
export function SecaoInicial() {
  const { estabelecimentoId = '' } = useParams();
  const { lojas } = useContextoDoPainel();
  const loja = lojas.find((uma) => uma.estabelecimentoId === estabelecimentoId);
  const primeira = secoesDe(loja?.permissoes ?? [])[0]?.secao;

  return primeira === undefined ? null : (
    <Navigate to={caminhoDaSecao(estabelecimentoId, primeira)} replace />
  );
}

/** As duas seções que existem são a mesma lista; "Disponibilidade" a mostra com a marcação. */
const MARCA: Record<Secao, boolean> = { cardapio: false, disponibilidade: true };

export function ListaDaSecao({ secao }: { secao: Secao }) {
  const { estabelecimentoId = '' } = useParams();
  const navegar = useNavigate();
  return (
    <ListaDeProdutos
      estabelecimentoId={estabelecimentoId}
      podeMarcar={MARCA[secao]}
      aoAbrir={(produtoId) =>
        void navegar(`${caminhoDaSecao(estabelecimentoId, secao)}/${produtoId}`)
      }
    />
  );
}

export function ProdutoDaSecao({ secao }: { secao: Secao }) {
  const { estabelecimentoId = '', produtoId = '' } = useParams();
  const navegar = useNavigate();
  return (
    <ProdutoAberto
      estabelecimentoId={estabelecimentoId}
      produtoId={produtoId}
      podeMarcar={MARCA[secao]}
      aoVoltar={() => void navegar(caminhoDaSecao(estabelecimentoId, secao))}
    />
  );
}
