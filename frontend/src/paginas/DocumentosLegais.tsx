import { useEffect, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router';

/**
 * Os Termos de uso e o Aviso de privacidade (13/11/2026). Páginas públicas, sem login, que a
 * tela de entrada, o cadastro, o cardápio e o checkout apontam.
 *
 * O texto segue o que o sistema faz de verdade — o que ele guarda, por quanto tempo (ADR-013)
 * e onde o pagamento acontece (ADR-023). **Não passou por revisão jurídica**: a pauta dessa
 * revisão está em `docs/operacao/revisao-juridica.md`, e a página diz isso ao leitor.
 */

const VERSAO = '13/11/2026';

function Documento({ titulo, children }: { titulo: string; children: ReactNode }) {
  const navegar = useNavigate();
  useEffect(() => {
    document.title = `${titulo} — AKI Delivery`;
    window.scrollTo({ top: 0 });
  }, [titulo]);

  return (
    <main className="legal">
      <div className="legal-topo">
        <button
          type="button"
          className="legal-voltar"
          onClick={() => {
            if (window.history.length > 1) window.history.back();
            else void navegar('/entrar');
          }}
        >
          <span className="material-symbols-outlined">arrow_back</span>Voltar
        </button>
        <span className="legal-marca">
          <b>AKI</b> Delivery
        </span>
      </div>
      <article className="legal-texto">
        <h1>{titulo}</h1>
        <p className="legal-versao">Versão de {VERSAO}</p>
        {children}
        <p className="legal-nota">
          O AKI Delivery está em fase de testes. Este texto descreve o que o sistema faz hoje e
          ainda vai passar por revisão jurídica antes do uso comercial.
        </p>
        <nav className="legal-links" aria-label="Documentos">
          <Link to="/termos">Termos de uso</Link>
          <Link to="/privacidade">Aviso de privacidade</Link>
        </nav>
      </article>
    </main>
  );
}

export function TermosDeUsoPage() {
  return (
    <Documento titulo="Termos de uso">
      <h2>1. O que é o AKI Delivery</h2>
      <p>
        O AKI Delivery é um sistema para lojas de bairro receberem pedidos pelo link do cardápio,
        acompanharem o preparo e a entrega e fecharem o caixa do dia. O cliente chama a loja no
        WhatsApp, recebe o link do cardápio, escolhe os produtos e faz o pedido, sem baixar
        aplicativo e sem criar conta.
      </p>

      <h2>2. Quem usa</h2>
      <ul>
        <li>
          <b>A loja:</b> o responsável cria a conta com o telefone, cadastra a loja e convida a
          equipe e os entregadores, cada um com a própria conta e as permissões que a loja dá.
        </li>
        <li>
          <b>O cliente:</b> faz o pedido pelo link do cardápio, informando nome, telefone e, na
          entrega, o endereço. Ele acompanha o pedido pelo link que recebe.
        </li>
      </ul>

      <h2>3. O pedido é entre o cliente e a loja</h2>
      <p>
        O AKI Delivery não vende, não prepara e não entrega produtos. Cardápio, preços, fotos,
        horário de funcionamento, taxa de entrega, prazo e atendimento são definidos pela loja, que
        responde por eles. O total do pedido é calculado pelo sistema com os preços que a loja
        cadastrou.
      </p>

      <h2>4. Pagamento</h2>
      <p>
        O pagamento acontece na entrega ou na retirada, direto com a loja: em dinheiro, no cartão da
        maquininha da loja ou por Pix na conta da loja. O AKI Delivery não recebe, não guarda e não
        repassa dinheiro; ele apenas registra como cada pedido foi pago. O Pix informado pelo
        entregador fica "a conferir" até a loja confirmar no extrato dela.
      </p>

      <h2>5. Conta e senha</h2>
      <p>
        A conta é pessoal. Quem a usa responde pelo que faz com ela e deve guardar a senha. Se
        esquecer, use "Esqueci minha senha" na tela de entrada.
      </p>

      <h2>6. O que não é permitido</h2>
      <ul>
        <li>cadastrar produto ou loja de outra pessoa, ou com informação falsa;</li>
        <li>usar o sistema para vender produtos proibidos por lei;</li>
        <li>tentar acessar dados de outra loja ou atrapalhar o funcionamento do sistema.</li>
      </ul>
      <p>Quem fizer isso pode ter a conta suspensa.</p>

      <h2>7. Disponibilidade</h2>
      <p>
        Na fase de testes, o sistema pode ficar fora do ar, mudar ou perder dados de teste, sem
        aviso. Não use o AKI Delivery como único registro de uma operação real enquanto ele estiver
        nessa fase.
      </p>

      <h2>8. Privacidade</h2>
      <p>
        O que o sistema guarda, para quê e por quanto tempo está no{' '}
        <Link to="/privacidade">Aviso de privacidade</Link>.
      </p>

      <h2>9. Mudanças</h2>
      <p>
        Estes termos podem mudar. A versão em vigor é sempre a desta página, com a data no topo.
      </p>
    </Documento>
  );
}

export function AvisoDePrivacidadePage() {
  return (
    <Documento titulo="Aviso de privacidade">
      <p>
        Este aviso explica, em linguagem simples, quais dados o AKI Delivery usa, para quê, com quem
        compartilha e por quanto tempo guarda, conforme a Lei Geral de Proteção de Dados (LGPD, Lei
        nº 13.709/2018).
      </p>

      <h2>1. Quem decide sobre os seus dados</h2>
      <p>
        Quando você faz um pedido, os seus dados são da <b>loja</b> em que você comprou: é ela que
        decide como atendê-lo. O AKI Delivery é o sistema que a loja usa e trata os dados para que o
        pedido funcione. Os dados de quem tem conta no sistema — o responsável pela loja, a equipe e
        os entregadores — são tratados pelo AKI Delivery para manter a conta.
      </p>

      <h2>2. Quais dados usamos</h2>
      <ul>
        <li>
          <b>Do cliente:</b> nome, telefone (WhatsApp), endereço de entrega, o que foi pedido, a
          forma de pagamento escolhida e o troco.
        </li>
        <li>
          <b>De quem tem conta:</b> nome e telefone. A senha é guardada só de forma cifrada, que
          ninguém consegue ler.
        </li>
        <li>
          <b>Da loja:</b> nome, CPF ou CNPJ, telefone, endereço, horário, cardápio e as fotos dos
          produtos.
        </li>
      </ul>
      <p>
        <b>Não coletamos</b> número de cartão, documento do cliente, localização pelo GPS nem dados
        de navegação para propaganda.
      </p>

      <h2>3. Para quê</h2>
      <ul>
        <li>
          <b>Fazer o pedido acontecer:</b> preparar, entregar, avisar o cliente e registrar o
          pagamento. É a execução do pedido que você fez (LGPD, art. 7º, V).
        </li>
        <li>
          <b>Cumprir obrigações legais:</b> manter o registro das vendas que a loja precisa guardar
          (art. 7º, II).
        </li>
        <li>
          <b>Segurança:</b> proteger as contas e evitar fraude e abuso (art. 7º, IX).
        </li>
      </ul>

      <h2>4. Com quem compartilhamos</h2>
      <ul>
        <li>
          <b>A loja e a equipe dela</b> veem os pedidos da loja. O entregador vê o nome, o telefone
          e o endereço só dos pedidos que estão com ele.
        </li>
        <li>
          <b>WhatsApp:</b> quando a loja avisa o cliente, a mensagem sai do WhatsApp da própria
          loja, com as regras de privacidade do WhatsApp.
        </li>
      </ul>
      <p>
        <b>Não vendemos nem alugamos dados</b> para ninguém.
      </p>

      <h2>5. No seu navegador</h2>
      <p>
        O sistema guarda no navegador, só enquanto a aba está aberta, o login de quem tem conta e o
        carrinho do cliente. Não usamos cookies de rastreamento nem de propaganda.
      </p>

      <h2>6. Por quanto tempo guardamos</h2>
      <ul>
        <li>
          <b>Pedidos e vendas:</b> 5 anos, pelo registro fiscal. Se o cliente pedir a exclusão, o
          nome, o telefone e o endereço são apagados do pedido, e ficam só os valores.
        </li>
        <li>
          <b>Contato do cliente:</b> enquanto ele continuar pedindo, e até 2 anos sem pedido.
        </li>
        <li>
          <b>Conta no sistema:</b> enquanto a pessoa fizer parte da loja, e depois de forma anônima,
          por até 5 anos.
        </li>
        <li>
          <b>Registros técnicos:</b> 30 dias.
        </li>
      </ul>

      <h2>7. Seus direitos</h2>
      <p>
        Você pode pedir para confirmar se temos dados seus, ver, corrigir, levar para outro serviço,
        apagar o que não precisa ser guardado e saber com quem os compartilhamos (LGPD, art. 18).
      </p>
      <p>
        <b>Se você é cliente</b>, fale com a loja em que comprou, pelo WhatsApp dela: ela é quem
        responde pelo seu pedido, e o AKI Delivery a ajuda a atender. <b>Se você tem conta</b> no
        sistema, fale com o responsável pela sua loja ou com a equipe do AKI Delivery.
      </p>

      <h2>8. Segurança</h2>
      <p>
        O acesso é por conta e senha, cada pessoa vê só o que a permissão dela permite, as senhas
        são guardadas cifradas e a comunicação usa conexão segura (HTTPS).
      </p>
    </Documento>
  );
}
