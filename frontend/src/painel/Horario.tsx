import { useEffect, useState, type FormEvent } from 'react';
import { fraseDaRecusa } from '../api/cardapio';
import { chamar } from '../api/cliente';
import { caminhoDoHorario, definirHorario, type Horario, type NovoHorario } from '../api/horario';
import { useSessao } from '../auth/useSessao';
import { emDemonstracao } from '../aki/demo';
import { gravar, ler } from '../aki/guardado';
import { toast } from '../aki/toast';
import { CascaDaLoja } from './CascaDaLoja';
import { useLojaAtual } from './lojaAtual';

/**
 * O horário da semana (ADR-074), no visual do protótipo: as faixas em que a loja
 * abre, dia por dia. Uma faixa que fecha antes de abrir atravessa a meia-noite.
 *
 * A tela não valida o que o domínio decide (ADR-074): sobreposição e faixa de
 * duração zero são do servidor, e a frase dele aparece. O que a tela confere é só
 * o que ela sabe — hora em branco —, e diz **qual dia**.
 */
const DIAS = [
  ['MONDAY', 'Segunda-feira'],
  ['TUESDAY', 'Terça-feira'],
  ['WEDNESDAY', 'Quarta-feira'],
  ['THURSDAY', 'Quinta-feira'],
  ['FRIDAY', 'Sexta-feira'],
  ['SATURDAY', 'Sábado'],
  ['SUNDAY', 'Domingo'],
] as const;
type Dia = (typeof DIAS)[number][0];
type Faixa = { inicio: string; fim: string };
type Semana = Record<Dia, Faixa[]>;

const FRASE_DO_VAZIO =
  'Salvar sem nenhuma faixa deixa a loja sem horário: ela não abre por horário, e marcar o ' +
  'que acabou como esgotado hoje passa a ser recusado.';

const CHAVE_DEMO = 'aki.horario';
const hora = (valor: string | undefined) => (valor ?? '').slice(0, 5);

function semanaDe(horario: Horario): Semana {
  const lida = horario.horarioDeFuncionamento ?? {};
  const semana = {} as Semana;
  for (const [dia] of DIAS) {
    semana[dia] = (lida[dia] ?? []).map((faixa) => ({
      inicio: hora(faixa.inicio),
      fim: hora(faixa.fim),
    }));
  }
  return semana;
}

function corpoDe(semana: Semana): NovoHorario {
  const horarioDeFuncionamento: NovoHorario['horarioDeFuncionamento'] = {};
  for (const [dia] of DIAS) {
    if (semana[dia].length > 0) horarioDeFuncionamento[dia] = semana[dia];
  }
  return { horarioDeFuncionamento };
}

const cruzaAMeiaNoite = (faixa: Faixa) =>
  faixa.inicio !== '' && faixa.fim !== '' && faixa.fim < faixa.inicio;

function semanaDeExemplo(): Semana {
  const noite = [{ inicio: '18:00', fim: '00:00' }];
  return {
    MONDAY: [],
    TUESDAY: noite,
    WEDNESDAY: noite,
    THURSDAY: noite,
    FRIDAY: noite,
    SATURDAY: [{ inicio: '18:00', fim: '02:00' }],
    SUNDAY: noite,
  };
}

type Leitura =
  | { estado: 'carregando' }
  | { estado: 'pronto'; semana: Semana }
  | { estado: 'erro'; frase: string };

export function HorarioDaLoja() {
  const { estabelecimentoId } = useLojaAtual();
  const token = useSessao().sessao?.token;
  const demo = emDemonstracao();
  const [leitura, definirLeitura] = useState<Leitura>({ estado: 'carregando' });

  useEffect(() => {
    if (demo) {
      definirLeitura({ estado: 'pronto', semana: ler<Semana>(CHAVE_DEMO, semanaDeExemplo()) });
      return;
    }
    if (token === undefined) return;
    let atual = true;
    chamar<Horario>(caminhoDoHorario(estabelecimentoId), { token })
      .then((horario) => {
        if (atual) definirLeitura({ estado: 'pronto', semana: semanaDe(horario) });
      })
      .catch((erro: unknown) => {
        if (!atual) return;
        const frase = fraseDaRecusa(
          erro,
          { 403: 'Você não tem permissão para ver o horário desta loja.' },
          'Não foi possível ler o horário agora.',
        );
        if (frase !== null) definirLeitura({ estado: 'erro', frase });
      });
    return () => {
      atual = false;
    };
  }, [demo, estabelecimentoId, token]);

  return (
    <CascaDaLoja
      atual="horario"
      titulo="Horário de funcionamento"
      subtitulo="As faixas em que a loja abre, dia por dia."
    >
      {demo && (
        <div className="prod-aviso" role="status">
          Modo demonstração: o horário fica só nesta aba.
        </div>
      )}
      {leitura.estado === 'erro' && (
        <div className="prod-aviso" role="alert">
          {leitura.frase}
        </div>
      )}
      <section className="panel prod-panel">
        <div className="table-title">
          <div>
            <h2>Horário da semana</h2>
            <p className="admin-muted">
              Uma faixa que fecha antes de abrir atravessa a meia-noite.
            </p>
          </div>
          <span className="material-symbols-outlined section-icon">schedule</span>
        </div>
        {leitura.estado === 'carregando' && <p className="admin-muted">Carregando o horário…</p>}
        {leitura.estado === 'pronto' && (
          <EditorDoHorario
            inicial={leitura.semana}
            salvar={async (semana) => {
              if (demo) {
                gravar(CHAVE_DEMO, semana);
                return semana;
              }
              if (token === undefined) throw new Error('sem sessão');
              return semanaDe(await definirHorario(estabelecimentoId, corpoDe(semana), token));
            }}
          />
        )}
      </section>
      <footer className="footer">
        <b>AKI Delivery</b> © 2026 — Horário
      </footer>
    </CascaDaLoja>
  );
}

