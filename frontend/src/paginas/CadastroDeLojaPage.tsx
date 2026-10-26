import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { ErroDaApi, SemResposta } from '../api/cliente';
import { criarEstabelecimento, type NovaLoja } from '../api/estabelecimentos';
import { useSessao } from '../auth/useSessao';
import { Casca } from '../aki/Casca';
import { emDemonstracao } from '../aki/demo';
import { toast } from '../aki/toast';
import { caminhoDaSecao } from '../painel/caminhos';
import { useContextoDoPainel } from '../painel/contextoDoPainel';

/**
 * Cadastrar minha loja (`POST /me/estabelecimentos`), no visual do protótipo.
 * Horário e áreas de entrega vêm depois, nas telas da loja.
 */
type TipoDeOperacao = NovaLoja['tipoDeOperacao'];
type FormaDePagar = NovaLoja['metodosPorModalidade'][string][number];
type Fuso = NovaLoja['identificacao']['fusoHorario'];

const ROTULO_DO_FUSO: Record<Fuso, string> = {
  'America/Araguaina': 'Araguaína',
  'America/Bahia': 'Bahia',
  'America/Belem': 'Belém',
  'America/Boa_Vista': 'Boa Vista',
  'America/Campo_Grande': 'Campo Grande',
  'America/Cuiaba': 'Cuiabá',
  'America/Eirunepe': 'Eirunepé',
  'America/Fortaleza': 'Fortaleza',
  'America/Maceio': 'Maceió',
  'America/Manaus': 'Manaus',
  'America/Noronha': 'Fernando de Noronha',
  'America/Porto_Velho': 'Porto Velho',
  'America/Recife': 'Recife',
  'America/Rio_Branco': 'Rio Branco',
  'America/Santarem': 'Santarém',
  'America/Sao_Paulo': 'São Paulo (horário de Brasília)',
};
const MODALIDADES = ['ENTREGA', 'RETIRADA'] as const;
type Modalidade = (typeof MODALIDADES)[number];
const ROTULO_DA_MODALIDADE: Record<Modalidade, string> = {
  ENTREGA: 'Entrega',
  RETIRADA: 'Retirada',
};
const ROTULO_DO_TIPO: Record<TipoDeOperacao, string> = {
  PRODUCAO: 'Produção (cozinha)',
  SEPARACAO: 'Separação (balcão)',
  MISTA: 'Mista',
};
const ROTULO_DA_FORMA: Record<FormaDePagar, string> = {
  DINHEIRO: 'Dinheiro',
  CARTAO: 'Cartão',
  PIX: 'Pix',
};
const ZONAS = Object.keys(ROTULO_DO_FUSO) as Fuso[];
const TIPOS = Object.keys(ROTULO_DO_TIPO) as TipoDeOperacao[];
const FORMAS = Object.keys(ROTULO_DA_FORMA) as FormaDePagar[];

