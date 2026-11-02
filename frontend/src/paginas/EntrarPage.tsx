import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { useSessao } from '../auth/useSessao';
import { sairDaDemonstracao } from '../aki/demo';
import { AvisoDoCartao, CabecalhoDoCartao, TelaDeEntrada } from '../aki/TelaDeEntrada';

/**
 * Entrar — a tela de login do protótipo, ligada ao `identity`.
 *
 * Entra por **telefone e senha** — e só por eles: os botões de Google e Apple do protótipo
 * saíram em 02/11/2026, porque a conta é o telefone e um segundo jeito de entrar só confundia. O perfil não é escolhido aqui: depois do
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
  // A recuperação de senha volta para cá com o telefone e o aviso de que deu certo.
  const volta = local.state as {
    de?: string;
    acao?: string;
    telefone?: string;
    senhaRedefinida?: boolean;
  } | null;
  const destino = volta?.de ?? '/painel';
  const naVolta = volta?.acao === undefined ? undefined : { acao: volta.acao };

  const [telefone, definirTelefone] = useState(volta?.telefone ?? '');
  const [senha, definirSenha] = useState('');
  const [verSenha, definirVerSenha] = useState(false);
  const [erros, definirErros] = useState<{ telefone?: string; senha?: string }>({});
  const [ocupado, definirOcupado] = useState(false);
  const [aviso, definirAviso] = useState<string | null>(
    volta?.senhaRedefinida === true ? 'Senha alterada. Entre com a senha nova.' : null,
  );

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
            <Link
              to="/recuperar-senha"
              className="forgot-password"
              id="forgotPassword"
              state={telefone.trim() === '' ? undefined : { telefone: telefone.trim() }}
            >
              Esqueci minha senha
            </Link>
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
