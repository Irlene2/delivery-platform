import { Navigate, Route, Routes } from 'react-router';
import { EntrarPage } from './paginas/EntrarPage';
import { CadastrarPage } from './paginas/CadastrarPage';
import { PainelPage } from './paginas/PainelPage';
import { ExigeSessao } from './rotas/ExigeSessao';

/**
 * Quatro rotas, e a de `*` não é descuido: sem ela, um caminho errado renderiza
 * página em branco sem erro nenhum no console, que é o defeito mais difícil de
 * diagnosticar num roteador.
 */
export function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/painel" replace />} />
      <Route path="/entrar" element={<EntrarPage />} />
      <Route path="/cadastrar" element={<CadastrarPage />} />
      <Route
        path="/painel"
        element={
          <ExigeSessao>
            <PainelPage />
          </ExigeSessao>
        }
      />
      <Route path="*" element={<Navigate to="/painel" replace />} />
    </Routes>
  );
}