export function CadastroDeLojaPage() {
  const token = useSessao().sessao?.token;
  const { recarregarLojas } = useContextoDoPainel();
  const navegar = useNavigate();

  const [nome, definirNome] = useState('');
  const [documento, definirDocumento] = useState('');
  const [telefone, definirTelefone] = useState('');
  const [enderecoTextual, definirEndereco] = useState('');
  const [bairro, definirBairro] = useState('');
  const [fusoHorario, definirFuso] = useState<Fuso>('America/Sao_Paulo');
  const [fundo, definirFundo] = useState('');
  const [aceitaSemTroco, definirAceitaSemTroco] = useState(false);
  const [tipo, definirTipo] = useState<TipoDeOperacao>('PRODUCAO');
  const [matriz, definirMatriz] = useState<Record<Modalidade, FormaDePagar[]>>({
    ENTREGA: [],
    RETIRADA: [],
  });
  const [enviando, definirEnviando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);

  function alternar(modalidade: Modalidade, forma: FormaDePagar) {
    definirMatriz((atual) => ({
      ...atual,
      [modalidade]: atual[modalidade].includes(forma)
        ? atual[modalidade].filter((uma) => uma !== forma)
        : [...atual[modalidade], forma],
    }));
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (emDemonstracao()) {
      toast('No modo demonstração a loja não é criada. Entre com o servidor no ar.');
      return;
    }
    if (token === undefined) return;
    const metodosPorModalidade: NovaLoja['metodosPorModalidade'] = {};
    for (const modalidade of MODALIDADES) {
      if (matriz[modalidade].length > 0) metodosPorModalidade[modalidade] = matriz[modalidade];
    }
    const corpo: NovaLoja = {
      identificacao: { nome, documento, telefone, enderecoTextual, bairro, fusoHorario },
      politicaDeTroco: {
        // Vazio vira NaN de propósito: o servidor recusa com a frase certa (o front não decide a regra).
        fundoMaximoDeTroco: fundo.trim() === '' ? Number.NaN : Number(fundo.replace(',', '.')),
        aceitaPedidoSemTrocoDisponivel: aceitaSemTroco,
      },
      tipoDeOperacao: tipo,
      metodosPorModalidade,
    };
    definirEnviando(true);
    definirRecusa(null);
    try {
      const loja = await criarEstabelecimento(corpo, token);
      recarregarLojas();
      void navegar(caminhoDaSecao(String(loja.estabelecimentoId), 'cardapio'));
    } catch (erro) {
      if (erro instanceof ErroDaApi) {
        if (erro.status === 401) return;
        const detalhe = erro.detalhe?.trim();
        definirRecusa(
          detalhe !== undefined && detalhe !== ''
            ? detalhe
            : erro.status === 400
              ? 'A loja não foi cadastrada. Confira os campos e tente de novo.'
              : 'Não foi possível cadastrar a loja agora.',
        );
      } else if (erro instanceof SemResposta) {
        definirRecusa('Não foi possível falar com o servidor.');
      } else {
        definirRecusa('Não foi possível cadastrar a loja agora.');
      }
    } finally {
      definirEnviando(false);
    }
  }

  const texto = (
    rotulo: string,
    valor: string,
    definir: (v: string) => void,
    extra: object = {},
  ) => (
    <label className="prod-campo">
      {rotulo} *
      <input
        value={valor}
        onChange={(evento) => definir(evento.target.value)}
        aria-required
        {...extra}
      />
    </label>
  );

  return (
    <Casca
      menu={[{ chave: 'lojas', rotulo: 'Minhas lojas', icone: 'storefront', para: '/painel' }]}
      atual="lojas"
      titulo="Cadastrar minha loja"
      subtitulo="Horário de funcionamento e áreas de entrega vêm depois, nas telas da loja."
      rodape={
        <>
          <b>AKI Delivery</b>
          <br />
          Gestão da operação
        </>
      }
      acoes={<div className="avatar">AD</div>}
    >
      <section className="panel prod-panel">
        <form
          className="prod-form"
          noValidate
          onSubmit={(evento) => void enviar(evento)}
          onChange={() => definirRecusa(null)}
        >
          <fieldset className="prod-grupo">
            <legend>A loja</legend>
            {texto('Nome da loja', nome, definirNome)}
            <div className="prod-linha">
              {texto('CPF ou CNPJ', documento, definirDocumento)}
              {texto('Telefone da loja', telefone, definirTelefone, { inputMode: 'tel' })}
            </div>
            {texto('Endereço', enderecoTextual, definirEndereco)}
            <div className="prod-linha">
              {texto('Bairro', bairro, definirBairro)}
              <label className="prod-campo">
                Fuso horário
                <select
                  value={fusoHorario}
                  onChange={(evento) => definirFuso(evento.target.value as Fuso)}
                >
                  {ZONAS.map((zona) => (
                    <option key={zona} value={zona}>
                      {ROTULO_DO_FUSO[zona]}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </fieldset>

          <fieldset className="prod-grupo">
            <legend>Troco</legend>
            {texto('Fundo máximo de troco (R$)', fundo, definirFundo, { inputMode: 'decimal' })}
            <label className="prod-opcao">
              <input
                type="checkbox"
                checked={aceitaSemTroco}
                onChange={(evento) => definirAceitaSemTroco(evento.target.checked)}
              />{' '}
              <span>
                <b>Aceitar pedido mesmo sem troco disponível</b>
              </span>
            </label>
          </fieldset>

          <fieldset className="prod-grupo">
            <legend>Como a loja opera</legend>
            <label className="prod-campo">
              Tipo de operação
              <select
                value={tipo}
                onChange={(evento) => definirTipo(evento.target.value as TipoDeOperacao)}
              >
                {TIPOS.map((valor) => (
                  <option key={valor} value={valor}>
                    {ROTULO_DO_TIPO[valor]}
                  </option>
                ))}
              </select>
            </label>
            {MODALIDADES.map((modalidade) => (
              <div
                key={modalidade}
                className="prod-campo"
                role="group"
                aria-label={`Formas de pagar na ${ROTULO_DA_MODALIDADE[modalidade].toLowerCase()}`}
              >
                <span>{ROTULO_DA_MODALIDADE[modalidade]}</span>
                <div className="prod-acoes">
                  {FORMAS.map((forma) => (
                    <label key={forma} className="prod-opcao">
                      <input
                        type="checkbox"
                        checked={matriz[modalidade].includes(forma)}
                        onChange={() => alternar(modalidade, forma)}
                      />{' '}
                      <span>
                        <b>{ROTULO_DA_FORMA[forma]}</b>
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </fieldset>

          {recusa !== null && (
            <p className="prod-msg erro" role="alert">
              {recusa}
            </p>
          )}
          <div className="prod-acoes">
            <button type="submit" className="btn btn-primary" aria-busy={enviando}>
              <span className="material-symbols-outlined">add_business</span>Cadastrar loja
            </button>
            <button type="button" className="btn btn-light" onClick={() => void navegar('/painel')}>
              <span className="material-symbols-outlined">close</span>Sair sem salvar
            </button>
          </div>
        </form>
      </section>
    </Casca>
  );
}
