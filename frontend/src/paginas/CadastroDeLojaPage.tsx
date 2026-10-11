import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';

import { ErroDaApi, SemResposta } from '../api/cliente';
import { criarEstabelecimento, type NovaLoja } from '../api/estabelecimentos';
import { useSessao } from '../auth/useSessao';
import { Aviso } from '../componentes/Aviso';
import { Botao } from '../componentes/Botao';
import { Campo } from '../componentes/Campo';
import { caminhoDaSecao } from '../painel/caminhos';
import { useContextoDoPainel } from '../painel/contextoDoPainel';

type TipoDeOperacao = NovaLoja['tipoDeOperacao'];
type FormaDePagar = NovaLoja['metodosPorModalidade'][string][number];
type Fuso = NovaLoja['identificacao']['fusoHorario'];

/**
 * **As listas do formulário saem do contrato pelo typecheck** — o precedente do
 * `ESTADOS`/`ROTULO` da `disponibilidade.ts`. Cada `Record` abaixo é indexado pelo tipo
 * gerado: uma zona, um tipo de operação ou uma forma de pagar a mais ou a menos no
 * contrato, e o `typecheck` cai aqui. As opções em tempo de execução são as chaves dele.
 *
 * Por que não o `--enum-values` do `openapi-typescript`, que exportaria os arrays
 * prontos: medido na W-E, ele gera para os valores do mapa `metodosPorModalidade` um
 * array com tipo errado, e o arquivo gerado deixa de compilar.
 */
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

/**
 * As modalidades **não** estão no contrato: o mapa `metodosPorModalidade` é declarado
 * com chave `string`, e só as formas de pagar vêm como enum (medido na W-E). Esta é a
 * única lista do formulário que a tela escreve — e uma divergência com o domínio vira
 * 400 sem `detail`, não uma regra a mais aqui. Está no `dividas.md`.
 */
const MODALIDADES = ['ENTREGA', 'RETIRADA'] as const;
type Modalidade = (typeof MODALIDADES)[number];

const ROTULO_DA_MODALIDADE: Record<Modalidade, string> = {
  ENTREGA: 'Entrega',
  RETIRADA: 'Retirada',
};

/** O `Record` prende os rótulos ao contrato: um valor novo regerado derruba o typecheck. */
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
const TIPOS_DE_OPERACAO = Object.keys(ROTULO_DO_TIPO) as TipoDeOperacao[];
const FORMAS_DE_PAGAR = Object.keys(ROTULO_DA_FORMA) as FormaDePagar[];

/** ADR-025 §2: *"já preenchido com `America/Sao_Paulo`, que o comerciante pode trocar num toque"*. */
const FUSO_PADRAO: Fuso = 'America/Sao_Paulo';

const MENSAGEM_POR_CODIGO: Record<number, string> = {
  0: 'Não foi possível falar com o servidor.',
  400: 'A loja não foi cadastrada. Confira os campos e tente de novo.',
};
const MENSAGEM_PADRAO = 'Não foi possível cadastrar a loja agora.';

/**
 * O cadastro de loja — a tela que a ADR-060 esperava (W-E).
 *
 * <h2>Os campos são o corpo da rota, e nada além</h2>
 *
 * Identificação, política de troco, tipo de operação e a matriz de modalidade por forma
 * de pagar. **Horário e área de entrega não estão aqui**: a loja nasce sem os dois
 * (ADR-060 §2), e eles são telas próprias.
 *
 * <h2>A tela não antecipa regra de domínio</h2>
 *
 * Com a matriz vazia o botão continua habilitado, e a requisição acontece: quem recusa
 * é o agregado (M12), e a tela mostra o `detail` do 400 **como veio** (ADR-055). O fuso,
 * o tipo de operação e as formas de pagar vêm do contrato — o front **lê** o conjunto, não
 * o copia (ADR-061 §4).
 *
 * <h2>O 201 tem destino</h2>
 *
 * O `estabelecimentoId` da resposta é a loja nova, e o painel vai para o cardápio dela.
 * Antes de ir, relê as lojas: a lista que o painel tinha foi lida antes desta nascer.
 */
