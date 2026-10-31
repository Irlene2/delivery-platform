import { Navigate, Route, Routes } from 'react-router';
import { AcompanhamentoDoPedido } from './cliente/AcompanhamentoDoPedido';
import { CardapioDoCliente } from './cliente/CardapioDoCliente';
import { InicioDoCliente } from './cliente/InicioDoCliente';
import { LojaPublica } from './cliente/LojaPublica';
import { MeusPedidos } from './cliente/MeusPedidos';
import { PedidosDaVitrine } from './cliente/PedidosDaVitrine';
import { CadastrarPage } from './paginas/CadastrarPage';
import { CadastroDeLojaPage } from './paginas/CadastroDeLojaPage';
import { ConvitePage } from './paginas/ConvitePage';
import { EntrarPage } from './paginas/EntrarPage';
import { PainelPage } from './paginas/PainelPage';
import { CadastroDeProdutos } from './painel/CadastroDeProdutos';
import { CardapioDaLoja } from './painel/CardapioDaLoja';
import { ConfiguracoesDaLoja } from './painel/ConfiguracoesDaLoja';
import { FechamentoDoDia } from './painel/FechamentoDoDia';
import { Disponibilidade } from './painel/Disponibilidade';
import { EditorDeProduto } from './painel/EditorDeProduto';
import { Equipe } from './painel/Equipe';
import { HorarioDaLoja } from './painel/Horario';
import { InicioDoPainel } from './painel/InicioDoPainel';
import { InicioDaLoja, LojaDoPainel } from './painel/LojaDoPainel';
import { MinhasEntregas } from './painel/MinhasEntregas';
import { PedidosDaLoja } from './painel/PedidosDaLoja';
import { ExigeSessao } from './rotas/ExigeSessao';

/**
 * As rotas. As telas são as do protótipo `aki_delivery`, com o mesmo CSS.
 *
 * A de `*` não é descuido: sem ela, um caminho errado renderiza página em
 * branco sem erro nenhum no console.
 */
export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/painel" replace />} />
      <Route path="/entrar" element={<EntrarPage />} />
      <Route path="/cadastrar" element={<CadastrarPage />} />
      {/* ADR-070 §4: pública, fora do ExigeSessao — quem abre o link pode não ter conta. */}
      <Route path="/convite/:token" element={<ConvitePage />} />
      {/* ADR-077: o cliente compra pelo link da loja, sem conta. Públicas, fora do ExigeSessao. */}
      <Route path="/loja/:estabelecimentoId" element={<LojaPublica />} />
      <Route path="/loja/:estabelecimentoId/pedidos" element={<PedidosDaVitrine />} />
      <Route path="/pedido/:codigo" element={<AcompanhamentoDoPedido />} />

      {/* ADR-061: a loja escolhida mora na URL, e cada tela do painel tem endereço. */}
      <Route
        path="/painel"
        element={
          <ExigeSessao>
            <PainelPage />
          </ExigeSessao>
        }
      >
        <Route index element={<InicioDoPainel />} />
        <Route path="lojas/nova" element={<CadastroDeLojaPage />} />
        <Route path="lojas/:estabelecimentoId" element={<LojaDoPainel />}>
          <Route index element={<InicioDaLoja />} />
          <Route path="produtos" element={<CadastroDeProdutos />} />
          {/* Segmento fixo ganha do :produtoId no ranking do roteador. ADR-067 §3. */}
          <Route path="produtos/novo" element={<EditorDeProduto />} />
          <Route path="produtos/:produtoId" element={<EditorDeProduto />} />
          <Route path="disponibilidade" element={<Disponibilidade />} />
          <Route path="equipe" element={<Equipe />} />
          <Route path="horario" element={<HorarioDaLoja />} />
          <Route path="cardapio" element={<CardapioDaLoja />} />
          <Route path="pedidos" element={<PedidosDaLoja />} />
          <Route path="configuracoes" element={<ConfiguracoesDaLoja />} />
          <Route path="fechamento" element={<FechamentoDoDia />} />
          <Route path="entregas" element={<MinhasEntregas />} />
        </Route>
      </Route>

      {/* A área do cliente do protótipo, com dados de demonstração. O pedido de verdade é o
          /loja/:estabelecimentoId acima. */}
      <Route path="/cliente" element={<InicioDoCliente />} />
      <Route path="/cliente/cardapio" element={<CardapioDoCliente />} />
      <Route path="/cliente/pedidos" element={<MeusPedidos />} />
      {/* O app do entregador agora mora na loja (painel/MinhasEntregas). */}
      <Route path="/entregador" element={<Navigate to="/painel" replace />} />

      <Route path="*" element={<Navigate to="/painel" replace />} />
    </Routes>
  );
}
