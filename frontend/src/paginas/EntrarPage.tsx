import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { useSessao } from '../auth/useSessao';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { Aviso } from '../componentes/Aviso';
import { Botao } from '../componentes/Botao';
import { Campo } from '../componentes/Campo';

/**
 * Entrar: telefone e senha.
 *
 * <h2>É telefone, e há senha</h2>
 *
 * O `LoginRequest` do contrato é `{ telefone, senha }`. Não é e-mail — e não é
 * o código de seis dígitos, que serve para verificar o telefone no cadastro e
 * não para entrar. (Eu afirmei o contrário em 28/09, ao criticar o protótipo, e
 * estava errado; a correção está na página do marco 2.)
 *
 * <h2>Nenhum seletor de perfil, e essa ausência é a decisão</h2>
 *
 * O protótipo tinha um `<select>` chamado "Tipo de acesso" com Administrador,
 * Colaborador, Entregador e Cliente, e o destino depois do login era a página
 * escolhida. Isso é autorização declarada pelo cliente, e é o inverso deste
 * sistema: o token tem seis claims e **nenhuma permissão dentro**
 * (`CLAUDE.md`: *"Permissão é do vínculo usuário × estabelecimento e é resolvida
 * por requisição"*). O que se pode fazer vem do `merchant`, por loja.
 *
 * <h2>O 401 não explica, e a tela não inventa</h2>
 *
 * O `identity` responde 401 com `detail: "telefone ou senha inválidos"` — uma
 * mensagem só, de propósito (ADR-037 §7): distinguir "não existe" de "senha
 * errada" transformaria o login num verificador de quem tem conta. A tela ecoa
 * o que veio e não acrescenta palpite.
 */
export function EntrarPage() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const local = useLocation();

  const [telefone, setTelefone] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const destino = (local.state as { de?: string } | null)?.de ?? '/painel';

  /**
   * O tratador é síncrono, e o trabalho é assíncrono.
   *
   * `onSubmit` espera `void`. Passar uma função `async` direto devolve uma
   * promessa que ninguém recebe: se ela rejeitasse, o erro sumiria no
   * `unhandledrejection` em vez de virar mensagem na tela. O `void` explícito
   * diz que a promessa é tratada por dentro — e é, pelo `try/catch` do `enviar`.
   */
  function aoEnviar(evento: React.FormEvent) {
    evento.preventDefault();
    void enviar();
  }

  async function enviar() {
    setErro(null);

    if (telefone.trim().length === 0 || senha.length === 0) {
      setErro('Preencha telefone e senha.');
      return;
    }

    setOcupado(true);
    try {
      await entrar(telefone.trim(), senha);
      // `void`: no React Router 7 o navigate pode devolver promessa, e esperar por
      // ela aqui prenderia o `finally` que solta o botão.
      void navegar(destino, { replace: true });
    } catch (causa) {
      if (causa instanceof ErroDaApi) {
        // O 401 do login TEM corpo; o 401 de token não tem. O `??` é essa
        // diferença, e não defensividade genérica.
        setErro(causa.detalhe ?? 'Não foi possível entrar.');
      } else if (causa instanceof SemResposta) {
        setErro('Não foi possível falar com o servidor. Verifique se ele está no ar.');
      } else {
        setErro('Não foi possível entrar.');
      }
    } finally {
      setOcupado(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-sm flex-col justify-center gap-6 px-4 py-10">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
          Entrar
        </h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">Painel de quem vende.</p>
      </header>

      <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-4">
        {erro !== null && <Aviso tom="erro">{erro}</Aviso>}

        <Campo
          rotulo="Telefone"
          type="tel"
          inputMode="tel"
          autoComplete="username"
          value={telefone}
          onChange={(evento) => setTelefone(evento.target.value)}
          placeholder="(21) 99999-0000"
        />

        <Campo
          rotulo="Senha"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={(evento) => setSenha(evento.target.value)}
        />

        <Botao type="submit" ocupado={ocupado}>
          {ocupado ? 'Entrando…' : 'Entrar'}
        </Botao>
      </form>

      <p className="text-sm text-slate-600 dark:text-slate-400">
        Ainda não tem conta?{' '}
        <Link
          to="/cadastrar"
          className="font-medium text-slate-900 underline underline-offset-4 dark:text-slate-100"
        >
          Criar conta
        </Link>
      </p>
    </main>
  );
}
