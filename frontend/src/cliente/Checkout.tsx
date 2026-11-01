import { useEffect, useState, type FormEvent } from 'react';
import type {
  FormaDePagamento,
  Modalidade,
  NovoPedido,
  PedidoPublico,
  VitrineDaLoja,
} from '../api/pedidos';
import { dinheiro } from '../aki/formatos';
import { totaisDoCarrinho, type ItemDoCarrinho } from '../aki/pedidos';
import { lerPreco, mensagemDoErro } from '../painel/catalogo';
import type { Vitrine } from '../pedido/fontes';
import { ROTULO_DA_FORMA, ROTULO_DA_MODALIDADE } from '../pedido/regras';
import { AvisoDeLojaFechada } from './AvisoDeLojaFechada';

/**
 * O resumo e o checkout de verdade, no lugar do bloco de pagamento da demonstração — com as
 * mesmas classes do protótipo (`.order-summary`, `.payment-checkout`, `.payment-select`,
 * `.cash-change-option`).
 *
 * **O total desta tela é exibição.** O servidor lê o preço no cardápio e devolve o total que
 * vale; se o cardápio mudou entre abrir a página e pedir, é o da resposta que aparece no
 * acompanhamento.
 */
const FORMAS: FormaDePagamento[] = ['PIX', 'CARTAO', 'DINHEIRO'];

