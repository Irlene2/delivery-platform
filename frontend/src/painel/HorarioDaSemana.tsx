import { useState, type FormEvent } from 'react';

import { fraseDaRecusa } from '../api/cardapio';
import { caminhoDoHorario, definirHorario, type Horario, type NovoHorario } from '../api/horario';
import { useRecurso } from '../api/useRecurso';
import { useSessao } from '../auth/useSessao';
import { Aviso } from '../componentes/Aviso';
import { Botao } from '../componentes/Botao';
import { Campo } from '../componentes/Campo';

/** Os sete dias, na ordem da semana, com o nome que o contrato usa (`DayOfWeek`). */
export const DIAS = [
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

/** A frase que a tela diz antes de salvar sem faixa nenhuma (ADR-074 §2). */
export const FRASE_DO_VAZIO =
  'Salvar sem nenhuma faixa deixa a loja sem horário: ela não abre por horário, e marcar o ' +
  'que acabou como esgotado hoje passa a ser recusado.';

/** `HH:mm` — o servidor pode devolver os segundos, e o campo de hora mostra minutos. */
function hora(valor: string | undefined): string {
  return (valor ?? '').slice(0, 5);
}

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

/**
 * O corpo do `PUT`: a semana inteira, e **só os dias com faixa** — o domínio descarta dia
 * com lista vazia na normalização, e mandar só o que existe é mandar o que vai ficar.
 */
function corpoDe(semana: Semana): NovoHorario {
  const horarioDeFuncionamento: NovoHorario['horarioDeFuncionamento'] = {};
  for (const [dia] of DIAS) {
    if (semana[dia].length > 0) horarioDeFuncionamento[dia] = semana[dia];
  }
  return { horarioDeFuncionamento };
}

function cruzaAMeiaNoite(faixa: Faixa): boolean {
  return faixa.inicio !== '' && faixa.fim !== '' && faixa.fim < faixa.inicio;
}

/**
 * A seção **Horário** (ADR-074): a semana inteira numa tela, lida com o `GET` da E-5 e
 * salva com um `PUT`.
 *
 * <h2>A tela não valida o que o domínio decide</h2>
 *
 * Faixas sobrepostas passam; fim antes do início passa, e a tela **nomeia** a faixa que
 * cruza a meia-noite; início igual ao fim vai ao servidor e volta recusado, com a frase
 * dele. O que a tela confere é forma de campo — hora é hora — e nada mais
 * (`premissas-do-front.md`).
 *
 * <h2>Lida uma vez, editada aqui</h2>
 *
 * O `useRecurso` lê quando a loja muda, e não volta a ler sozinho; o editor só monta
 * quando a leitura está pronta, e semeia o estado dele com o que voltou. Depois de salvar,
 * a tela mostra **o que o `PUT` devolveu** — a semana normalizada pelo domínio — e não
 * relê.
 */
export function HorarioDaSemana({ estabelecimentoId }: { estabelecimentoId: string }) {
  const recurso = useRecurso<Horario>(caminhoDoHorario(estabelecimentoId));

  return (
    <section className="flex flex-col gap-6">
      <header>
        <h2 className="text-xl font-semibold text-slate-900 dark:text-slate-50">Horário</h2>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
          As faixas em que a loja abre, dia por dia. Uma faixa que fecha antes de abrir atravessa a
          meia-noite.
        </p>
      </header>
      {recurso.estado === 'carregando' && (
        <p className="text-sm text-slate-600 dark:text-slate-400">Carregando o horário…</p>
      )}
      {recurso.estado === 'erro' && <ErroDeLeitura erro={recurso.erro} />}
      {recurso.estado === 'pronto' && (
        <EditorDoHorario estabelecimentoId={estabelecimentoId} inicial={recurso.dados} />
      )}
    </section>
  );
}

function ErroDeLeitura({ erro }: { erro: Error }) {
  const frase = fraseDaRecusa(
    erro,
    { 403: 'Você não tem permissão para ver o horário desta loja.' },
    'Não foi possível ler o horário agora.',
  );
  return frase === null ? null : <Aviso tom="erro">{frase}</Aviso>;
}

function EditorDoHorario({
  estabelecimentoId,
  inicial,
}: {
  estabelecimentoId: string;
  inicial: Horario;
}) {
  const token = useSessao().sessao?.token;
  const [semana, definirSemana] = useState<Semana>(() => semanaDe(inicial));
  const [confirmandoVazio, definirConfirmandoVazio] = useState(false);
  const [enviando, definirEnviando] = useState(false);
  const [recusa, definirRecusa] = useState<string | null>(null);
  const [salvo, definirSalvo] = useState(false);

  const vazia = DIAS.every(([dia]) => semana[dia].length === 0);

  function mudar(dia: Dia, faixas: Faixa[]) {
    definirSemana((atual) => ({ ...atual, [dia]: faixas }));
    definirSalvo(false);
    definirConfirmandoVazio(false);
  }

  async function salvar() {
    if (token === undefined) return;
    definirEnviando(true);
    definirRecusa(null);
    try {
      const gravado = await definirHorario(estabelecimentoId, corpoDe(semana), token);
      definirSemana(semanaDe(gravado));
      definirSalvo(true);
      definirConfirmandoVazio(false);
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
    const incompleta = DIAS.some(([dia]) =>
      semana[dia].some((faixa) => faixa.inicio === '' || faixa.fim === ''),
    );
    if (incompleta) {
      definirRecusa('Preencha a hora de abrir e a de fechar de cada faixa.');
      return;
    }
    if (vazia && !confirmandoVazio) {
      definirConfirmandoVazio(true);
      return;
    }
    void salvar();
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-6" noValidate>
      {vazia && (
        <p className="text-sm font-medium text-slate-800 dark:text-slate-200">
          Esta loja não abre por horário.
        </p>
      )}

      {DIAS.map(([dia, nome]) => (
        <fieldset key={dia} className="flex flex-col gap-3">
          <legend className="text-sm font-semibold text-slate-900 dark:text-slate-100">
            {nome}
          </legend>
          {semana[dia].map((faixa, indice) => (
            <div key={indice} className="flex flex-wrap items-end gap-3">
              <Campo
                rotulo={`${nome}, faixa ${indice + 1}: abre às`}
                type="time"
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
              <Campo
                rotulo={`${nome}, faixa ${indice + 1}: fecha às`}
                type="time"
                value={faixa.fim}
                dica={
                  cruzaAMeiaNoite(faixa) ? 'Cruza a meia-noite: fecha no dia seguinte.' : undefined
                }
                onChange={(evento) =>
                  mudar(
                    dia,
                    semana[dia].map((uma, i) =>
                      i === indice ? { ...uma, fim: evento.target.value } : uma,
                    ),
                  )
                }
              />
              <Botao
                type="button"
                variante="discreto"
                onClick={() =>
                  mudar(
                    dia,
                    semana[dia].filter((_, i) => i !== indice),
                  )
                }
              >
                {`Remover a faixa ${indice + 1} de ${nome}`}
              </Botao>
            </div>
          ))}
          <div>
            <Botao
              type="button"
              variante="discreto"
              onClick={() => mudar(dia, [...semana[dia], { inicio: '', fim: '' }])}
            >
              {`Acrescentar faixa em ${nome}`}
            </Botao>
          </div>
        </fieldset>
      ))}

      {confirmandoVazio && (
        <div className="flex flex-col gap-3">
          <Aviso tom="neutro">{FRASE_DO_VAZIO}</Aviso>
          <div className="flex gap-3">
            <Botao type="submit" ocupado={enviando}>
              Salvar sem horário
            </Botao>
            <Botao type="button" variante="discreto" onClick={() => definirConfirmandoVazio(false)}>
              Voltar
            </Botao>
          </div>
        </div>
      )}
      {recusa !== null && <Aviso tom="erro">{recusa}</Aviso>}
      {salvo && <Aviso tom="neutro">Horário salvo.</Aviso>}
      {!confirmandoVazio && (
        <div>
          <Botao type="submit" ocupado={enviando}>
            Salvar horário
          </Botao>
        </div>
      )}
    </form>
  );
}
