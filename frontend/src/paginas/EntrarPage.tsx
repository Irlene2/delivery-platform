import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { useSessao } from '../auth/useSessao';
import { sairDaDemonstracao } from '../aki/demo';
import { AvisoDoCartao, CabecalhoDoCartao, TelaDeEntrada } from '../aki/TelaDeEntrada';

/**
 * Entrar — a tela de login do protótipo, ligada ao `identity`.
 *
 * Entra por **telefone e senha**. O perfil não é escolhido aqui: depois do
 * login, o painel lê o papel da pessoa em cada loja, e é o servidor que decide.
 *
 * Até 01/11/2026 havia aqui o link "Ver as telas em modo demonstração", com dados
 * de exemplo e sem servidor. Saiu: a loja de teste agora é a "Pizzaria da
 * Fixture", semeada no banco (docs/como-subir-local.md). O código do modo
 * demonstração continua em `aki/demo.ts`, sem porta de entrada.
 */

export function EntrarPage() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const local = useLocation();
  // De onde a pessoa veio e o que ela já pediu para fazer na volta — hoje, aceitar o
  // convite (ADR-076): a intenção viaja com a volta.
  const volta = local.state as { de?: string; acao?: string } | null;
  const destino = volta?.de ?? '/painel';
  const naVolta = volta?.acao === undefined ? undefined : { acao: volta.acao };

  const [telefone, definirTelefone] = useState('');
  const [senha, definirSenha] = useState('');
  const [verSenha, definirVerSenha] = useState(false);
  const [erros, definirErros] = useState<{ telefone?: string; senha?: string }>({});
  const [ocupado, definirOcupado] = useState(false);
  const [aviso, definirAviso] = useState<string | null>(null);

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    definirErros({});
    definirAviso(null);

    const faltando: typeof erros = {};
    if (telefone.trim() === '') faltando.telefone = 'Digite seu telefone.';
    if (senha === '') faltando.senha = 'Digite sua senha.';
    if (faltando.telefone !== undefined || faltando.senha !== undefined) {
      definirErros(faltando);
      return;
    }

    definirOcupado(true);
    try {
      await entrar(telefone.trim(), senha);
      // Quem entrou na demonstração antes de ela sair da tela não fica preso nela.
      sairDaDemonstracao();
      void navegar(destino, { replace: true, state: naVolta });
    } catch (causa) {
      let frase = 'Não foi possível entrar.';
      if (causa instanceof ErroDaApi && causa.detalhe !== undefined) frase = causa.detalhe;
      if (causa instanceof SemResposta) {
        frase = 'Não foi possível falar com o servidor. Verifique se o back-end está no ar.';
      }
      definirErros({ senha: frase });
    } finally {
      definirOcupado(false);
    }
  }

  return (
    <TelaDeEntrada>
      <CabecalhoDoCartao
        selo="Acesso ao sistema"
        icone="lock"
        titulo="Bem-vindo!"
        texto="Entre na sua conta para continuar."
      />

      <form
        id="loginForm"
        className="login-form"
        noValidate
        onSubmit={(evento) => void enviar(evento)}
      >
        <div className="form-group">
          <label htmlFor="emailInput">Telefone</label>
          <div className="input-container">
            <span className="material-icons input-icon">person</span>
            <input
              type="tel"
              id="emailInput"
              name="telefone"
              inputMode="tel"
              placeholder="(21) 99999-0000"
              autoComplete="username"
              className={erros.telefone !== undefined ? 'input-error' : undefined}
              value={telefone}
              onChange={(evento) => definirTelefone(evento.target.value)}
            />
          </div>
          <small className="error-message" id="emailError">
            {erros.telefone}
          </small>
        </div>

        <div className="form-group">
          <div className="form-label-row">
            <label htmlFor="passwordInput">Senha</label>
            <a
              href="#"
              className="forgot-password"
              id="forgotPassword"
              onClick={(evento) => {
                evento.preventDefault();
                definirAviso('A recuperação de senha será disponibilizada em uma próxima etapa.');
              }}
            >
              Esqueci minha senha
            </a>
          </div>
          <div className="input-container">
            <span className="material-icons input-icon">lock</span>
            <input
              type={verSenha ? 'text' : 'password'}
              id="passwordInput"
              name="password"
              placeholder="Digite sua senha"
              autoComplete="current-password"
              className={erros.senha !== undefined ? 'input-error' : undefined}
              value={senha}
              onChange={(evento) => definirSenha(evento.target.value)}
            />
            <button
              type="button"
              className="password-toggle"
              id="passwordToggle"
              aria-label={verSenha ? 'Ocultar senha' : 'Mostrar senha'}
              onClick={() => definirVerSenha((valor) => !valor)}
            >
              <span className="material-icons">{verSenha ? 'visibility' : 'visibility_off'}</span>
            </button>
          </div>
          <small className="error-message" id="passwordError">
            {erros.senha}
          </small>
        </div>

        <div className="login-options">
          <label className="remember-option">
            <input type="checkbox" id="rememberMe" />
            <span>Lembrar meu acesso</span>
          </label>
        </div>

        <button
          type="submit"
          className={`login-button${ocupado ? ' loading' : ''}`}
          id="submitBtn"
          disabled={ocupado}
        >
          <span className="button-text" id="btnText">
            {ocupado ? 'Entrando' : 'Entrar'}
          </span>
        </button>
        {aviso !== null && <AvisoDoCartao>{aviso}</AvisoDoCartao>}
      </form>

      <div className="login-divider">
        <span />
        <small>ou continue com</small>
        <span />
      </div>

      <div className="social-login">
        <button
          type="button"
          className="social-button"
          id="googleLogin"
          aria-label="Entrar com Google"
          onClick={() =>
            definirAviso('O login com Google precisa ser conectado a um serviço de autenticação.')
          }
        >
          <svg className="social-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M21.35 12.27c0-.78-.07-1.54-.22-2.27H12v4.3h5.24a4.48 4.48 0 0 1-1.94 2.94v2.45h3.14c1.84-1.69 2.91-4.18 2.91-7.42Z"
              fill="#4285F4"
            />
            <path
              d="M12 21.5c2.63 0 4.84-.87 6.45-2.35l-3.14-2.45c-.87.58-1.98.93-3.31.93-2.54 0-4.69-1.72-5.46-4.03H3.29v2.53A9.75 9.75 0 0 0 12 21.5Z"
              fill="#34A853"
            />
            <path
              d="M6.54 13.6a5.86 5.86 0 0 1 0-3.2V7.87H3.29a9.76 9.76 0 0 0 0 8.26l3.25-2.53Z"
              fill="#FBBC05"
            />
            <path
              d="M12 6.37c1.43 0 2.71.49 3.72 1.45l2.79-2.79C16.84 3.45 14.63 2.5 12 2.5a9.75 9.75 0 0 0-8.71 5.37l3.25 2.53C7.31 8.09 9.46 6.37 12 6.37Z"
              fill="#EA4335"
            />
          </svg>
          <span>Google</span>
        </button>
        <button
          type="button"
          className="social-button"
          id="appleLogin"
          aria-label="Entrar com Apple"
          onClick={() =>
            definirAviso('O login com Apple precisa ser conectado a um serviço de autenticação.')
          }
        >
          <svg className="social-icon apple-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M17.05 20.28c-.98.95-2.05.8-3.08.35-1.09-.46-2.09-.48-3.24 0-1.44.62-2.2.44-3.06-.35C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.1.8 1.21-.25 2.37-.94 3.66-.84 1.56.13 2.74.74 3.51 1.83-3.22 1.93-2.46 6.18.5 7.36-.59 1.55-1.35 3.09-2.77 3.82ZM12.03 7.25C11.88 4.95 13.74 3.07 15.88 2.89c.3 2.65-2.42 4.62-3.85 4.36Z"
              fill="currentColor"
            />
          </svg>
          <span>Apple</span>
        </button>
      </div>

      <div className="login-footer">
        <p>
          Ainda não possui uma conta?{' '}
          <Link to="/cadastrar" id="createAccount" state={local.state as { de?: string } | null}>
            Cadastre-se
          </Link>
        </p>
      </div>
    </TelaDeEntrada>
  );
}