function EditorDoHorario({
  inicial,
  salvar,
}: {
  inicial: Semana;
  salvar: (semana: Semana) => Promise<Semana>;
}) {
  const [semana, definirSemana] = useState<Semana>(inicial);
  const [confirmandoVazio, definirConfirmandoVazio] = useState(false);
  const [enviando, definirEnviando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);
  const [diasIncompletos, definirDiasIncompletos] = useState<Dia[]>([]);
  const vazia = DIAS.every(([dia]) => semana[dia].length === 0);

  function mudar(dia: Dia, faixas: Faixa[]) {
    definirSemana((atual) => ({ ...atual, [dia]: faixas }));
    definirConfirmandoVazio(false);
    // Corrigiu? A recusa antiga sai da tela (passagem de 19/10, item 11).
    definirRecusa(null);
    definirDiasIncompletos([]);
  }

  async function gravarSemana() {
    definirEnviando(true);
    definirRecusa(null);
    try {
      definirSemana(await salvar(semana));
      definirConfirmandoVazio(false);
      toast('Horário salvo');
    } catch (falha) {
      const frase = fraseDaRecusa(
        falha,
        { 403: 'Você não tem permissão para mudar o horário desta loja.' },
        'Não foi possível salvar o horário agora.',
      );
      if (frase !== null) definirRecusa(frase);
    } finally {
      definirEnviando(false);
    }
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const incompletos = DIAS.filter(([dia]) =>
      semana[dia].some((faixa) => faixa.inicio === '' || faixa.fim === ''),
    ).map(([dia]) => dia);
    if (incompletos.length > 0) {
      definirDiasIncompletos(incompletos);
      const nomes = DIAS.filter(([dia]) => incompletos.includes(dia)).map(([, nome]) => nome);
      definirRecusa(`Preencha a hora de abrir e a de fechar em: ${nomes.join(', ')}.`);
      return;
    }
    if (vazia && !confirmandoVazio) {
      definirConfirmandoVazio(true);
      return;
    }
    void gravarSemana();
  }

  return (
    <form className="prod-form" noValidate onSubmit={enviar}>
      {vazia && <p className="prod-nota">Esta loja não abre por horário.</p>}
      {DIAS.map(([dia, nome]) => (
        <fieldset key={dia} className="prod-grupo" id={`dia-${dia}`}>
          <legend>{nome}</legend>
          {semana[dia].length === 0 && <p className="prod-dica">Fechado.</p>}
          {semana[dia].map((faixa, indice) => (
            <div
              key={indice}
              className="prod-linha"
              style={{ gridTemplateColumns: '1fr 1fr auto', alignItems: 'end' }}
            >
              <label className="prod-campo">
                Abre às
                <input
                  type="time"
                  aria-label={`${nome}, faixa ${indice + 1}: abre às`}
                  className={
                    diasIncompletos.includes(dia) && faixa.inicio === ''
                      ? 'campo-invalido'
                      : undefined
                  }
                  value={faixa.inicio}
                  onChange={(evento) =>
                    mudar(
                      dia,
                      semana[dia].map((uma, i) =>
                        i === indice ? { ...uma, inicio: evento.target.value } : uma,
                      ),
                    )
                  }
                />
              </label>
              <label className="prod-campo">
                Fecha às
                <input
                  type="time"
                  aria-label={`${nome}, faixa ${indice + 1}: fecha às`}
                  className={
                    diasIncompletos.includes(dia) && faixa.fim === '' ? 'campo-invalido' : undefined
                  }
                  value={faixa.fim}
                  onChange={(evento) =>
                    mudar(
                      dia,
                      semana[dia].map((uma, i) =>
                        i === indice ? { ...uma, fim: evento.target.value } : uma,
                      ),
                    )
                  }
                />
                {cruzaAMeiaNoite(faixa) && (
                  <small className="prod-dica">Cruza a meia-noite: fecha no dia seguinte.</small>
                )}
              </label>
              <button
                type="button"
                className="btn btn-light"
                aria-label={`Remover a faixa ${indice + 1} de ${nome}`}
                onClick={() =>
                  mudar(
                    dia,
                    semana[dia].filter((_, i) => i !== indice),
                  )
                }
              >
                <span className="material-symbols-outlined">delete</span>
              </button>
            </div>
          ))}
          <div className="prod-acoes">
            <button
              type="button"
              className="btn btn-light"
              aria-label={`Acrescentar faixa em ${nome}`}
              onClick={() => mudar(dia, [...semana[dia], { inicio: '', fim: '' }])}
            >
              <span className="material-symbols-outlined">add</span>Acrescentar faixa
            </button>
          </div>
        </fieldset>
      ))}

      {confirmandoVazio && (
        <div className="prod-confirmar" role="alertdialog" aria-label="Salvar sem horário">
          <span>{FRASE_DO_VAZIO}</span>
          <button type="submit" className="btn btn-danger-soft" aria-busy={enviando}>
            Salvar sem horário
          </button>
          <button
            type="button"
            className="btn btn-light"
            onClick={() => definirConfirmandoVazio(false)}
          >
            Voltar
          </button>
        </div>
      )}
      {recusa !== null && (
        <p className="prod-msg erro" role="alert">
          {recusa}
        </p>
      )}
      {!confirmandoVazio && (
        <div className="prod-acoes">
          <button type="submit" className="btn btn-primary" aria-busy={enviando}>
            <span className="material-symbols-outlined">save</span>Salvar horário
          </button>
        </div>
      )}
    </form>
  );
}
