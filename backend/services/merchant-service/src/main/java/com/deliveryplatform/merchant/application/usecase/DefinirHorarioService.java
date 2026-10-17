package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.exception.HorarioRecusado;
import com.deliveryplatform.merchant.application.exception.HorarioRecusado.Motivo;
import com.deliveryplatform.merchant.application.port.in.DefinirHorario;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Disponibilidade;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.Faixa;
import com.deliveryplatform.merchant.domain.model.Permissao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O horário semanal, substituído inteiro (ADR-073). É a primeira escrita do
 * {@code merchant} que <b>altera</b> uma loja existente — as anteriores criavam.
 *
 * <p><b>Autoriza antes de buscar</b>, no molde do convite: sem vínculo ativo com
 * {@code CONFIGURAR_LOJA} é o 403 único (M7), loja que não existe inclusive. Buscar
 * primeiro deixaria quem não tem vínculo distinguir o que existe do que não existe
 * (invariante 9, ADR-056).
 *
 * <p><b>Não publica nada</b> (ADR-073 §3). O que o horário muda é {@code aberto}, e a
 * abertura do expediente é da varredura da ADR-046, que compara estado a cada passada:
 * a loja que ganha horário no meio de uma faixa é notada na passada seguinte, pela
 * mesma marca d'água. Publicar aqui seria uma segunda forma de disparar a abertura, e
 * a marcação feita nesse intervalo não é desfeita — o carimbo e a abertura levam o
 * mesmo expediente, e a reativação exige carimbo anterior.
 */
@Service
public class DefinirHorarioService implements DefinirHorario {

    private final MembroRepositorio membros;
    private final EstabelecimentoRepositorio estabelecimentos;

    public DefinirHorarioService(MembroRepositorio membros,
                                 EstabelecimentoRepositorio estabelecimentos) {
        this.membros = membros;
        this.estabelecimentos = estabelecimentos;
    }

    @Override
    @Transactional
    public Disponibilidade definir(UUID lojaId, UUID autorUsuarioId,
                                   Map<DayOfWeek, List<FaixaPedida>> horarioDeFuncionamento) {
        membros.buscarPorUsuarioELoja(autorUsuarioId, lojaId)
                .filter(vinculo -> vinculo.pode(Permissao.CONFIGURAR_LOJA))
                .orElseThrow(AcessoNegado::new);

        Map<DayOfWeek, List<Faixa>> horario = faixas(horarioDeFuncionamento);
        Estabelecimento loja = estabelecimentos.buscarPorId(lojaId).orElseThrow(AcessoNegado::new);

        Estabelecimento comHorarioNovo;
        try {
            comHorarioNovo = loja.comHorario(horario);
        } catch (IllegalArgumentException repetida) {
            throw new HorarioRecusado(Motivo.FAIXA_REPETIDA, repetida);
        }
        return estabelecimentos.salvar(comHorarioNovo).getDisponibilidade();
    }

    /** A primeira das duas recusas: a {@code Faixa} com início igual ao fim. */
    private static Map<DayOfWeek, List<Faixa>> faixas(Map<DayOfWeek, List<FaixaPedida>> pedido) {
        Map<DayOfWeek, List<Faixa>> horario = new EnumMap<>(DayOfWeek.class);
        pedido.forEach((dia, faixasDoDia) -> {
            List<Faixa> doDia = new ArrayList<>();
            for (FaixaPedida faixa : faixasDoDia) {
                try {
                    doDia.add(new Faixa(faixa.inicio(), faixa.fim()));
                } catch (IllegalArgumentException ambigua) {
                    throw new HorarioRecusado(Motivo.INICIO_IGUAL_AO_FIM, ambigua);
                }
            }
            horario.put(dia, doDia);
        });
        return horario;
    }
}
