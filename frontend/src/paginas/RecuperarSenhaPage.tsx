import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { pedirCodigoDeRecuperacao, redefinirSenha } from '../api/autenticacao';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { CampoDoCartao } from '../aki/CampoDoCartao';
import { AvisoDoCartao, CabecalhoDoCartao, TelaDeEntrada } from '../aki/TelaDeEntrada';

/**
 * Esqueci minha senha (02/11/2026), em dois passos, no visual da criação de conta:
 *
 * 1. o telefone, para o `identity` gerar o código;
 * 2. o código e a senha nova — e volta para a entrada, com o telefone preenchido.
 *
 * Como no cadastro, a resposta do primeiro passo é a mesma tenha o telefone conta ou não.
 */
function traduzir(causa: unknown): string {
  if (causa instanceof ErroDaApi) return causa.detalhe ?? 'Não foi possível trocar a senha.';
  if (causa instanceof SemResposta) {
    return 'Não foi possível falar com o servidor. Verifique se ele está no ar.';
  }
  return 'Não foi possível trocar a senha.';
}

export function RecuperarSenhaPage() {
  const navegar = useNavigate();
  const vindo = useLocation().state as { telefone?: string } | null;

  const [passo, definirPasso] = useState<'telefone' | 'senha'>('telefone');
  const [telefone, definirTelefone] = useState(vindo?.telefone ?? '');
  const [codigo, definirCodigo] = useState('');
  // Só com a bandeira de desenvolvimento do identity (ADR-042, emenda de 01/11/2026).
  const [codigoDeTeste, definirCodigoDeTeste] = useState<string | null>(null);
  const [senha, definirSenha] = useState('');
  const [confirmacao, definirConfirmacao] = useState('');
  const [erro, definirErro] = useState<string | null>(null);
  const [ocupado, definirOcupado] = useState(false);

  async function pedir(evento: FormEvent) {
    evento.preventDefault();
    definirErro(null);
    if (telefone.trim() === '') {
      definirErro('Informe o telefone da sua conta.');
      return;
    }
    definirOcupado(true);
    try {
      const devolvido = await pedirCodigoDeRecuperacao(telefone.trim());
      definirCodigoDeTeste(devolvido);
      if (devolvido !== null) definirCodigo(devolvido);
      definirPasso('senha');
    } catch (causa) {
      definirErro(traduzir(causa));
    } finally {
      definirOcupado(false);
    }
  }

  async function trocar(evento: FormEvent) {
    evento.preventDefault();
    definirErro(null);
    if (codigo.trim() === '') {
      definirErro('Informe o código de verificação.');
      return;
    }
    if (senha.length < 8 || senha.length > 72) {
      definirErro('A senha precisa ter de 8 a 72 caracteres.');
      return;
    }
    if (senha !== confirmacao) {
      definirErro('As duas senhas não são iguais.');
      return;
    }
    definirOcupado(true);
    try {
      await redefinirSenha({ telefone: telefone.trim(), codigo: codigo.trim(), novaSenha: senha });
      void navegar('/entrar', {
        replace: true,
        state: { telefone: telefone.trim(), senhaRedefinida: true },
      });
    } catch (causa) {
      definirErro(traduzir(causa));
    } finally {
      definirOcupado(false);
    }
  }

  return (
    <TelaDeEntrada>
      <CabecalhoDoCartao
        selo="Recuperar acesso"
        icone="lock_reset"
        titulo="Esqueci minha senha"
        texto={
          passo === 'telefone'
            ? 'Informe o telefone da sua conta para receber um código.'
            : 'Agora o código e a senha nova.'
        }
      />

      {passo === 'telefone' ? (
        <form className="login-form" noValidate onSubmit={(evento) => void pedir(evento)}>
          <CampoDoCartao
            id="recuperarTelefone"
            rotulo="Telefone"
            icone="phone"
            type="tel"
            inputMode="tel"
            autoComplete="username"
            placeholder="(21) 99999-0000"
            value={telefone}
            onChange={(evento) => definirTelefone(evento.target.value)}
          />
          <button
            type="submit"
            className={`login-button${ocupado ? ' loading' : ''}`}
            disabled={ocupado}
          >
            <span className="button-text">{ocupado ? 'Pedindo…' : 'Pedir código'}</span>
          </button>
          {erro !== null && <AvisoDoCartao tom="erro">{erro}</AvisoDoCartao>}
        </form>
      ) : (
        <form className="login-form" noValidate onSubmit={(evento) => void trocar(evento)}>
          <AvisoDoCartao>
            Se este telefone tiver conta, o código foi gerado. Ele vale por dez minutos e aceita
            cinco tentativas.
          </AvisoDoCartao>
          {codigoDeTeste !== null && (
            <AvisoDoCartao>
              Ambiente de teste: o código é <b>{codigoDeTeste}</b> e já foi preenchido.
            </AvisoDoCartao>
          )}
          <CampoDoCartao
            id="recuperarCodigo"
            rotulo="Código de verificação"
            icone="pin"
            inputMode="numeric"
            autoComplete="one-time-code"
            dica="Seis dígitos."
            value={codigo}
            onChange={(evento) => definirCodigo(evento.target.value)}
          />
          <CampoDoCartao
            id="recuperarSenha"
            rotulo="Senha nova"
            icone="lock"
            type="password"
            autoComplete="new-password"
            dica="De 8 a 72 caracteres."
            value={senha}
            onChange={(evento) => definirSenha(evento.target.value)}
          />
          <CampoDoCartao
            id="recuperarConfirmacao"
            rotulo="Repita a senha nova"
            icone="lock"
            type="password"
            autoComplete="new-password"
            value={confirmacao}
            onChange={(evento) => definirConfirmacao(evento.target.value)}
          />
          <button
            type="submit"
            className={`login-button${ocupado ? ' loading' : ''}`}
            disabled={ocupado}
          >
            <span className="button-text">{ocupado ? 'Trocando…' : 'Trocar a senha'}</span>
          </button>
          {erro !== null && <AvisoDoCartao tom="erro">{erro}</AvisoDoCartao>}
        </form>
      )}

      <div className="login-footer">
        {passo === 'senha' && (
          <p>
            Telefone errado?{' '}
            <a
              href="#"
              onClick={(evento) => {
                evento.preventDefault();
                definirPasso('telefone');
              }}
            >
              Corrigir o telefone
            </a>
          </p>
        )}
        <p>
          Lembrou a senha? <Link to="/entrar">Entrar</Link>
        </p>
      </div>
    </TelaDeEntrada>
  );
}
