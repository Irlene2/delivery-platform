import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { cadastrar, pedirCodigo } from '../api/autenticacao';
import { useSessao } from '../auth/useSessao';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { Aviso } from '../componentes/Aviso';
import { Botao } from '../componentes/Botao';
import { Campo } from '../componentes/Campo';

/**
 * Cadastro em dois passos, porque a API tem dois passos.
 *
 * <h2>O 202 não confirma nada, e a tela não pode dizer que confirma</h2>
 *
 * `POST /api/v1/auth/verification-code` devolve **202 sem corpo, sempre** —
 * inclusive para um telefone que já tem conta, caso em que nenhum código é
 * criado (ADR-042 §5). A razão é que responder diferente transformaria a rota
 * num verificador de quem já é cliente.
 *
 * <p>Então a tela <b>não</b> diz "enviamos um código para o seu telefone". Ela
 * diz o que é verdade: o pedido foi aceito, e se este telefone puder receber um
 * código, ele está a caminho. É uma frase pior de ler e é a única honesta — e
 * uma tela que prometesse a entrega faria o suporte receber a ligação de quem
 * já tinha conta e nunca recebeu nada.
 *
 * <h2>O transporte do código é humano, e isso está escrito</h2>
 *
 * Não existe adaptador de canal ainda: a ADR-042 registra que o código sai em
 * texto claro no log do serviço, e que quem faz o onboarding o entrega. É por
 * isso que o 202 é 202 e não 200 — o que a requisição produz acontece fora
 * dela.
 *
 * <h2>O que o `signup` devolve, e o que ele não devolve</h2>
 *
 * **201 com `{ id }`** — e nenhum token. Cadastrar não autentica. Por isso esta
 * página faz login em seguida com a senha que a pessoa acabou de escolher, em
 * vez de mandá-la digitar tudo de novo numa segunda tela.
 */
export function CadastrarPage() {
  const { entrar } = useSessao();
  const navegar = useNavigate();

  const [passo, setPasso] = useState<'telefone' | 'dados'>('telefone');
  const [telefone, setTelefone] = useState('');
  const [codigo, setCodigo] = useState('');
  const [nome, setNome] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  function traduzir(causa: unknown): string {
    if (causa instanceof ErroDaApi) {
      // 400 do identity vem com detail: "código de verificação inválido ou
      // expirado" ou "telefone inválido". O 401 não acontece aqui.
      return causa.detalhe ?? 'Não foi possível concluir o cadastro.';
    }
    if (causa instanceof SemResposta) {
      return 'Não foi possível falar com o servidor. Verifique se ele está no ar.';
    }
    return 'Não foi possível concluir o cadastro.';
  }

  /** Tratador síncrono, trabalho assíncrono — ver o javadoc do mesmo par na EntrarPage. */
  function aoPedir(evento: React.FormEvent) {
    evento.preventDefault();
    void pedir();
  }

  async function pedir() {
    setErro(null);
    if (telefone.trim().length === 0) {
      setErro('Informe o telefone.');
      return;
    }
    setOcupado(true);
    try {
      await pedirCodigo(telefone.trim());
      setPasso('dados');
    } catch (causa) {
      setErro(traduzir(causa));
    } finally {
      setOcupado(false);
    }
  }

  function aoConcluir(evento: React.FormEvent) {
    evento.preventDefault();
    void concluir();
  }

  async function concluir() {
    setErro(null);

    // As duas conferências que o contrato declara, e só elas: `codigo` e
    // `nome` sem tamanho mínimo além de não-vazio, `senha` de 8 a 72. Copiar
    // para cá uma regra que o servidor não tem seria criar uma segunda verdade
    // — e foi o que o protótipo fez, exigindo seis caracteres de senha.
    if (codigo.trim().length === 0 || nome.trim().length === 0) {
      setErro('Preencha o código e o nome.');
      return;
    }
    if (senha.length < 8 || senha.length > 72) {
      setErro('A senha precisa ter de 8 a 72 caracteres.');
      return;
    }

    setOcupado(true);
    try {
      await cadastrar({
        telefone: telefone.trim(),
        codigo: codigo.trim(),
        nome: nome.trim(),
        senha,
      });
      // O signup não devolve token. O login em seguida evita uma segunda tela
      // pedindo o que a pessoa acabou de digitar.
      await entrar(telefone.trim(), senha);
      void navegar('/painel', { replace: true });
    } catch (causa) {
      setErro(traduzir(causa));
    } finally {
      setOcupado(false);
    }
  }

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-sm flex-col justify-center gap-6 px-4 py-10">
      <header>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
          Criar conta
        </h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          {passo === 'telefone'
            ? 'Primeiro o telefone, para verificar.'
            : 'Agora o código, seu nome e uma senha.'}
        </p>
      </header>

      {erro !== null && <Aviso tom="erro">{erro}</Aviso>}

      {passo === 'telefone' ? (
        <form onSubmit={aoPedir} noValidate className="flex flex-col gap-4">
          <Campo
            rotulo="Telefone"
            type="tel"
            inputMode="tel"
            autoComplete="username"
            value={telefone}
            onChange={(evento) => setTelefone(evento.target.value)}
            placeholder="(21) 99999-0000"
          />
          <Botao type="submit" ocupado={ocupado}>
            {ocupado ? 'Pedindo…' : 'Pedir código'}
          </Botao>
        </form>
      ) : (
        <form onSubmit={aoConcluir} noValidate className="flex flex-col gap-4">
          <Aviso tom="neutro">
            Pedido aceito. <strong>Se este telefone puder receber um código</strong>, ele foi pedido
            — quem faz o seu cadastro entrega os seis dígitos. O código vale por dez minutos e
            aceita cinco tentativas.
          </Aviso>

          <Campo
            rotulo="Código de verificação"
            inputMode="numeric"
            autoComplete="one-time-code"
            value={codigo}
            onChange={(evento) => setCodigo(evento.target.value)}
            dica="Seis dígitos."
          />
          <Campo
            rotulo="Seu nome"
            autoComplete="name"
            maxLength={120}
            value={nome}
            onChange={(evento) => setNome(evento.target.value)}
          />
          <Campo
            rotulo="Senha"
            type="password"
            autoComplete="new-password"
            value={senha}
            onChange={(evento) => setSenha(evento.target.value)}
            dica="De 8 a 72 caracteres."
          />

          <Botao type="submit" ocupado={ocupado}>
            {ocupado ? 'Criando…' : 'Criar conta e entrar'}
          </Botao>
          <Botao type="button" variante="discreto" onClick={() => setPasso('telefone')}>
            Corrigir o telefone
          </Botao>
        </form>
      )}

      <p className="text-sm text-slate-600 dark:text-slate-400">
        Já tem conta?{' '}
        <Link
          to="/entrar"
          className="font-medium text-slate-900 underline underline-offset-4 dark:text-slate-100"
        >
          Entrar
        </Link>
      </p>
    </main>
  );
}