export function ResumoComCheckout({
  itens,
  vitrine,
  aoCriar,
}: {
  itens: readonly ItemDoCarrinho[];
  vitrine: Vitrine;
  aoCriar: (pedido: PedidoPublico) => void;
}) {
  const [configuracao, definirConfiguracao] = useState<VitrineDaLoja | null>(null);
  const [nome, definirNome] = useState('');
  const [telefone, definirTelefone] = useState('');
  const [modalidade, definirModalidade] = useState<Modalidade>('ENTREGA');
  const [logradouro, definirLogradouro] = useState('');
  const [bairro, definirBairro] = useState('');
  const [complemento, definirComplemento] = useState('');
  const [forma, definirForma] = useState<FormaDePagamento>('PIX');
  const [troco, definirTroco] = useState('');
  const [enviando, definirEnviando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);

  useEffect(() => {
    let atual = true;
    vitrine
      .configuracao()
      .then((lida) => {
        if (!atual) return;
        definirConfiguracao(lida);
        // A loja que só faz retirada já abre na retirada.
        if (!lida.aceitaEntrega) definirModalidade('RETIRADA');
      })
      .catch(() => {
        // Sem a configuração, o checkout segue com o padrão do servidor (taxa zero, as
        // duas modalidades); o servidor recusa o que a loja não aceitar, com a frase.
        if (atual)
          definirConfiguracao({ taxaDeEntrega: 0, aceitaEntrega: true, aceitaRetirada: true });
      });
    return () => {
      atual = false;
    };
  }, [vitrine]);

  const fechada = configuracao?.aberta === false;
  const entrega = modalidade === 'ENTREGA';
  const taxa = entrega ? (configuracao?.taxaDeEntrega ?? 0) : 0;
  const { subtotal, fee, total } = totaisDoCarrinho(itens, taxa);
  const modalidades: Modalidade[] = [
    ...(configuracao?.aceitaEntrega !== false ? (['ENTREGA'] as const) : []),
    ...(configuracao?.aceitaRetirada !== false ? (['RETIRADA'] as const) : []),
  ];

  async function finalizar(evento: FormEvent) {
    evento.preventDefault();
    if (itens.length === 0) {
      definirRecusa('Adicione itens ao carrinho antes de finalizar.');
      return;
    }
    if (nome.trim() === '' || telefone.trim() === '') {
      definirRecusa('Informe seu nome e seu telefone com DDD.');
      return;
    }
    if (entrega && (logradouro.trim() === '' || bairro.trim() === '')) {
      definirRecusa('Para entrega, informe o endereço e o bairro.');
      return;
    }
    let trocoPara: number | undefined;
    if (forma === 'DINHEIRO' && troco.trim() !== '') {
      const lido = lerPreco(troco);
      if (lido === null) {
        definirRecusa('O troco precisa ser um valor, como 50,00.');
        return;
      }
      trocoPara = lido;
    }

    const novo: NovoPedido = {
      cliente: { nome: nome.trim(), telefone: telefone.trim() },
      modalidade,
      ...(entrega
        ? {
            endereco: {
              logradouro: logradouro.trim(),
              bairro: bairro.trim(),
              ...(complemento.trim() === '' ? {} : { complemento: complemento.trim() }),
            },
          }
        : {}),
      itens: itens.map((item) => ({ produtoId: item.id, quantidade: item.qty })),
      pagamento: { forma, ...(trocoPara === undefined ? {} : { trocoPara }) },
    };

    definirRecusa(null);
    definirEnviando(true);
    try {
      aoCriar(await vitrine.fazer(novo));
    } catch (erro) {
      definirRecusa(mensagemDoErro(erro));
    } finally {
      definirEnviando(false);
    }
  }

  return (
    <form onSubmit={(evento) => void finalizar(evento)} noValidate>
      <div className="summary-heading">
        <span>Resumo do pedido</span>
        <span className="summary-live">
          <i /> Atualizado
        </span>
      </div>
      <div className="summary-row">
        <span>Subtotal</span>
        <b data-subtotal="">{dinheiro(subtotal)}</b>
      </div>
      <div className="summary-row">
        <span>{entrega ? 'Taxa de entrega' : 'Retirada no balcão'}</span>
        <b data-fee="">{dinheiro(fee)}</b>
      </div>
      <div className="summary-divider" />
      <div className="summary-row total">
        <span>Total</span>
        <b data-total="">{dinheiro(total)}</b>
      </div>

      <div className="payment-checkout menu-payment-checkout">
        <div className="summary-heading">
          <span>Seus dados</span>
          <span className="material-symbols-outlined section-icon">person</span>
        </div>
        <label className="cash-change-option" htmlFor="ckNome">
          Nome
          <input
            id="ckNome"
            autoComplete="name"
            maxLength={120}
            value={nome}
            onChange={(evento) => definirNome(evento.target.value)}
          />
        </label>
        <label className="cash-change-option" htmlFor="ckTelefone">
          Telefone (WhatsApp)
          <input
            id="ckTelefone"
            type="tel"
            autoComplete="tel"
            placeholder="(21) 99999-0000"
            value={telefone}
            onChange={(evento) => definirTelefone(evento.target.value)}
          />
        </label>
      </div>

      <div className="payment-checkout menu-payment-checkout">
        <div className="summary-heading">
          <span>Entrega</span>
          <span className="material-symbols-outlined section-icon">two_wheeler</span>
        </div>
        <label className="payment-select-label" htmlFor="ckModalidade">
          Como você quer receber
        </label>
        <select
          id="ckModalidade"
          className="payment-select"
          value={modalidade}
          onChange={(evento) => definirModalidade(evento.target.value as Modalidade)}
        >
          {modalidades.map((uma) => (
            <option key={uma} value={uma}>
              {ROTULO_DA_MODALIDADE[uma]}
            </option>
          ))}
        </select>
        {entrega && (
          <>
            <label className="cash-change-option" htmlFor="ckLogradouro">
              Endereço (rua e número)
              <input
                id="ckLogradouro"
                autoComplete="street-address"
                maxLength={200}
                value={logradouro}
                onChange={(evento) => definirLogradouro(evento.target.value)}
              />
            </label>
            <label className="cash-change-option" htmlFor="ckBairro">
              Bairro
              <input
                id="ckBairro"
                maxLength={80}
                value={bairro}
                onChange={(evento) => definirBairro(evento.target.value)}
              />
            </label>
            <label className="cash-change-option" htmlFor="ckComplemento">
              Complemento (opcional)
              <input
                id="ckComplemento"
                maxLength={80}
                value={complemento}
                onChange={(evento) => definirComplemento(evento.target.value)}
              />
            </label>
          </>
        )}
      </div>

      <div className="payment-checkout menu-payment-checkout">
        <div className="summary-heading">
          <span>Forma de pagamento</span>
          <span className="material-symbols-outlined section-icon">payments</span>
        </div>
        <label className="payment-select-label" htmlFor="clientPaymentMethod">
          {entrega ? 'Você paga na entrega' : 'Você paga no balcão'}
        </label>
        <select
          id="clientPaymentMethod"
          className="payment-select"
          value={forma}
          onChange={(evento) => definirForma(evento.target.value as FormaDePagamento)}
        >
          {FORMAS.map((uma) => (
            <option key={uma} value={uma}>
              {ROTULO_DA_FORMA[uma]}
            </option>
          ))}
        </select>
        <label
          className="cash-change-option"
          id="cashChangeWrap"
          hidden={forma !== 'DINHEIRO'}
          htmlFor="cashChange"
        >
          Troco para (opcional)
          <input
            id="cashChange"
            inputMode="decimal"
            placeholder="Ex.: 50,00"
            value={troco}
            onChange={(evento) => definirTroco(evento.target.value)}
          />
        </label>
      </div>

      <AvisoDeLojaFechada vitrine={configuracao} />
      <button
        type="submit"
        className="btn btn-primary full checkout-btn"
        disabled={enviando || fechada}
        aria-busy={enviando}
      >
        <span className="material-symbols-outlined">shopping_bag</span>
        {enviando ? 'Enviando pedido…' : fechada ? 'Loja fechada agora' : 'Finalizar pedido'}
      </button>
      {recusa !== null && (
        <p className="prod-msg erro" role="alert" style={{ marginTop: '10px' }}>
          {recusa}
        </p>
      )}
      <small className="secure-note">
        <span className="material-symbols-outlined">verified_user</span> O total final é confirmado
        pela loja ao receber o pedido
      </small>
    </form>
  );
}
