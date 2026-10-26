import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { cadastrar, pedirCodigo } from '../api/autenticacao';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { useSessao } from '../auth/useSessao';
import { CampoDoCartao } from '../aki/CampoDoCartao';
import { sairDaDemonstracao } from '../aki/demo';
import { AvisoDoCartao, CabecalhoDoCartao, TelaDeEntrada } from '../aki/TelaDeEntrada';

/**
 * Criar conta, em dois passos, no visual da tela de entrada do protótipo.
 *
 * 1. o telefone, para o `identity` mandar o código de verificação;
 * 2. o código, o nome e a senha — e a conta já entra.
 *
 * A resposta do primeiro passo é a mesma exista ou não o telefone: a tela não
 * diz "este número já tem conta" (o servidor não conta, de propósito).
 */
function traduzir(causa: unknown): string {
  if (causa instanceof ErroDaApi) return causa.detalhe ?? 'Não foi possível concluir o cadastro.';
  if (causa instanceof SemResposta) {
    return 'Não foi possível falar com o servidor. Verifique se ele está no ar.';
  }
  return 'Não foi possível concluir o cadastro.';
}

export function CadastrarPage() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const local = useLocation();
  // De onde a pessoa veio e o que ela já pediu para fazer na volta — hoje, aceitar o
  // convite (ADR-076): a intenção viaja com a volta.
  const volta = local.state as { de?: string; acao?: string } | null;
  const destino = volta?.de ?? '/painel';
  const naVolta = volta?.acao === undefined ? undefined : { acao: volta.acao };

  const [passo, definirPasso] = useState<'telefone' | 'dados'>('telefone');
  const [telefone, definirTelefone] = useState('');
  const [codigo, definirCodigo] = useState('');
  const [nome, definirNome] = useState('');
  const [senha, definirSenha] = useState('');
  const [erro, definirErro] = useState<string | null>(null);
  const [ocupado, definirOcupado] = useState(false);

  async function pedir(evento: FormEvent) {
    evento.preventDefault();
    definirErro(null);
    if (telefone.trim() === '') {
      definirErro('Informe o telefone.');
      return;
    }
    definirOcupado(true);
    try {
      await pedirCodigo(telefone.trim());
      definirPasso('dados');
    } catch (causa) {
      definirErro(traduzir(causa));
    } finally {
      definirOcupado(false);
    }
  }

  async function concluir(evento: FormEvent) {
    evento.preventDefault();
    definirErro(null);
    if (codigo.trim() === '' || nome.trim() === '') {
      definirErro('Preencha o código e o nome.');
      return;
    }
    if (senha.length < 8 || senha.length > 72) {
      definirErro('A senha precisa ter de 8 a 72 caracteres.');
      return;
    }
    definirOcupado(true);
    try {
      await cadastrar({
        telefone: telefone.trim(),
        codigo: codigo.trim(),
        nome: nome.trim(),
        senha,
      });
      await entrar(telefone.trim(), senha);
      sairDaDemonstracao();
      void navegar(destino, { replace: true, state: naVolta });
    } catch (causa) {
      definirErro(traduzir(causa));
    } finally {
      definirOcupado(false);
    }
  }

  return (
    <TelaDeEntrada>
      <CabecalhoDoCartao
        selo="Nova conta"
        icone="person_add"
        titulo="Criar conta"
        texto={
          passo === 'telefone'
            ? 'Primeiro o telefone, para verificar.'
            : 'Agora o código, seu nome e uma senha.'
        }
      />

      {passo === 'telefone' ? (
        <form className="login-form" noValidate onSubmit={(evento) => void pedir(evento)}>
          <CampoDoCartao
            id="cadastroTelefone"
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
        <form className="login-form" noValidate onSubmit={(evento) => void concluir(evento)}>
          <AvisoDoCartao>
            Pedido aceito. Se este telefone puder receber um código, ele foi pedido. O código vale
            por dez minutos e aceita cinco tentativas.
          </AvisoDoCartao>
          <CampoDoCartao
            id="cadastroCodigo"
            rotulo="Código de verificação"
            icone="pin"
            inputMode="numeric"
            autoComplete="one-time-code"
            dica="Seis dígitos."
            value={codigo}
            onChange={(evento) => definirCodigo(evento.target.value)}
          />
          <CampoDoCartao
            id="cadastroNome"
            rotulo="Seu nome"
            icone="badge"
            autoComplete="name"
            maxLength={120}
            value={nome}
            onChange={(evento) => definirNome(evento.target.value)}
          />
          <CampoDoCartao
            id="cadastroSenha"
            rotulo="Senha"
            icone="lock"
            type="password"
            autoComplete="new-password"
            dica="De 8 a 72 caracteres."
            value={senha}
            onChange={(evento) => definirSenha(evento.target.value)}
          />
          <button
            type="submit"
            className={`login-button${ocupado ? ' loading' : ''}`}
            disabled={ocupado}
          >
            <span className="button-text">{ocupado ? 'Criando…' : 'Criar conta e entrar'}</span>
          </button>
          {erro !== null && <AvisoDoCartao tom="erro">{erro}</AvisoDoCartao>}
        </form>
      )}

      <div className="login-footer">
        {passo === 'dados' && (
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
          Já tem conta?{' '}
          <Link to="/entrar" state={local.state as { de?: string } | null}>
            Entrar
          </Link>
        </p>
      </div>
    </TelaDeEntrada>
  );
}