export function CadastroDeLojaPage() {
  const token = useSessao().sessao?.token;
  const { recarregarLojas } = useContextoDoPainel();
  const navegar = useNavigate();

  const [nome, definirNome] = useState('');
  const [documento, definirDocumento] = useState('');
  const [telefone, definirTelefone] = useState('');
  const [enderecoTextual, definirEndereco] = useState('');
  const [bairro, definirBairro] = useState('');
  const [fusoHorario, definirFuso] = useState<Fuso>(FUSO_PADRAO);
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
    if (token === undefined) return;

    // Só as linhas marcadas: a loja aceita a modalidade se há forma de pagar para ela.
    // Matriz vazia vai vazia — e é o agregado que diz o que fazer com isso.
    const metodosPorModalidade: NovaLoja['metodosPorModalidade'] = {};
    for (const modalidade of MODALIDADES) {
      if (matriz[modalidade].length > 0) metodosPorModalidade[modalidade] = matriz[modalidade];
    }

    const corpo: NovaLoja = {
      identificacao: { nome, documento, telefone, enderecoTextual, bairro, fusoHorario },
      politicaDeTroco: {
        // Texto do formulário para o número que o contrato pede. Não é conta: é leitura.
        // Vazio não é zero: vira NaN, que o JSON escreve como null, e o @NotNull recusa.
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
        if (erro.status === 401) return; // a sessão caiu; a raiz troca a tela
        const detalhe = erro.detalhe?.trim();
        definirRecusa(
          detalhe !== undefined && detalhe !== ''
            ? detalhe
            : (MENSAGEM_POR_CODIGO[erro.status] ?? MENSAGEM_PADRAO),
        );
      } else if (erro instanceof SemResposta) {
        definirRecusa(MENSAGEM_POR_CODIGO[0] ?? MENSAGEM_PADRAO);
      } else {
        definirRecusa(MENSAGEM_PADRAO);
      }
    } finally {
      definirEnviando(false);
    }
  }

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-50">
          Cadastrar minha loja
        </h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          Horário de funcionamento e áreas de entrega vêm depois, nas telas da loja.
        </p>
      </header>

      {/* A recusa sai quando qualquer campo muda, ou no envio seguinte (ADR-067 §5): na
          passagem humana de 10/10 ela ficava na tela depois de o campo ser corrigido. */}
      <form
        onSubmit={(evento) => void enviar(evento)}
        onChange={() => definirRecusa(null)}
        className="flex flex-col gap-6"
        noValidate
      >
        <fieldset className="flex flex-col gap-3">
          <legend className="mb-2 text-sm font-semibold text-slate-900 dark:text-slate-100">
            A loja
          </legend>
          <Campo
            rotulo="Nome da loja"
            value={nome}
            onChange={(e) => definirNome(e.target.value)}
            aria-required
          />
          <Campo
            rotulo="CPF ou CNPJ"
            value={documento}
            onChange={(e) => definirDocumento(e.target.value)}
            aria-required
          />
          <Campo
            rotulo="Telefone da loja"
            value={telefone}
            onChange={(e) => definirTelefone(e.target.value)}
            aria-required
          />
          <Campo
            rotulo="Endereço"
            value={enderecoTextual}
            onChange={(e) => definirEndereco(e.target.value)}
            aria-required
          />
          <Campo
            rotulo="Bairro"
            value={bairro}
            onChange={(e) => definirBairro(e.target.value)}
            aria-required
          />
          <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">
            Fuso horário
            <select
              value={fusoHorario}
              onChange={(e) => definirFuso(e.target.value as Fuso)}
              className="rounded-md border border-slate-300 bg-white px-3 py-2 text-base text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            >
              {ZONAS.map((zona) => (
                <option key={zona} value={zona}>
                  {ROTULO_DO_FUSO[zona]}
                </option>
              ))}
            </select>
          </label>
        </fieldset>

        <fieldset className="flex flex-col gap-3">
          <legend className="mb-2 text-sm font-semibold text-slate-900 dark:text-slate-100">
            Troco
          </legend>
          <Campo
            rotulo="Fundo máximo de troco (R$)"
            inputMode="decimal"
            value={fundo}
            onChange={(e) => definirFundo(e.target.value)}
            aria-required
          />
          <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-200">
            <input
              type="checkbox"
              checked={aceitaSemTroco}
              onChange={(e) => definirAceitaSemTroco(e.target.checked)}
            />
            Aceitar pedido mesmo sem troco disponível
          </label>
        </fieldset>

        <fieldset className="flex flex-col gap-3">
          <legend className="mb-2 text-sm font-semibold text-slate-900 dark:text-slate-100">
            Como a loja opera
          </legend>
          <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700 dark:text-slate-200">
            Tipo de operação
            <select
              value={tipo}
              onChange={(e) => definirTipo(e.target.value as TipoDeOperacao)}
              className="rounded-md border border-slate-300 bg-white px-3 py-2 text-base text-slate-900 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
            >
              {TIPOS_DE_OPERACAO.map((valor) => (
                <option key={valor} value={valor}>
                  {ROTULO_DO_TIPO[valor]}
                </option>
              ))}
            </select>
          </label>

          {MODALIDADES.map((modalidade) => (
            <div
              key={modalidade}
              role="group"
              aria-label={`Formas de pagar na ${ROTULO_DA_MODALIDADE[modalidade].toLowerCase()}`}
            >
              <p className="text-sm font-medium text-slate-700 dark:text-slate-200">
                {ROTULO_DA_MODALIDADE[modalidade]}
              </p>
              <div className="mt-1 flex flex-wrap gap-4">
                {FORMAS_DE_PAGAR.map((forma) => (
                  <label
                    key={forma}
                    className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-200"
                  >
                    <input
                      type="checkbox"
                      checked={matriz[modalidade].includes(forma)}
                      onChange={() => alternar(modalidade, forma)}
                    />
                    {ROTULO_DA_FORMA[forma]}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </fieldset>

        {recusa !== null && <Aviso tom="erro">{recusa}</Aviso>}

        <div>
          <Botao type="submit" ocupado={enviando}>
            Cadastrar loja
          </Botao>
        </div>
      </form>
    </section>
  );
}
